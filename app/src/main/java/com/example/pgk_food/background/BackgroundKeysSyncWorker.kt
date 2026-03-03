package com.example.pgk_food.background

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pgk_food.shared.data.local.initAndroidDatabaseContext
import com.example.pgk_food.shared.platform.initAndroidPlatformContext
import com.example.pgk_food.shared.runtime.BackgroundSyncOutcome
import com.example.pgk_food.shared.runtime.BackgroundSyncRunner
import java.util.concurrent.TimeUnit

class BackgroundKeysSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Ensure shared platform/database context is always available in worker process.
        initAndroidPlatformContext(applicationContext)
        initAndroidDatabaseContext(applicationContext)

        val outcome = runCatching {
            BackgroundSyncRunner().runOnce()
        }.getOrElse {
            return Result.retry()
        }

        return when (outcome) {
            BackgroundSyncOutcome.RETRYABLE_FAILURE -> Result.retry()
            BackgroundSyncOutcome.SUCCESS,
            BackgroundSyncOutcome.SKIPPED,
            BackgroundSyncOutcome.PERMANENT_FAILURE,
            -> Result.success()
        }
    }

    companion object {
        const val BACKOFF_MINUTES = 30L
        val BACKOFF_POLICY: BackoffPolicy = BackoffPolicy.EXPONENTIAL
        val BACKOFF_DURATION: Pair<Long, TimeUnit> = BACKOFF_MINUTES to TimeUnit.MINUTES
    }
}
