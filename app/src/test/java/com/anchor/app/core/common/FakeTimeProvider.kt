package com.anchor.app.core.common

import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Deterministic [TimeProvider] for unit tests (docs 11-testing-strategy.md §5).
 * Tests must never call the real system clock.
 */
class FakeTimeProvider(
    private var nowMillis: Long,
    private val zone: ZoneId = ZoneOffset.UTC,
) : TimeProvider {

    override fun nowMillis(): Long = nowMillis
    override fun zoneId(): ZoneId = zone

    /** Advance the clock by [millis] (for simulating elapsed time). */
    fun advanceBy(millis: Long) {
        nowMillis += millis
    }
}
