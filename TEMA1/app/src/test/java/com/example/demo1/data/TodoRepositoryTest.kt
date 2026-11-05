package com.example.demo1.data

import com.example.demo1.data.local.toEntity
import com.example.demo1.data.remote.Account
import com.example.demo1.model.Category
import com.example.demo1.model.MAX_TASK_LENGTH
import com.example.demo1.model.Priority
import com.example.demo1.model.TodoItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TodoRepositoryTest {
    private val dao = FakeTodoDao()
    private var time = 1_000L
    private var nextId = 0

    private fun TestScope.repository(remote: FakeRemote?) = TodoRepository(
        dao = dao,
        remote = remote,
        scope = backgroundScope,
        clock = { time++ },
        newId = { "id-${nextId++}" },
    )

    private val guest = Account("user-1", isAnonymous = true)

    private suspend fun TodoRepository.list() = todos.first()

    @Test
    fun withoutFirebase_everythingWorksLocally() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository(remote = null)
        repo.startSync()
        assertEquals(SyncState.NotConfigured, repo.syncState.value)

        val milk = repo.add("  Lapte  ", Category.SHOPPING, Priority.LOW)!!
        val report = repo.add("Raport", Category.WORK, Priority.HIGH)!!
        assertEquals("Lapte", milk.text)
        assertEquals(listOf(report.id, milk.id), repo.list().map { it.id })

        repo.toggle(milk.id)
        assertTrue(repo.list().single { it.id == milk.id }.done)

        repo.clearCompleted()
        assertEquals(listOf(report.id), repo.list().map { it.id })

        repo.delete(report.id)
        assertTrue(repo.list().isEmpty())
    }

    @Test
    fun blankTasksAreIgnored_andLongOnesAreCutToTheRulesLimit() = runTest(UnconfinedTestDispatcher()) {
        val repo = repository(remote = null)
        assertNull(repo.add("   ", Category.PERSONAL, Priority.LOW))
        val long = repo.add("x".repeat(MAX_TASK_LENGTH + 50), Category.PERSONAL, Priority.LOW)!!
        assertEquals(MAX_TASK_LENGTH, long.text.length)
    }

    @Test
    fun tasksCreatedBeforeSignIn_areUploadedOnSync() = runTest(UnconfinedTestDispatcher()) {
        val remote = FakeRemote()
        val repo = repository(remote)
        val early = repo.add("Înainte de login", Category.PERSONAL, Priority.MEDIUM)!!
        assertTrue(dao.rows.value.getValue(early.id).pendingUpload)

        repo.startSync()

        assertEquals(SyncState.Synced(guest), repo.syncState.value)
        assertEquals(early, remote.server().value[early.id])
        assertFalse(dao.rows.value.getValue(early.id).pendingUpload)
    }

    @Test
    fun writesAfterSignIn_goToFirestoreAndRoom() = runTest(UnconfinedTestDispatcher()) {
        val remote = FakeRemote()
        val repo = repository(remote)
        repo.startSync()

        val task = repo.add("Sală", Category.PERSONAL, Priority.LOW)!!
        assertEquals(task, remote.server().value[task.id])
        assertFalse(dao.rows.value.getValue(task.id).pendingUpload)

        repo.toggle(task.id)
        assertTrue(remote.server().value.getValue(task.id).done)
        assertTrue(repo.list().single().done)

        repo.clearCompleted()
        assertTrue(remote.server().value.isEmpty())
        assertTrue(repo.list().isEmpty())
    }

    @Test
    fun changesFromAnotherDevice_reachRoom() = runTest(UnconfinedTestDispatcher()) {
        val remote = FakeRemote()
        val repo = repository(remote)
        repo.startSync()
        val local = repo.add("De pe telefon", Category.WORK, Priority.HIGH)!!

        val fromWeb = TodoItem("web-1", "Din consolă", category = Category.SHOPPING, createdAt = 5_000L)
        remote.server().update { it + (fromWeb.id to fromWeb) }
        assertEquals(listOf(fromWeb.id, local.id), repo.list().map { it.id })

        remote.server().update { it - local.id }
        assertEquals(listOf(fromWeb), repo.list())
    }

    @Test
    fun cacheSnapshotsNeverDelete_andPendingTasksSurviveServerSnapshots() = runTest(UnconfinedTestDispatcher()) {
        val remote = FakeRemote()
        val repo = repository(remote)
        repo.startSync()
        val synced = repo.add("Sincronizată", Category.PERSONAL, Priority.LOW)!!
        val pending = TodoItem("pending-1", "Încă neîncărcată", createdAt = 1L)
        dao.upsert(pending.toEntity(pendingUpload = true))

        remote.fromCache.value = true
        remote.server().value = emptyMap()
        assertEquals(setOf(synced.id, pending.id), repo.list().map { it.id }.toSet())

        remote.fromCache.value = false
        assertEquals(listOf(pending.id), repo.list().map { it.id })
    }

    @Test
    fun failedSignIn_keepsWorkingLocally_andRetryUploads() = runTest(UnconfinedTestDispatcher()) {
        val remote = FakeRemote(signInError = IOException("fără internet"))
        val repo = repository(remote)
        repo.startSync()
        assertEquals(SyncState.Failed("fără internet"), repo.syncState.value)

        val offline = repo.add("Offline", Category.WORK, Priority.MEDIUM)!!
        assertTrue(remote.server().value.isEmpty())
        assertTrue(dao.rows.value.getValue(offline.id).pendingUpload)

        remote.signInError = null
        repo.startSync()
        assertEquals(2, remote.signInCalls)
        assertEquals(SyncState.Synced(guest), repo.syncState.value)
        assertEquals(offline, remote.server().value[offline.id])
    }

    @Test
    fun restoringADeletedTask_putsItBackInTheSamePlace() = runTest(UnconfinedTestDispatcher()) {
        val remote = FakeRemote()
        val repo = repository(remote)
        repo.startSync()
        val a = repo.add("A", Category.PERSONAL, Priority.LOW)!!
        val b = repo.add("B", Category.PERSONAL, Priority.LOW)!!
        val c = repo.add("C", Category.PERSONAL, Priority.LOW)!!

        repo.delete(b.id)
        assertFalse(b.id in remote.server().value)
        repo.save(b)

        assertEquals(listOf(c.id, b.id, a.id), repo.list().map { it.id })
        assertEquals(b, remote.server().value[b.id])
    }

    @Test
    fun googleLogin_upgradesTheGuest_andKeepsItsTasks() = runTest(UnconfinedTestDispatcher()) {
        val remote = FakeRemote()
        val repo = repository(remote)
        repo.startSync()
        val task = repo.add("Păstrează-mă", Category.WORK, Priority.HIGH)!!

        repo.signInWithGoogle("google-id-token")

        val account = (repo.syncState.value as SyncState.Synced).account
        assertEquals(Account("user-1", isAnonymous = false, email = "ana@gmail.com", displayName = "Ana Pop"), account)
        assertEquals(listOf(task), repo.list())
        assertEquals(task, remote.server("user-1").value[task.id])

        // Still synced with the same collection after the upgrade.
        val after = repo.add("După login", Category.PERSONAL, Priority.LOW)!!
        assertEquals(after, remote.server("user-1").value[after.id])
    }

    @Test
    fun googleLogin_intoAnExistingAccount_addsThisDevicesTasksToIt() = runTest(UnconfinedTestDispatcher()) {
        val google = Account("google-9", isAnonymous = false, email = "ana@gmail.com")
        val fromCloud = TodoItem("cloud-1", "Din alt telefon", createdAt = 50L)
        val remote = FakeRemote().apply {
            existingGoogleAccount = google
            server("google-9").value = mapOf(fromCloud.id to fromCloud)
        }
        val repo = repository(remote)
        repo.startSync()
        val local = repo.add("De pe telefonul ăsta", Category.SHOPPING, Priority.MEDIUM)!!

        repo.signInWithGoogle("google-id-token")

        assertEquals(SyncState.Synced(google), repo.syncState.value)
        assertEquals(setOf(local.id, fromCloud.id), repo.list().map { it.id }.toSet())
        assertEquals(local, remote.server("google-9").value[local.id])
        assertTrue(dao.rows.value.values.none { it.pendingUpload })

        // Edits from the other phone keep arriving, and new writes go to the Google account.
        remote.server("google-9").update { it - fromCloud.id }
        assertEquals(listOf(local), repo.list())
        val next = repo.add("Nouă", Category.PERSONAL, Priority.LOW)!!
        assertEquals(next, remote.server("google-9").value[next.id])
    }

    @Test
    fun logout_removesTheAccountsTasksFromTheDevice_andStartsANewGuest() = runTest(UnconfinedTestDispatcher()) {
        val remote = FakeRemote()
        val repo = repository(remote)
        repo.startSync()
        val task = repo.add("În contul Google", Category.WORK, Priority.LOW)!!
        repo.signInWithGoogle("google-id-token")

        repo.signOut()

        assertEquals(SyncState.Synced(Account("guest-2", isAnonymous = true)), repo.syncState.value)
        assertTrue(repo.list().isEmpty())
        assertEquals(task, remote.server("user-1").value[task.id])

        // A late change in the old account must not come back to this device.
        remote.server("user-1").update { it + ("late" to TodoItem("late", "Întârziată", createdAt = 9L)) }
        assertTrue(repo.list().isEmpty())

        val guestTask = repo.add("Ca invitat", Category.PERSONAL, Priority.LOW)!!
        assertEquals(guestTask, remote.server("guest-2").value[guestTask.id])
        assertFalse(guestTask.id in remote.server("user-1").value)
    }

    @Test
    fun loggingOutAGuest_isRefused_soItsTasksAreNotLost() = runTest(UnconfinedTestDispatcher()) {
        val remote = FakeRemote()
        val repo = repository(remote)
        repo.startSync()
        val task = repo.add("Invitat", Category.PERSONAL, Priority.LOW)!!

        repo.signOut()

        assertEquals(SyncState.Synced(guest), repo.syncState.value)
        assertEquals(listOf(task), repo.list())
    }
}
