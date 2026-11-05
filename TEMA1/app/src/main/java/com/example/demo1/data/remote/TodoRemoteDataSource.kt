package com.example.demo1.data.remote

import com.example.demo1.model.TodoItem
import kotlinx.coroutines.flow.Flow

/** A full copy of the signed-in user's tasks as Firestore currently sees them. */
data class RemoteSnapshot(
    val items: List<TodoItem>,
    /** True when served from the on-device cache without confirmation from the server. */
    val fromCache: Boolean,
)

/** The Firebase user the app is working as. */
data class Account(
    val uid: String,
    /** True for the automatic guest account; false once signed in with Google. */
    val isAnonymous: Boolean,
    val email: String? = null,
    val displayName: String? = null,
)

/**
 * The cloud side of the repository. Kept as an interface so the repository can be tested
 * without Firebase and so the app still runs when Firebase is not configured.
 */
interface TodoRemoteDataSource {
    /** The current user, emitted at once and again after every sign-in or sign-out; null while signed out. */
    fun accounts(): Flow<Account?>

    /** Returns the current user, creating an anonymous one if nobody is signed in. Throws on failure. */
    suspend fun ensureSignedIn(): Account

    /**
     * Signs in with a Google ID token obtained through OAuth. A guest account is upgraded in place
     * (same uid, same tasks); if that Google account already exists, it signs into it instead.
     */
    suspend fun signInWithGoogle(idToken: String): Account

    suspend fun signOut()

    /** Live snapshots of `users/{uid}/todos`. The flow fails if the server refuses access. */
    fun observe(uid: String): Flow<RemoteSnapshot>

    /**
     * Writes are fire-and-forget: Firestore applies them to its local cache immediately and
     * sends them to the server when there is a connection, even after an app restart.
     */
    fun upsert(uid: String, item: TodoItem)

    fun delete(uid: String, ids: List<String>)
}
