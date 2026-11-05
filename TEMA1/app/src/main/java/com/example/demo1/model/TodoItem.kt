package com.example.demo1.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.demo1.R
import com.example.demo1.ui.theme.CategoryPersonal
import com.example.demo1.ui.theme.CategoryShopping
import com.example.demo1.ui.theme.CategoryWork
import com.example.demo1.ui.theme.PriorityHigh
import com.example.demo1.ui.theme.PriorityLow
import com.example.demo1.ui.theme.PriorityMedium

/**
 * A task as the UI sees it. [id] is a UUID string so the same task has the same key
 * in Room and in Firestore; [createdAt] (epoch millis) keeps the list in a stable order.
 */
data class TodoItem(
    val id: String,
    val text: String,
    val done: Boolean = false,
    val category: Category = Category.PERSONAL,
    val priority: Priority = Priority.MEDIUM,
    val createdAt: Long = 0L,
)

/** Firestore rejects longer texts (see firestore.rules), so the app never sends them. */
const val MAX_TASK_LENGTH = 500

enum class Category(@StringRes val label: Int, val color: Color) {
    PERSONAL(R.string.category_personal, CategoryPersonal),
    WORK(R.string.category_work, CategoryWork),
    SHOPPING(R.string.category_shopping, CategoryShopping),
}

enum class Priority(@StringRes val label: Int, val color: Color) {
    LOW(R.string.priority_low, PriorityLow),
    MEDIUM(R.string.priority_medium, PriorityMedium),
    HIGH(R.string.priority_high, PriorityHigh),
}

enum class TaskFilter(@StringRes val label: Int) {
    ALL(R.string.filter_all),
    ACTIVE(R.string.filter_active),
    DONE(R.string.filter_done),
}

enum class ThemeMode(@StringRes val label: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
}
