package com.example.demo1.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.demo1.model.Category
import com.example.demo1.model.Priority
import java.util.UUID

/** Entry point that ties the entities and DAOs together. */
@Database(entities = [TodoEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "todos.db")
                .addCallback(SeedCallback)
                .build()
    }

    /**
     * Fills a brand-new database with the sample tasks the app used to keep in memory. They are
     * marked as pending upload, so they reach Firestore with the first signed-in user.
     */
    private object SeedCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            val samples = listOf(
                Triple("Cumpără lapte și pâine", Category.SHOPPING, Priority.LOW) to false,
                Triple("Trimite raportul săptămânal", Category.WORK, Priority.HIGH) to false,
                Triple("Sună-l pe bunicul", Category.PERSONAL, Priority.MEDIUM) to false,
                Triple("Pregătește prezentarea", Category.WORK, Priority.MEDIUM) to true,
                Triple("Mergi la sală", Category.PERSONAL, Priority.LOW) to true,
                Triple("Fructe pentru weekend", Category.SHOPPING, Priority.MEDIUM) to false,
            )
            samples.forEachIndexed { index, (task, done) ->
                val (text, category, priority) = task
                db.execSQL(
                    "INSERT INTO todos (id, text, done, category, priority, created_at, pending_upload) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 1)",
                    arrayOf<Any>(
                        UUID.randomUUID().toString(), text, if (done) 1 else 0,
                        category.name, priority.name, now - index,
                    )
                )
            }
        }
    }
}
