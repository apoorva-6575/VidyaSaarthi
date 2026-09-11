package com.hackx.ruraledtech.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hackx.ruraledtech.domain.repository.SyncOutcome
import com.hackx.ruraledtech.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * PS section 33/59: WorkManager triggers this opportunistically once connectivity allows.
 * Group 4 provides the real upload inside SyncRepositoryImpl.syncNow(); this worker only
 * owns retry/backoff scheduling, never the sync logic itself.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (syncRepository.syncNow()) {
        is SyncOutcome.Deferred -> Result.retry()
        is SyncOutcome.Success -> Result.success()
        is SyncOutcome.PartialFailure -> Result.retry()
    }
}
