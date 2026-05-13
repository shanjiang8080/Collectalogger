package com.example.collectalogger2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.collectalogger2.data.workers.UpdateOwnedGamesWorker
import com.example.collectalogger2.navigation.CollectaloggerNavGraph
import com.example.collectalogger2.ui.theme.Collectalogger2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Collectalogger2Theme {
                CollectaloggerNavGraph()
            }
        }
        // Add the background activity worker
        val updateOwnedGamesRequest: OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<UpdateOwnedGamesWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED) // Only run if online
                        .setRequiresBatteryNotLow(true)                // Be nice to the battery
                        .build()
                )
                .build()
        WorkManager
            .getInstance(applicationContext)
            .enqueueUniqueWork(
                "update owned games",
                ExistingWorkPolicy.KEEP,
                updateOwnedGamesRequest
            )
    }
}

fun CreationExtras.collectaloggerApplication(): CollectaloggerApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as CollectaloggerApplication)