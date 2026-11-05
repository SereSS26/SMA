package com.example.demo1.data.remote

import android.util.Log
import com.example.demo1.data.local.enumOrDefault
import com.example.demo1.model.Category
import com.example.demo1.model.Priority
import com.example.demo1.model.TodoItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Stores each user's tasks in `users/{uid}/todos/{taskId}`. Every install starts with an
 * anonymous account and can be upgraded to a Google account; firestore.rules keeps each
 * user's space private.
 */
class FirestoreTodoDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : TodoRemoteDataSource {

    override fun accounts(): Flow<Account?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAccount()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun ensureSignedIn(): Account {
        auth.currentUser?.let { return it.toAccount() }
        return auth.signInAnonymously().await().user.orFail().toAccount()
    }

    override suspend fun signInWithGoogle(idToken: String): Account {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val current = auth.currentUser
        val user = if (current != null && current.isAnonymous) {
            try {
                // Keeps the uid, so the guest's tasks now belong to the Google account.
                current.linkWithCredential(credential).await().user
            } catch (e: FirebaseAuthUserCollisionException) {
                // This Google account was used before (e.g. on another phone): switch to it.
                auth.signInWithCredential(e.updatedCredential ?: credential).await().user
            }
        } else {
            auth.signInWithCredential(credential).await().user
        }
        return user.orFail().toAccount()
    }

    override suspend fun signOut() = auth.signOut()

    override fun observe(uid: String): Flow<RemoteSnapshot> = callbackFlow {
        val registration = todos(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(
                    RemoteSnapshot(
                        items = snapshot.documents.mapNotNull { it.toTodoItem() },
                        fromCache = snapshot.metadata.isFromCache,
                    )
                )
            }
        }
        awaitClose { registration.remove() }
    }

    override fun upsert(uid: String, item: TodoItem) {
        todos(uid).document(item.id).set(item.toFirestore())
            .addOnFailureListener { Log.w(TAG, "Upload of task ${item.id} failed", it) }
    }

    override fun delete(uid: String, ids: List<String>) {
        // A batch holds at most 500 writes.
        ids.chunked(500).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { batch.delete(todos(uid).document(it)) }
            batch.commit().addOnFailureListener { Log.w(TAG, "Deleting ${chunk.size} tasks failed", it) }
        }
    }

    private fun todos(uid: String): CollectionReference =
        firestore.collection("users").document(uid).collection("todos")

    private companion object {
        const val TAG = "FirestoreTodos"
    }
}

private fun FirebaseUser?.orFail(): FirebaseUser = checkNotNull(this) { "Firebase sign-in returned no user" }

private fun FirebaseUser.toAccount() = Account(
    uid = uid,
    isAnonymous = isAnonymous,
    // Right after linking, the profile may only be filled in on the Google provider entry.
    email = email?.takeIf { it.isNotBlank() } ?: providerData.firstNotNullOfOrNull { it.email?.takeIf(String::isNotBlank) },
    displayName = displayName?.takeIf { it.isNotBlank() }
        ?: providerData.firstNotNullOfOrNull { it.displayName?.takeIf(String::isNotBlank) },
)

/** Field names must match the ones allowed in firestore.rules. */
internal fun TodoItem.toFirestore(): Map<String, Any> = mapOf(
    "text" to text,
    "done" to done,
    "category" to category.name,
    "priority" to priority.name,
    "createdAt" to createdAt,
)

private fun DocumentSnapshot.toTodoItem(): TodoItem? {
    val text = getString("text") ?: return null
    return TodoItem(
        id = id,
        text = text,
        done = getBoolean("done") ?: false,
        category = enumOrDefault(getString("category"), Category.PERSONAL),
        priority = enumOrDefault(getString("priority"), Priority.MEDIUM),
        createdAt = getLong("createdAt") ?: 0L,
    )
}
