package com.example.demo1

import com.example.demo1.auth.GoogleSignInResult
import com.example.demo1.data.FakeRemote
import com.example.demo1.data.FakeTodoDao
import com.example.demo1.data.SyncState
import com.example.demo1.data.TodoRepository
import com.example.demo1.data.remote.Account
import com.example.demo1.model.Category
import com.example.demo1.model.Priority
import com.example.demo1.model.TaskFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val appScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val remote = FakeRemote()
    private lateinit var viewModel: TodoViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        var time = 0L
        val repository = TodoRepository(FakeTodoDao(), remote, appScope, clock = { time++ })
        repository.startSync()
        viewModel = TodoViewModel(repository)
    }

    @After
    fun tearDown() {
        appScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun showsSyncStateFromRepository() {
        assertEquals(SyncState.Synced(Account("user-1", isAnonymous = true)), viewModel.syncState)
    }

    @Test
    fun addedTasksAppear_sortedByDoneThenPriority() {
        viewModel.add("Scăzută", Category.PERSONAL, Priority.LOW)
        viewModel.add("Ridicată", Category.WORK, Priority.HIGH)
        viewModel.add("Medie", Category.SHOPPING, Priority.MEDIUM)
        viewModel.toggle(viewModel.todos.single { it.text == "Ridicată" }.id)

        assertEquals(listOf("Medie", "Scăzută", "Ridicată"), viewModel.visibleTodos.map { it.text })
        assertEquals(3, remote.server().value.size)

        viewModel.filter = TaskFilter.ACTIVE
        viewModel.selectedCategory = Category.SHOPPING
        assertEquals(listOf("Medie"), viewModel.visibleTodos.map { it.text })
        assertEquals(2, viewModel.activeCount(null))
    }

    @Test
    fun deleteThenUndo_restoresTheTask() {
        viewModel.add("A", Category.PERSONAL, Priority.LOW)
        viewModel.add("B", Category.PERSONAL, Priority.LOW)
        val before = viewModel.todos

        val removed = viewModel.delete(before.first { it.text == "A" }.id)!!
        assertEquals(listOf("B"), viewModel.todos.map { it.text })
        assertTrue(remote.server().value.values.none { it.text == "A" })

        viewModel.restore(removed)
        assertEquals(before, viewModel.todos)
        assertEquals(2, remote.server().value.size)
    }

    @Test
    fun clearCompleted_removesOnlyDoneTasks() {
        viewModel.add("Gata", Category.PERSONAL, Priority.LOW)
        viewModel.add("De făcut", Category.PERSONAL, Priority.LOW)
        viewModel.toggle(viewModel.todos.single { it.text == "Gata" }.id)

        viewModel.clearCompleted()

        assertEquals(listOf("De făcut"), viewModel.todos.map { it.text })
        assertEquals(listOf("De făcut"), remote.server().value.values.map { it.text })
    }

    @Test
    fun googleSignIn_success_logsInAndTellsTheUser() {
        viewModel.beginGoogleSignIn()
        assertTrue(viewModel.authBusy)

        viewModel.onGoogleSignInResult(GoogleSignInResult.Success("google-id-token"))

        assertFalse(viewModel.authBusy)
        assertEquals(UiMessage(R.string.login_success), viewModel.message)
        val account = (viewModel.syncState as SyncState.Synced).account
        assertEquals("ana@gmail.com", account.email)
        assertFalse(account.isAnonymous)

        viewModel.messageShown()
        assertNull(viewModel.message)
    }

    @Test
    fun googleSignIn_cancelledOrUnavailable() {
        viewModel.beginGoogleSignIn()
        viewModel.onGoogleSignInResult(GoogleSignInResult.Cancelled)
        assertFalse(viewModel.authBusy)
        assertNull(viewModel.message)

        viewModel.beginGoogleSignIn()
        viewModel.onGoogleSignInResult(GoogleSignInResult.NotConfigured)
        assertFalse(viewModel.authBusy)
        assertEquals(UiMessage(R.string.login_not_configured), viewModel.message)

        viewModel.onGoogleSignInResult(GoogleSignInResult.Error("network"))
        assertEquals(UiMessage(R.string.login_failed, "network"), viewModel.message)
        assertTrue((viewModel.syncState as SyncState.Synced).account.isAnonymous)
    }

    @Test
    fun signOut_afterGoogleLogin_leavesAnEmptyGuestList() {
        viewModel.add("Sarcină", Category.WORK, Priority.LOW)
        viewModel.onGoogleSignInResult(GoogleSignInResult.Success("google-id-token"))

        viewModel.signOut()

        assertTrue(viewModel.todos.isEmpty())
        assertEquals(UiMessage(R.string.logout_done), viewModel.message)
        assertTrue((viewModel.syncState as SyncState.Synced).account.isAnonymous)
    }
}
