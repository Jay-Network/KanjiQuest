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
import com.jworks.kanjiquest.core.data.sync.SyncResult
import com.jworks.kanjiquest.core.data.sync.SyncTrigger
import com.jworks.kanjiquest.core.domain.UserSessionProvider
import com.jworks.kanjiquest.core.domain.model.LOCAL_USER_ID
import com.jworks.kanjiquest.core.domain.repository.LearningSyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class LearningSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val learningSyncRepository: LearningSyncRepository,
    private val userSessionProvider: UserSessionProvider
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val userId = userSessionProvider.getUserId()
            if (userId == null || userId == LOCAL_USER_ID) {
                Log.d(TAG, "No logged-in user, skipping sync")
                return Result.success()
            }

            Log.d(TAG, "Starting cross-device sync...")

            when (val result = learningSyncRepository.syncAll(userId, SyncTrigger.BACKGROUND_PERIODIC)) {
                is SyncResult.Success -> {
                    Log.i(TAG, "Sync complete: pushed=${result.pushed}, pulled=${result.pulled}, version=${result.newVersion}")
                    Result.success()
                }
                is SyncResult.Error -> {
                    Log.w(TAG, "Sync error: ${result.message}")
                    Result.retry()
                }
                is SyncResult.NotLoggedIn -> {
                    Log.d(TAG, "Not logged in, skipping sync")
                    Result.success()
                }
                is SyncResult.AlreadySyncing -> {
                    Log.d(TAG, "Sync already in progress")
                    Result.success()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Learning sync failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "LearningSyncWorker"
        private const val WORK_NAME = "learning_sync_periodic"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<LearningSyncWorker>(
                repeatInterval = 30,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

            Log.i(TAG, "Learning data background sync scheduled (every 30 minutes)")
        }
    }
}
