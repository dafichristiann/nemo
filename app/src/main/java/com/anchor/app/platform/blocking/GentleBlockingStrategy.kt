package com.anchor.app.platform.blocking

import com.anchor.app.core.domain.blocking.BlockingStrategy
import com.anchor.app.core.domain.model.FocusMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [BlockingStrategy] (Spike B `13-spike-b-accessibility.md`).
 *
 * App blocking (Strict/Custom) depends on an experimental, feature-flagged `AccessibilityService`
 * that is **not implemented yet** — Phase 3 ships Focus with Gentle first. So this strategy
 * reports blocking as **unavailable** and always resolves to [FocusMode.GENTLE]; the
 * [com.anchor.app.core.domain.usecase.focus.StartFocusUseCase] then records a Gentle session
 * with a fallback reason instead of failing (PRD §10.5, docs 08 §5/§8).
 *
 * When the Accessibility feature lands, replace this with a real strategy that returns true only
 * while the service is genuinely available — the domain contract stays the same.
 */
@Singleton
class GentleBlockingStrategy @Inject constructor() : BlockingStrategy {

    override val effectiveMode: FocusMode = FocusMode.GENTLE

    override fun isAvailable(): Boolean = false

    override suspend fun engage(packages: Set<String>): Boolean = false

    override suspend fun release() {
        // Nothing is ever blocked in Gentle mode — safe no-op (idempotent by contract).
    }
}
