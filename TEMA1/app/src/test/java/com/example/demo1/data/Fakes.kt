package com.example.demo1.data

import com.example.demo1.data.local.TodoDao
import com.example.demo1.data.local.TodoEntity
import com.example.demo1.data.remote.Account
import com.example.demo1.data.remote.RemoteSnapshot
import com.example.demo1.data.remote.TodoRemoteDataSource
import com.example.demo1.model.TodoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory stand-in for the Room DAO. Uses the real `applyRemoteSnapshot` default implementation. */
class FakeTodoDao : TodoDao {
    val rows = MutableStateFlow<Map<String, TodoEntity>>(emptyMap())

    override fun observeAll(): Flow<List<TodoEntity>> =
        rows.map { it.values.sortedByDescending(TodoEntity::createdAt) }

    override suspend fun get(id: String) = rows.value[id]
    override suspend fun getDone() = rows.value.values.filter { it.done }
    override suspend fun getPendingUpload() = rows.value.values.filter { it.pendingUpload }
    override suspend fun getSyncedIds() = rows.value.values.filterNot { it.pendingUpload }.map { it.id }
    override suspend fun upsert(todo: TodoEntity) = rows.update { it + (todo.id to todo) }
    override suspend fun upsertAll(todos: List<TodoEntity>) = rows.update { it + todos.associateBy(TodoEntity::id) }
    override suspend fun delete(id: String) = rows.update { it - id }
    override suspend fun deleteAll(ids: List<String>) = rows.update { it - ids.toSet() }
    override suspend fun markUploaded(ids: List<String>) = rows.update { current ->
        current.mapValues { (id, row) -> if (id in ids) row.copy(pendingUpload = false) else row }
    }
    override suspend fun markAllPending() = rows.update { current -> current.mapValues { it.value.copy(pendingUpload = true) } }
    override suspend fun clearAll() = rows.update { emptyMap() }
}

/**
 * Behaves like Firebase Auth + Firestore with latency compensation: a write shows up in the next
 * snapshot immediately. Tests change [server] directly to simulate edits from another device.
 */
class FakeRemote(
    var signInError: Exception? = null,
    private val firstGuestUid: String = "user-1",
) : TodoRemoteDataSource {
    private val servers = mutableMapOf<String, MutableStateFlow<Map<String, TodoItem>>>()
    private val current = MutableStateFlow<Account?>(null)
    val currentAccount: StateFlow<Account?> get() = current
    val fromCache = MutableStateFlow(false)

    /** Set to simulate a Google account that already exists, so logging in switches to it instead of linking. */
    var existingGoogleAccount: Account? = null
    var signInCalls = 0
        private set
    private var guests = 0

    fun server(uid: String = firstGuestUid): MutableStateFlow<Map<String, TodoItem>> =
        servers.getOrPut(uid) { MutableStateFlow(emptyMap()) }

    override fun accounts(): Flow<Account?> = current

    override suspend fun ensureSignedIn(): Account {
        signInCalls++
        signInError?.let { throw it }
        current.value?.let { return it }
        val uid = if (guests++ == 0) firstGuestUid else "guest-$guests"
        return Account(uid, isAnonymous = true).also { current.value = it }
    }

    override suspend fun signInWithGoogle(idToken: String): Account {
        val user = current.value
        val existing = existingGoogleAccount
        val account = if (existing == null && user != null && user.isAnonymous) {
            user.copy(isAnonymous = false, email = "ana@gmail.com", displayName = "Ana Pop")
        } else {
            checkNotNull(existing) { "no Google account to sign into" }
        }
        current.value = account
        return account
    }

    override suspend fun signOut() {
        current.value = null
    }

    override fun observe(uid: String): Flow<RemoteSnapshot> =
        combine(server(uid), fromCache) { items, cache -> RemoteSnapshot(items.values.toList(), cache) }

    override fun upsert(uid: String, item: TodoItem) {
        check(uid == current.value?.uid) { "wrote to another user's collection" }
        server(uid).update { it + (item.id to item) }
    }

    override fun delete(uid: String, ids: List<String>) {
        check(uid == current.value?.uid) { "deleted from another user's collection" }
        server(uid).update { it - ids.toSet() }
    }
}
