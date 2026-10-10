package com.anchor.app.core.domain.usecase.insight

import com.anchor.app.core.common.FakeIdProvider
import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.domain.model.AppCategory
import com.anchor.app.core.domain.model.AppUsage
import com.anchor.app.core.domain.model.DailyUsageSummary
import com.anchor.app.core.domain.model.FocusSession
import com.anchor.app.core.domain.model.FocusMode
import com.anchor.app.core.domain.model.FocusStatus
import com.anchor.app.core.domain.model.InsightPayload
import com.anchor.app.core.domain.model.InsightSource
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.testutil.FakeAiInsightSource
import com.anchor.app.testutil.FakeFocusRepository
import com.anchor.app.testutil.FakeInsightRepository
import com.anchor.app.testutil.FakeSettingsRepository
import com.anchor.app.testutil.FakeTaskRepository
import com.anchor.app.testutil.FakeUsageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for the insight pipeline (FR-AI1..AI6, FR-DR1..DR5; AC §10.8-10.9). Verifies the
 * aggregate-only payload, rule-based fallback, AI schema validation/fallback, caching, and the
 * "hide usage when absent" honesty rule.
 */
class InsightPipelineTest {

    private val utc = ZoneOffset.UTC
    private val fixedNow: Long =
        ZonedDateTime.of(2026, 10, 7, 20, 0, 0, 0, utc).toInstant().toEpochMilli()
    private val todayEpoch = LocalDate.of(2026, 10, 7).toEpochDay()

    private fun time() = FakeTimeProvider(fixedNow, utc)

    // --- Payload builder (FR-AI1, docs 09-privacy §6) ---

    @Test
    fun payload_containsOnlyAggregatesAndNullUsageWhenAbsent() = runTest {
        val tasks = FakeTaskRepository()
        tasks.insert(doneTask("t1"))
        tasks.insert(openTask("t2", deadline = fixedNow + 60_000L)) // planned for today
        val useCase = BuildInsightPayloadUseCase(tasks, FakeUsageRepository(), FakeFocusRepository(), time())

        val payload = useCase(todayEpoch)

        assertEquals(1, payload.tasksCompleted)
        assertEquals(1, payload.tasksPlanned)
        // No usage summary today → screen time is null (never a misleading 0).
        assertNull(payload.screenTimeMs)
        assertNull(payload.longestDistractionMs)
        assertTrue(InsightPayload.passesPrivacyAudit(payload))
    }

    @Test
    fun payload_scopesTaskCountsToRequestedDay() = runTest {
        // Regression: task counts must be scoped to the requested day. Counting all-time rows
        // made every reflection report the lifetime plan/complete totals as "today's".
        val tasks = FakeTaskRepository()
        tasks.insert(doneTask("today-done"))           // completed today
        tasks.insert(openTask("today-open", deadline = fixedNow + 60_000L)) // due today
        // A task due on another day must not count toward today's plan.
        val otherDay = fixedNow + 5 * 86_400_000L
        tasks.insert(openTask("other-open", deadline = otherDay))
        // A task completed on another day must not count toward today's completions.
        tasks.insert(doneTask("old-done", completedAt = fixedNow - 5 * 86_400_000L))

        val payload = BuildInsightPayloadUseCase(tasks, FakeUsageRepository(), FakeFocusRepository(), time())(todayEpoch)

        assertEquals("only today's completion counts", 1, payload.tasksCompleted)
        assertEquals("only today's planned tasks count", 1, payload.tasksPlanned)
    }

    @Test
    fun payload_derivesLongestDistractionFromAggregates() = runTest {
        val usage = FakeUsageRepository()
        usage.upsertDailySummary(
            DailyUsageSummary(
                dateEpochDay = todayEpoch,
                totalScreenTimeMs = 5 * 60 * 60_000L,
                distractionRatio = 0.5f,
                topApps = listOf(AppUsage("com.tiktok", 45 * 60_000L, 3), AppUsage("com.chrome", 20 * 60_000L, 2)),
                categoryBreakdown = emptyMap(),
                sessionCount = 5,
                computedAt = fixedNow,
            ),
        )
        usage.upsertAppCategory(AppCategory("com.tiktok", "SOCIAL", isDistraction = true, overrideSource = AppCategory.SOURCE_AUTO))

        val payload = BuildInsightPayloadUseCase(FakeTaskRepository(), usage, FakeFocusRepository(), time())(todayEpoch)

        assertEquals(5 * 60 * 60_000L, payload.screenTimeMs)
        assertEquals(45 * 60_000L, payload.longestDistractionMs)
        assertEquals("Social", payload.topDistractionCategory)
    }

    @Test
    fun payload_includesFocusTimeBetweenDayBounds() = runTest {
        val focus = FakeFocusRepository()
        val startOfDay = time().startOfDayMillis(fixedNow)
        focus.insert(
            FocusSession("f1", null, FocusMode.GENTLE, 25, 42 * 60_000L, startOfDay + 60_000L, startOfDay + 2 * 60 * 60_000L, FocusStatus.FINISHED, emptyList(), null),
        )
        val payload = BuildInsightPayloadUseCase(FakeTaskRepository(), FakeUsageRepository(), focus, time())(todayEpoch)
        assertEquals(42 * 60_000L, payload.focusTimeMs)
    }

    // --- Rule-based source (FR-AI4, §10.9) ---

    @Test
    fun ruleBased_producesDataSupportedSummary() = runTest {
        val draft = RuleBasedInsightSource().generate(
            payload(tasksCompleted = 2, tasksPlanned = 3, focusMs = 0, screenTimeMs = null),
        )
        assertTrue(draft.summary.contains("2 of 3"))
        // Offers a focus action because work remains.
        assertEquals("start_focus", draft.recommendedAction)
        assertTrue(draft.suggestions.isNotEmpty())
    }

