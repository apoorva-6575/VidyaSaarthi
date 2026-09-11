package com.hackx.ruraledtech.core.work

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(private val workManager: WorkManager) {

    fun scheduleOpportunisticSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("opportunistic_sync", ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Same "exists but nothing ever calls it" gap as SyncWorker had: ContentUpdateWorker
     * (downloads newly published content, verifies checksum, saves it for P2P store-and-
     * forward, installs it) was never scheduled from anywhere either.
     */
    fun scheduleContentUpdateCheck() {
        val request = OneTimeWorkRequestBuilder<ContentUpdateWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("content_update_check", ExistingWorkPolicy.KEEP, request)
    }
}
