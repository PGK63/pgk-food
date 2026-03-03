package com.example.pgk_food.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackgroundKeysSyncScheduler {
    private const val UNIQUE_WORK_NAME = "background_keys_sync"
    private const val REPEAT_INTERVAL_HOURS = 6L

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<BackgroundKeysSyncWorker>(
            REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackgroundKeysSyncWorker.BACKOFF_POLICY,
                BackgroundKeysSyncWorker.BACKOFF_DURATION.first,
                BackgroundKeysSyncWorker.BACKOFF_DURATION.second,
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
