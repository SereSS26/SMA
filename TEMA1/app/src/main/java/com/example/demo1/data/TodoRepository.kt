package com.example.demo1.data

import com.example.demo1.data.local.TodoDao
import com.example.demo1.data.local.toEntity
import com.example.demo1.data.local.toModel
import com.example.demo1.data.remote.Account
import com.example.demo1.data.remote.TodoRemoteDataSource
import com.example.demo1.model.Category
import com.example.demo1.model.MAX_TASK_LENGTH
import com.example.demo1.model.Priority
import com.example.demo1.model.TodoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/** What the settings screen shows about the account and the cloud backup. */
sealed interface SyncState {
    /** No google-services.json in the build: the app works only on this device. */
    data object NotConfigured : SyncState
    data object Connecting : SyncState
    data class Synced(val account: Account) : SyncState
    data class Failed(val message: String) : SyncState
}

/**
 * Chooses where data comes from. The UI only ever reads Room (single source of truth), so the
 * app starts instantly and works offline. Every write goes to Firestore first (which applies it
 * to its own cache at once and uploads it when online) and then to Room, and a Firestore
 * listener mirrors changes made elsewhere back into Room.
 *
 * The app is always signed in: as a guest (anonymous) by default, or with Google after the
 * user logs in. Room holds the tasks of whoever is signed in.
 */
class TodoRepository(
    private val dao: TodoDao,
    private val remote: TodoRemoteDataSource?,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    val todos: Flow<List<TodoItem>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

    private val _syncState = MutableStateFlow<SyncState>(if (remote == null) SyncState.NotConfigured else SyncState.Connecting)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /** Serializes writes, account changes and snapshot merges, so no change is lost between them. */
    private val mutex = Mutex()

    /** User whose collection receives writes; null while offline-only. Guarded by [mutex]. */
    private var userId: String? = null

    /** Last user whose data Room holds, to tell an account switch from an app start. Guarded by [mutex]. */
    private var lastUserId: String? = null

    private var syncJob: Job? = null

    /**
     * Follows the signed-in account: uploads tasks written before it was available, then keeps
     * Room in step with its Firestore collection. Safe to call again to retry after a failure.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun startSync() {
        val remote = remote ?: return
        if (syncJob?.isActive == true) return
        syncJob = scope.launch {
            _syncState.value = SyncState.Connecting
            try {
                remote.accounts()
                    .distinctUntilChangedBy { it?.uid }
                    .collectLatest { account ->
                        if (account == null) {
                            mutex.withLock { userId = null }
                            _syncState.value = SyncState.Connecting
                            // Nobody is signed in (first start or after logout): continue as a guest.
                            remote.ensureSignedIn()
                        } else {
                            follow(remote, account)
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Keep working locally; new tasks are queued for upload until a retry succeeds.
                mutex.withLock { userId = null }
                _syncState.value = SyncState.Failed(e.message ?: e::class.java.simpleName)
            }
        }
    }

    private suspend fun follow(remote: TodoRemoteDataSource, account: Account) {
        mutex.withLock {
            // Logging into a Google account that already has data: keep this device's tasks too.
            val previous = lastUserId
            if (previous != null && previous != account.uid) dao.markAllPending()
            val pending = dao.getPendingUpload()
            pending.forEach { remote.upsert(account.uid, it.toModel()) }
            pending.map { it.id }.chunked(500).forEach { dao.markUploaded(it) }
            userId = account.uid
            lastUserId = account.uid
        }
        _syncState.value = SyncState.Synced(account)
        remote.observe(account.uid).collect { snapshot ->
            mutex.withLock {
                // A late snapshot from an account that was just left must not touch Room.
                if (userId != account.uid) return@withLock
                dao.applyRemoteSnapshot(
                    remote = snapshot.items.map { it.toEntity() },
                    deleteMissing = !snapshot.fromCache,
                )
            }
        }
    }

    /**
     * Logs in with a Google ID token. A guest account is upgraded and keeps its tasks; an existing
     * Google account is opened and this device's tasks are added to it. Throws if Firebase refuses.
     */
    suspend fun signInWithGoogle(idToken: String) {
        val remote = checkNotNull(remote) { "Firebase is not configured" }
        val account = remote.signInWithGoogle(idToken)
        // Same uid (guest upgraded): the account listener does not fire, so publish the new profile here.
        val current = _syncState.value
        if (current is SyncState.Synced && current.account.uid == account.uid) {
            _syncState.value = SyncState.Synced(account)
        }
        startSync()
    }

    /**
     * Logs out of Google. The tasks stay in that account in the cloud but leave this device,
     * which continues with a new, empty guest account.
     */
    suspend fun signOut() {
        val remote = remote ?: return
        // Logging a guest out would throw its tasks away for good.
        val state = _syncState.value
        if (state is SyncState.Synced && state.account.isAnonymous) return
        mutex.withLock {
            userId = null
            lastUserId = null
            remote.signOut()
            dao.clearAll()
        }
        // The account listener now sees nobody signed in and continues with a new guest.
        startSync()
    }

    /** Returns the new task, or null if [text] is blank. */
    suspend fun add(text: String, category: Category, priority: Priority): TodoItem? {
        val clean = text.trim().take(MAX_TASK_LENGTH)
        if (clean.isEmpty()) return null
        val item = TodoItem(newId(), clean, category = category, priority = priority, createdAt = clock())
        save(item)
        return item
    }

    /** Inserts or replaces a task, e.g. to put back one that was just deleted. */
    suspend fun save(item: TodoItem) = mutex.withLock { saveLocked(item) }

    suspend fun toggle(id: String) = mutex.withLock {
        val current = dao.get(id)?.toModel() ?: return@withLock
        saveLocked(current.copy(done = !current.done))
    }

    suspend fun delete(id: String) = mutex.withLock {
        userId?.let { remote?.delete(it, listOf(id)) }
        dao.delete(id)
    }

    suspend fun clearCompleted() = mutex.withLock {
        val ids = dao.getDone().map { it.id }
        if (ids.isEmpty()) return@withLock
        userId?.let { remote?.delete(it, ids) }
        ids.chunked(500).forEach { dao.deleteAll(it) }
    }

    private suspend fun saveLocked(item: TodoItem) {
        val uid = userId
        if (uid != null) remote?.upsert(uid, item)
        dao.upsert(item.toEntity(pendingUpload = uid == null))
    }
}