    @Test
    fun ruleBased_hidesDistractionWhenNoUsage() = runTest {
        val draft = RuleBasedInsightSource().generate(
            payload(tasksCompleted = 0, tasksPlanned = 0, focusMs = 0, screenTimeMs = null),
        )
        assertTrue(draft.patterns.none { it.contains("distraction", ignoreCase = true) })
    }

    // --- Schema validation (FR-AI4, §7-07) ---

    @Test
    fun schemaValidator_rejectsBlankSummaryAndBadRisk() {
        assertFalse(InsightSchemaValidator.isValid(null))
        assertFalse(InsightSchemaValidator.isValid(draft(summary = "  ", risk = "low")))
        assertFalse(InsightSchemaValidator.isValid(draft(summary = "ok", risk = "critical")))
        assertTrue(InsightSchemaValidator.isValid(draft(summary = "ok", risk = "low")))
    }

    // --- Generator: fallback + caching (FR-AI2..AI5) ---

    @Test
    fun generator_usesRuleBasedWhenAiDisabled() = runTest {
        val settings = FakeSettingsRepository() // aiEnabled = false by default
        val ai = FakeAiInsightSource(draft = aiDraft())
        val useCase = generator(FakeInsightRepository(), settings, ai)

        val insight = useCase(todayEpoch)

        assertEquals(InsightSource.RULE_BASED, insight.source)
        assertEquals(0, ai.calls) // AI never called when disabled.
    }

    @Test
    fun generator_usesAiWhenEnabledAndValid() = runTest {
        val settings = FakeSettingsRepository().apply { setAiEnabled(true) }
        val ai = FakeAiInsightSource(draft = aiDraft(summary = "AI says hello"))
        val useCase = generator(FakeInsightRepository(), settings, ai)

        val insight = useCase(todayEpoch)

        assertEquals(InsightSource.AI, insight.source)
        assertEquals("AI says hello", insight.summary)
        assertEquals(1, ai.calls)
    }

    @Test
    fun generator_fallsBackWhenAiResponseInvalid() = runTest {
        val settings = FakeSettingsRepository().apply { setAiEnabled(true) }
        // Invalid: blank summary → schema rejects → rule-based.
        val ai = FakeAiInsightSource(draft = aiDraft(summary = " "))
        val useCase = generator(FakeInsightRepository(), settings, ai)

        val insight = useCase(todayEpoch)

        assertEquals(InsightSource.RULE_BASED, insight.source)
    }

    @Test
    fun generator_fallsBackWhenAiThrows() = runTest {
        val settings = FakeSettingsRepository().apply { setAiEnabled(true) }
        val ai = FakeAiInsightSource(throwOnGenerate = true)
        val useCase = generator(FakeInsightRepository(), settings, ai)

        val insight = useCase(todayEpoch)

        assertEquals(InsightSource.RULE_BASED, insight.source)
    }

    @Test
    fun generator_cachesPerDayUnlessForced() = runTest {
        val repo = FakeInsightRepository()
        val settings = FakeSettingsRepository()
        val ai = FakeAiInsightSource()
        val useCase = generator(repo, settings, ai)

        val first = useCase(todayEpoch)
        val second = useCase(todayEpoch)
        assertEquals(first.id, second.id) // cached.

        val forced = useCase(todayEpoch, force = true)
        assertTrue(forced.id != first.id || repo.insights.value.size >= 1)
        assertEquals(1, repo.insights.value.size) // still single-valued for the day.
    }

    // --- helpers ---

    private fun generator(
        repo: FakeInsightRepository,
        settings: FakeSettingsRepository,
        ai: FakeAiInsightSource,
    ) = GenerateDailyReflectionUseCase(
        insightRepository = repo,
        settingsRepository = settings,
        buildPayload = BuildInsightPayloadUseCase(
            FakeTaskRepository(),
            FakeUsageRepository(),
            FakeFocusRepository(),
            time(),
        ),
        ruleBasedSource = RuleBasedInsightSource(),
        aiInsightSource = ai,
        timeProvider = time(),
        idProvider = FakeIdProvider(),
    )

    private fun payload(
        tasksCompleted: Int,
        tasksPlanned: Int,
        focusMs: Long,
        screenTimeMs: Long?,
    ) = InsightPayload(
        dateEpochDay = todayEpoch,
        tasksCompleted = tasksCompleted,
        tasksPlanned = tasksPlanned,
        focusTimeMs = focusMs,
        screenTimeMs = screenTimeMs,
        longestDistractionMs = null,
        topDistractionCategory = null,
        productiveHours = emptyList(),
    )

    private fun draft(summary: String, risk: String?) = InsightDraft(
        summary = summary,
        patterns = emptyList(),
        riskLevel = risk,
        suggestions = emptyList(),
        recommendedAction = null,
    )

    private fun aiDraft(summary: String = "AI insight") = InsightDraft(
        summary = summary,
        patterns = listOf("AI pattern"),
        riskLevel = "low",
        suggestions = listOf("AI suggestion"),
        recommendedAction = "start_focus",
    )

    private fun doneTask(id: String, completedAt: Long = fixedNow) = Task(
        id = id,
        title = "Task $id",
        priority = TaskPriority.HIGH,
        status = TaskStatus.DONE,
        completedAt = completedAt,
        createdAt = fixedNow,
    )

    private fun openTask(id: String, deadline: Long? = null) = Task(
        id = id,
        title = "Task $id",
        priority = TaskPriority.MEDIUM,
        status = TaskStatus.TODO,
        deadline = deadline,
        createdAt = fixedNow,
    )
}
