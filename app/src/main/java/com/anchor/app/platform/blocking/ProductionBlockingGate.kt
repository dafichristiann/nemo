package com.anchor.app.platform.blocking

import android.content.Context
import com.anchor.app.BuildConfig
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.platform.accessibility.AccessibilityStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [BlockingGate] (PRD v0.4 §10.5). Composes:
 *  - the build flag ([BuildConfig.FOCUS_BLOCKING_ENABLED]);
 *  - the persisted opt-in toggle, cached from [SettingsRepository.observe] so [isToggleOn] is a
 *    cheap synchronous read (the service/gate is consulted on hot paths);
 *  - the OS service status via [AccessibilityStatus].
 */
@Singleton
class ProductionBlockingGate @Inject constructor(
    @ApplicationContext context: Context,
    private val accessibilityStatus: AccessibilityStatus,
    settingsRepository: SettingsRepository,
) : BlockingGate {

    private val appContext = context.applicationContext
    private val toggleOn = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Keep the cached toggle in sync with persisted settings for the process lifetime.
        settingsRepository.observe()
            .onEach { toggleOn.set(it.focusBlockingEnabled) }
            .launchIn(scope)
    }

    override fun isBuildFlagOn(): Boolean = BuildConfig.FOCUS_BLOCKING_ENABLED

    override fun isToggleOn(): Boolean = toggleOn.get()

    override fun isServiceEnabled(): Boolean = accessibilityStatus.isEnabled(appContext)
}
