package com.anchor.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.anchor.app.platform.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Hilt generates the [dagger.hilt.android.HiltAndroidApp] component from here, which
 * wires the [com.anchor.app.core.di] modules. Nemo is local-first with no backend —
 * nothing is initialised here that requires network access.
 *
 * Implements [Configuration.Provider] so WorkManager can construct `@HiltWorker`s with
 * injected dependencies (docs 02-architecture.md §5).
 */
@HiltAndroidApp
class NemoApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
