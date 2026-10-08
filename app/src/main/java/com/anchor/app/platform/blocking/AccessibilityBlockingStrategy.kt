package com.anchor.app.platform.blocking

import com.anchor.app.core.domain.blocking.BlockingStrategy
import com.anchor.app.core.domain.model.FocusMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real [BlockingStrategy] for Strict/Custom (PRD v0.4 §10.5, FR-F4/F8).
 *
 * Reports blocking as available only when **all three** gates pass — build flag, the user's
 * opt-in toggle, and the OS Accessibility service being enabled (see [BlockingGate]). On engage
 * it fills [BlockingState] so the [com.anchor.app.platform.accessibility.AnchorAccessibilityService]
 * can enforce it; on release it clears it. Any failure simply reports `false`, letting
 * [com.anchor.app.core.domain.usecase.focus.StartFocusUseCase] degrade the session to Gentle.
 *
 * Replaceable in tests via a fake [BlockingGate]; no Android framework needed for unit tests.
 */
@Singleton
class AccessibilityBlockingStrategy @Inject constructor(
    private val gate: BlockingGate,
    private val blockingState: BlockingState,
) : BlockingStrategy {

    override val effectiveMode: FocusMode = FocusMode.CUSTOM

    override fun isAvailable(): Boolean =
        gate.isBuildFlagOn() && gate.isToggleOn() && gate.isServiceEnabled()

    override suspend fun engage(packages: Set<String>): Boolean {
        if (!isAvailable()) return false
        blockingState.engage(packages)
        return true
    }

    override suspend fun release() {
        blockingState.release()
    }
}
