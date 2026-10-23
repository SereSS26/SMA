package com.example.demo1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.demo1.model.Category
import com.example.demo1.model.Priority
import com.example.demo1.model.TaskFilter
import com.example.demo1.model.ThemeMode
import com.example.demo1.model.TodoItem

/** Holds all UI state in memory; there is no persistence yet. */
class TodoViewModel : ViewModel() {
    private var nextId = 1L

    val todos = mutableStateListOf(
        sample("Cumpără lapte și pâine", Category.SHOPPING, Priority.LOW),
        sample("Trimite raportul săptămânal", Category.WORK, Priority.HIGH),
        sample("Sună-l pe bunicul", Category.PERSONAL, Priority.MEDIUM),
        sample("Pregătește prezentarea", Category.WORK, Priority.MEDIUM, done = true),
        sample("Mergi la sală", Category.PERSONAL, Priority.LOW, done = true),
        sample("Fructe pentru weekend", Category.SHOPPING, Priority.MEDIUM),
    )

    /** `null` means "all categories". */
    var selectedCategory by mutableStateOf<Category?>(null)
    var filter by mutableStateOf(TaskFilter.ALL)
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    var hideCompleted by mutableStateOf(false)

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
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        todos.add(0, TodoItem(nextId++, trimmed, category = category, priority = priority))
    }

    fun toggle(id: Long) {
        val index = todos.indexOfFirst { it.id == id }
        if (index >= 0) todos[index] = todos[index].copy(done = !todos[index].done)
    }

    /** Removes the item and returns it with its position, so it can be restored. */
    fun delete(id: Long): Pair<Int, TodoItem>? {
        val index = todos.indexOfFirst { it.id == id }
        if (index < 0) return null
        return index to todos.removeAt(index)
    }

    fun restore(index: Int, item: TodoItem) {
        todos.add(index.coerceIn(0, todos.size), item)
    }

    fun clearCompleted() {
        todos.removeAll { it.done }
    }

    private fun sample(text: String, category: Category, priority: Priority, done: Boolean = false) =
        TodoItem(nextId++, text, done, category, priority)
}
