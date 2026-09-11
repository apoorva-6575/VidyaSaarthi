package com.hackx.ruraledtech

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hackx.ruraledtech.core.seed.DemoContentSeeder
import com.hackx.ruraledtech.core.work.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RuralEdTechApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var demoContentSeeder: DemoContentSeeder

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { demoContentSeeder.seedIfNeeded() }
        // Catches any events left queued from a previous session that never got a chance to
        // sync (e.g. the app was killed before connectivity returned) — see SyncScheduler,
        // which was previously never invoked from anywhere, so nothing ever actually synced.
        syncScheduler.scheduleOpportunisticSync()
        syncScheduler.scheduleContentUpdateCheck()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
