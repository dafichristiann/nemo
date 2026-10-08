package com.anchor.app.core.domain

import com.anchor.app.core.common.FakeIdProvider
import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.InsightPayload
import com.anchor.app.core.domain.usecase.insight.BuildInsightPayloadUseCase
import com.anchor.app.core.domain.usecase.task.CreateTaskInput
import com.anchor.app.core.domain.usecase.task.CreateTaskResult
import com.anchor.app.core.domain.usecase.task.CreateTaskUseCase
import com.anchor.app.core.domain.usecase.usage.AggregateUsageUseCase
import com.anchor.app.core.domain.usecase.usage.CategorizeAppsUseCase
import com.anchor.app.testutil.FakeFocusRepository
import com.anchor.app.testutil.FakeTaskDefinitionRepository
import com.anchor.app.testutil.FakeTaskRepository
import com.anchor.app.testutil.FakeUsageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Hardening edge-case tests for Sprint 8 (docs 11-testing-strategy.md §4): timezone/day boundaries,
 * empty/invalid input, and the privacy invariant that only aggregates ever leave the domain.
 */
class EdgeCasesAndPrivacyTest {

    private val utc = ZoneOffset.UTC

    // --- Timezone / day boundary (docs 11 §4: "timezone berubah / DST / lewat tengah malam") ---

    @Test
    fun epochDay_isComputedInTheDeviceZone_notUtc() {
        // 2026-10-07 23:30 UTC is still Oct 7 in UTC, but already Oct 8 in UTC+2.
        val instant = ZonedDateTime.of(2026, 10, 7, 23, 30, 0, 0, utc).toInstant().toEpochMilli()

        val utcProvider = FakeTimeProvider(instant, ZoneOffset.UTC)
        val plusTwo = FakeTimeProvider(instant, ZoneId.of("+02:00"))

        assertEquals(LocalDate.of(2026, 10, 7).toEpochDay(), utcProvider.epochDayOf(instant))
        assertEquals(LocalDate.of(2026, 10, 8).toEpochDay(), plusTwo.epochDayOf(instant))
    }

    @Test
    fun startOfDay_respectsZoneOffset() {
        val instant = ZonedDateTime.of(2026, 10, 7, 10, 0, 0, 0, utc).toInstant().toEpochMilli()
        val provider = FakeTimeProvider(instant, ZoneId.of("+02:00"))
        val start = provider.startOfDayMillis(instant)
        // Local midnight in +02:00 == 22:00 UTC the previous day.
        val expected = ZonedDateTime.of(2026, 10, 7, 0, 0, 0, 0, ZoneId.of("+02:00")).toInstant().toEpochMilli()
        assertEquals(expected, start)
    }

    // --- Input validation (docs 11 §4: "judul kosong → tidak bisa disimpan") ---

    @Test
    fun createTask_rejectsBlankTitle() = runTest {
        val useCase = CreateTaskUseCase(
            FakeTaskRepository(),
            FakeTaskDefinitionRepository(),
            FakeIdProvider(),
            FakeTimeProvider(0L, utc),
        )
        assertEquals(CreateTaskResult.EmptyTitle, useCase(CreateTaskInput(title = "   ")))
    }

    // --- Privacy invariant: only aggregates leave the domain (docs 11 §2.7, docs 09 §6) ---

    @Test
    fun insightPayload_carriesNoRawUsageFields() = runTest {
        // The payload type has no field able to hold a raw session/timestamp/identity. Assert the
        // built payload passes the privacy audit and only exposes aggregate counters.
        val payload = InsightPayload(
            dateEpochDay = 0,
            tasksCompleted = 2,
            tasksPlanned = 3,
            focusTimeMs = 1_000,
            screenTimeMs = 5_000,
            longestDistractionMs = 500,
            topDistractionCategory = "Social",
            productiveHours = emptyList(),
        )
        assertTrue(InsightPayload.passesPrivacyAudit(payload))
    }

    @Test
    fun privacyAudit_rejectsImpossibleAggregates() {
        val bad = InsightPayload(
            dateEpochDay = 0,
            tasksCompleted = -1, // impossible
            tasksPlanned = 3,
            focusTimeMs = 0,
            screenTimeMs = null,
            longestDistractionMs = null,
            topDistractionCategory = null,
            productiveHours = emptyList(),
        )
        assertFalse(InsightPayload.passesPrivacyAudit(bad))
    }

    @Test
    fun insightPayload_builderIsNullSafeWhenUsageMissing() = runTest {
        val provider: TimeProvider = FakeTimeProvider(0L, utc)
        val payload = BuildInsightPayloadUseCase(
            FakeTaskRepository(),
            FakeUsageRepository(),
            FakeFocusRepository(),
            provider,
        )()
        // No usage data → null (never a misleading zero), and the audit still passes.
        assertEquals(null, payload.screenTimeMs)
        assertTrue(InsightPayload.passesPrivacyAudit(payload))
    }

    // --- Aggregation robustness (docs 11 §2.4: OEM returns empty) ---

    @Test
    fun aggregate_handlesEmptyUsageWithoutThrowing() = runTest {
        val usage = FakeUsageRepository()
        val time = FakeTimeProvider(0L, utc)
        val useCase = AggregateUsageUseCase(usage, CategorizeAppsUseCase(usage), time)
        // Should complete without throwing even when there is nothing to aggregate.
        val computed = useCase()
        assertTrue(computed.isEmpty())
    }
}
