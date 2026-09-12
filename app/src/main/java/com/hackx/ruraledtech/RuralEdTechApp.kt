package com.hackx.ruraledtech

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hackx.ruraledtech.core.work.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RuralEdTechApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override fun onCreate() {
        super.onCreate()
        // Catches any events left queued from a previous session that never got a chance to
        // sync (e.g. the app was killed before connectivity returned) — see SyncScheduler.
        syncScheduler.scheduleOpportunisticSync()
        syncScheduler.scheduleContentUpdateCheck()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
