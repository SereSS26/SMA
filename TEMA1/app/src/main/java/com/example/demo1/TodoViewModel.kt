package com.example.demo1

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.demo1.auth.GoogleSignInResult
import com.example.demo1.data.SyncState
import com.example.demo1.data.TodoRepository
import com.example.demo1.model.Category
import com.example.demo1.model.Priority
import com.example.demo1.model.TaskFilter
import com.example.demo1.model.ThemeMode
import com.example.demo1.model.TodoItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** A short message for the snackbar, kept as a resource id so the ViewModel needs no Context. */
data class UiMessage(@StringRes val text: Int, val arg: String? = null)

/**
 * Holds the screen state. Tasks come from [TodoRepository] (Room, synced with Firestore);
 * the ViewModel keeps no Context, only the repository.
 */
class TodoViewModel(private val repository: TodoRepository) : ViewModel() {

    /** Newest first, exactly as stored in Room. */
    var todos by mutableStateOf<List<TodoItem>>(emptyList())
        private set

    var syncState by mutableStateOf<SyncState>(repository.syncState.value)
        private set

    /** True while the Google account picker is open or Firebase is logging in. */
    var authBusy by mutableStateOf(false)
        private set

    /** Shown once by the UI, which then calls [messageShown]. */
    var message by mutableStateOf<UiMessage?>(null)
        private set

    /** `null` means "all categories". */
    var selectedCategory by mutableStateOf<Category?>(null)
    var filter by mutableStateOf(TaskFilter.ALL)
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    var hideCompleted by mutableStateOf(false)

    init {
        viewModelScope.launch { repository.todos.collect { todos = it } }
        viewModelScope.launch { repository.syncState.collect { syncState = it } }
    }

    val visibleTodos: List<TodoItem>
        get() = todos
            .filter { selectedCategory == null || it.category == selectedCategory }
            .filter {
                when (filter) {
                    TaskFilter.ALL -> !(hideCompleted && it.done)
                    TaskFilter.ACTIVE -> !it.done
                    TaskFilter.DONE -> it.done
                }
            }
            .sortedWith(compareBy<TodoItem> { it.done }.thenByDescending { it.priority.ordinal })

    fun activeCount(category: Category?): Int =
        todos.count { !it.done && (category == null || it.category == category) }

    fun add(text: String, category: Category, priority: Priority) {
        viewModelScope.launch { repository.add(text, category, priority) }
    }

    fun toggle(id: String) {
        viewModelScope.launch { repository.toggle(id) }
    }

    /** Removes the task and returns it, so the caller can offer to [restore] it. */
    fun delete(id: String): TodoItem? {
        val item = todos.firstOrNull { it.id == id } ?: return null
        viewModelScope.launch { repository.delete(id) }
        return item
    }

    /** Puts a deleted task back; its original timestamp returns it to the same place in the list. */
    fun restore(item: TodoItem) {
        viewModelScope.launch { repository.save(item) }
    }

    fun clearCompleted() {
        viewModelScope.launch { repository.clearCompleted() }
    }

    fun retrySync() = repository.startSync()

    /** Called by the UI right before it opens Google's account picker. */
    fun beginGoogleSignIn() {
        authBusy = true
    }

    /** Receives what the account picker returned and, on success, logs into Firebase with the token. */
    fun onGoogleSignInResult(result: GoogleSignInResult) {
        when (result) {
            is GoogleSignInResult.Success -> viewModelScope.launch {
                try {
                    repository.signInWithGoogle(result.idToken)
                    message = UiMessage(R.string.login_success)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    message = UiMessage(R.string.login_failed, e.message ?: e::class.java.simpleName)
                } finally {
                    authBusy = false
                }
            }
            GoogleSignInResult.Cancelled -> authBusy = false
            GoogleSignInResult.NotConfigured -> fail(UiMessage(R.string.login_not_configured))
            GoogleSignInResult.NoAccount -> fail(UiMessage(R.string.login_no_account))
            is GoogleSignInResult.Error -> fail(UiMessage(R.string.login_failed, result.message))
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            message = UiMessage(R.string.logout_done)
        }
    }

    fun messageShown() {
        message = null
    }

    private fun fail(error: UiMessage) {
        authBusy = false
        message = error
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                TodoViewModel((this[APPLICATION_KEY] as TodoApplication).repository)
            }
        }
    }
}
