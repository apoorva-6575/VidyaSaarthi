package com.hackx.ruraledtech

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.hackx.ruraledtech.core.seed.DemoContentSeeder
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

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch { demoContentSeeder.seedIfNeeded() }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
