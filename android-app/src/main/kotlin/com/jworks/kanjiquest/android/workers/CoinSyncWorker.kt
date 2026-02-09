package com.jworks.kanjiquest.android.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jworks.kanjiquest.core.domain.repository.JCoinRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically syncs pending J Coin events to Supabase.
 *
 * Runs every 30 minutes when network is available.
 * Uses exponential backoff for retries on failure.
 */
@HiltWorker
class CoinSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val jCoinRepository: JCoinRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting J Coin background sync...")

            val syncedCount = jCoinRepository.syncPendingEvents()

            if (syncedCount > 0) {
                Log.i(TAG, "Successfully synced $syncedCount coin events")
            }

            val pendingCount = jCoinRepository.getPendingSyncCount()
            if (pendingCount > 0) {
                Log.w(TAG, "$pendingCount coin events still pending (may have failed)")
                // Retry if there are still pending events
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "J Coin sync failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CoinSyncWorker"
        private const val WORK_NAME = "coin_sync_periodic"

        /**
         * Schedule periodic background sync for J Coins.
         *
         * This should be called once during app initialization.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<CoinSyncWorker>(
                repeatInterval = 30,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, // Initial backoff: 15 minutes
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Don't replace if already scheduled
                request
            )

            Log.i(TAG, "J Coin background sync scheduled (every 30 minutes)")
        }
    }
}
