package com.example.demo1

import android.app.Application
import android.util.Log
import com.example.demo1.data.TodoRepository
import com.example.demo1.data.local.AppDatabase
import com.example.demo1.data.remote.FirestoreTodoDataSource
import com.example.demo1.data.remote.TodoRemoteDataSource
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Builds the app's single instances once. Context stays here, so ViewModels never hold one.
 */
class TodoApplication : Application() {
    /** Lives as long as the process, so syncing continues while screens come and go. */
    private val appScope = CoroutineScope(SupervisorJob())

    val repository: TodoRepository by lazy {
        TodoRepository(
            dao = AppDatabase.create(this).todoDao(),
            remote = createRemote(),
            scope = appScope,
        ).also { it.startSync() }
    }

    /** Null when the build has no google-services.json; the app then runs on Room alone. */
    private fun createRemote(): TodoRemoteDataSource? {
        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.i("TodoApplication", "Firebase is not configured; tasks are stored on this device only")
            return null
        }
        return FirestoreTodoDataSource(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
    }
}
