package com.hackx.ruraledtech.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Placeholder hook for Group 4's backend content-update checks (PS section 17/19: GET
 * /content/manifest, GET /content/{package_id}). Content installation itself always goes
 * through ContentInstaller so this worker, P2P, and manual downloads share one code path.
 */
@HiltWorker
class ContentUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = Result.success()
}
