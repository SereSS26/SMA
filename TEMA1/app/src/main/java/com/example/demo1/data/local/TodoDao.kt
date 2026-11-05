package com.example.demo1.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** The only operations the app may run on the `todos` table; Room checks the SQL at compile time. */
@Dao
interface TodoDao {
    /** Newest first; the ViewModel applies the done/priority ordering on top of this. */
    @Query("SELECT * FROM todos ORDER BY created_at DESC")
    fun observeAll(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun get(id: String): TodoEntity?

    @Query("SELECT * FROM todos WHERE done = 1")
    suspend fun getDone(): List<TodoEntity>

    @Query("SELECT * FROM todos WHERE pending_upload = 1")
    suspend fun getPendingUpload(): List<TodoEntity>

    @Query("SELECT id FROM todos WHERE pending_upload = 0")
    suspend fun getSyncedIds(): List<String>

    @Upsert
    suspend fun upsert(todo: TodoEntity)

    @Upsert
    suspend fun upsertAll(todos: List<TodoEntity>)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM todos WHERE id IN (:ids)")
    suspend fun deleteAll(ids: List<String>)

    @Query("UPDATE todos SET pending_upload = 0 WHERE id IN (:ids)")
    suspend fun markUploaded(ids: List<String>)

    /** Queues every task for upload, used when the device switches to another account. */
    @Query("UPDATE todos SET pending_upload = 1")
    suspend fun markAllPending()

    @Query("DELETE FROM todos")
    suspend fun clearAll()

    /**
     * Makes the table match a snapshot from Firestore. Tasks still waiting to be uploaded are
     * kept; with [deleteMissing] false (a snapshot served from the offline cache) nothing is removed.
     */
    @Transaction
    suspend fun applyRemoteSnapshot(remote: List<TodoEntity>, deleteMissing: Boolean) {
        if (deleteMissing) {
            val remoteIds = remote.mapTo(HashSet()) { it.id }
            getSyncedIds()
                .filterNot { it in remoteIds }
                // SQLite on older devices allows at most 999 bound variables per statement.
                .chunked(500)
                .forEach { deleteAll(it) }
        }
        upsertAll(remote)
    }
}
