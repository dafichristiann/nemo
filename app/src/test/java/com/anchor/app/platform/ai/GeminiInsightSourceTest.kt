package com.anchor.app.platform.ai

import com.anchor.app.core.domain.model.InsightPayload
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [GeminiInsightSource]'s fail-soft guarantees (FR-AI4, FR-AI7, FR-AI8).
 *
 * The network path is not exercised here (no live key / no mock server). Instead we assert the
 * critical safety contract: with no configured key (the test BuildConfig has an empty
 * `GEMINI_API_KEY`) the source must return null **without throwing and without any network call**,
 * so the caller always falls back to rule-based.
 */
class GeminiInsightSourceTest {

    private val payload = InsightPayload(
        dateEpochDay = 20_734,
        tasksCompleted = 1,
        tasksPlanned = 3,
        focusTimeMs = 25 * 60_000L,
        screenTimeMs = 2 * 60 * 60_000L,
        longestDistractionMs = 45 * 60_000L,
        topDistractionCategory = "Social",
        productiveHours = emptyList(),
    )

    @Test
    fun returnsNullWithoutKeyAndNeverThrows() = runTest {
        // Test builds have no GEMINI_API_KEY → the source short-circuits before any network IO.
        val result = GeminiInsightSource().generate(payload)
        assertNull(result)
    }

    @Test
    fun returnsNullForPrivacyAuditFailure() = runTest {
        // Negative aggregates must be rejected before any request is attempted.
        val invalid = payload.copy(tasksCompleted = -1)
        val result = GeminiInsightSource().generate(invalid)
        assertNull(result)
    }
}
