package com.example.collectalogger2.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.collectalogger2.data.repository.GameLibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateOwnedGamesWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: GameLibraryRepository // Added dependency
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Update game libraries
                repository.updateGameLibraries()
                // It's done!
                Result.success()
            } catch (e: Exception) {
                // Something bad happened, handle it!
                Log.e("UpdateOwnedGamesWorker", "$e")
                // Report failure!
                Result.failure()
            }
        }
    }
}

class UpdateOwnedGamesWorkerFactory(private val repository: GameLibraryRepository) :
    WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            UpdateOwnedGamesWorker::class.java.name ->
                UpdateOwnedGamesWorker(appContext, workerParameters, repository)

            else -> null // Return null to let the default factory handle others
        }
    }
}