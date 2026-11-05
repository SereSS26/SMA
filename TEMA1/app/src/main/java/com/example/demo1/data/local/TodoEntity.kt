package com.example.demo1.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.demo1.model.Category
import com.example.demo1.model.Priority
import com.example.demo1.model.TodoItem

/**
 * Room table for tasks. Enums are stored by name so the column values match the
 * Firestore documents and survive reordering of the enum entries.
 */
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: String,
    val text: String,
    val done: Boolean,
    val category: String,
    val priority: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    /**
     * True while the task exists only on this device because nobody was signed in when it
     * was written. It is uploaded as soon as a Firebase user is available.
     */
    @ColumnInfo(name = "pending_upload") val pendingUpload: Boolean = false,
)

fun TodoEntity.toModel() = TodoItem(
    id = id,
    text = text,
    done = done,
    category = enumOrDefault(category, Category.PERSONAL),
    priority = enumOrDefault(priority, Priority.MEDIUM),
    createdAt = createdAt,
)

fun TodoItem.toEntity(pendingUpload: Boolean = false) = TodoEntity(
    id = id,
    text = text,
    done = done,
    category = category.name,
    priority = priority.name,
    createdAt = createdAt,
    pendingUpload = pendingUpload,
)

inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default
