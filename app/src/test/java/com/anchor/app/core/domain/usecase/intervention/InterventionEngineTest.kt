package com.anchor.app.core.domain.usecase.intervention

import com.anchor.app.core.common.FakeIdProvider
import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.domain.model.DriftSignal
import com.anchor.app.core.domain.model.InterventionAction
import com.anchor.app.core.domain.model.InterventionChannel
import com.anchor.app.core.domain.model.InterventionDecision
import com.anchor.app.core.domain.model.InterventionLevel
import com.anchor.app.core.domain.model.InterventionStatus
import com.anchor.app.core.domain.model.SuppressReason
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.testutil.FakeInterventionRepository
import com.anchor.app.testutil.FakeTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for the Intervention Engine (FR-I1..I4, I6, I7; AC §10.4). Verifies quota,
 * cooldown, level escalation and the Detection-vs-Attempt separation
 * (docs/superpowers/design/04-intervention-engine.md §3, §5, PRD §10.4).
 */
class InterventionEngineTest {

    private val utc = ZoneOffset.UTC
    private val fixedNow: Long =
        ZonedDateTime.of(2026, 10, 7, 20, 0, 0, 0, utc).toInstant().toEpochMilli()

    private fun engine(
        interventions: FakeInterventionRepository,
        tasks: FakeTaskRepository,
        now: Long = fixedNow,
    ) = InterventionEngine(
        interventionRepository = interventions,
        taskRepository = tasks,
        timeProvider = FakeTimeProvider(now, utc),
        idProvider = FakeIdProvider(),
    )

    private fun signal(pkg: String = "com.tiktok", taskId: String? = "t1") = DriftSignal(
        packageName = pkg,
        currentSessionMs = 40 * 60_000L,
        baselineAvgMs = 20 * 60_000L,
        deviationSigma = 4.0,
        relatedTaskId = taskId,
        detectedAt = fixedNow,
    )

    private fun task(
        id: String = "t1",
        priority: TaskPriority = TaskPriority.HIGH,
        estimate: Int? = null,
        deadline: Long? = null,
    ) = Task(
        id = id,
        title = "Task $id",
        priority = priority,
        status = TaskStatus.TODO,
        estimatedDurationMin = estimate,
        deadline = deadline,
        createdAt = fixedNow,
    )

    // --- Basic decisions (FR-I1, FR-I2) ---

    @Test
    fun noSignal_isSuppressedAsNoDrift() = runTest {
        val decision = engine(FakeInterventionRepository(), FakeTaskRepository()).evaluate(null)
        assertEquals(InterventionDecision.Suppress(SuppressReason.NO_DRIFT), decision)
    }

    @Test
    fun withSignalAndQuota_showsContextualWhenTaskHasNoEstimate() = runTest {
        val tasks = FakeTaskRepository().apply { insert(task(estimate = null)) }
        val decision = engine(FakeInterventionRepository(), tasks)
            .evaluate(signal(), notificationPermissionGranted = true)

        val show = decision as InterventionDecision.Show
        assertEquals(InterventionLevel.CONTEXTUAL, show.level)
        assertEquals(InterventionChannel.NOTIFICATION, show.channel)
    }

    @Test
    fun withEstimate_escalatesToFutureView() = runTest {
        val tasks = FakeTaskRepository().apply { insert(task(estimate = 60)) }
        val show = engine(FakeInterventionRepository(), tasks)
            .evaluate(signal()) as InterventionDecision.Show
        assertEquals(InterventionLevel.FUTURE_VIEW, show.level)
    }

    @Test
    fun withoutNotificationPermission_fallsBackToInApp() = runTest {
        val tasks = FakeTaskRepository().apply { insert(task()) }
        val show = engine(FakeInterventionRepository(), tasks)
            .evaluate(signal(), notificationPermissionGranted = false) as InterventionDecision.Show
        assertEquals(InterventionChannel.IN_APP, show.channel)
    }

    @Test
    fun userInsideAnchor_isSuppressed() = runTest {
        val tasks = FakeTaskRepository().apply { insert(task()) }
        val decision = engine(FakeInterventionRepository(), tasks)
            .evaluate(signal(), userInsideAnchor = true)
        assertEquals(InterventionDecision.Suppress(SuppressReason.USER_IN_APP), decision)
    }

    // --- Level 4 is gated (never a cold jump, FR-I2) ---

    @Test
    fun levelFour_requiresPriorEngagementAtLevelThree() = runTest {
        val tasks = FakeTaskRepository().apply { insert(task(estimate = 60)) }
        // No history: escalation caps at Future View, never Focus.
        val first = engine(FakeInterventionRepository(), tasks).evaluate(signal())
        assertEquals(InterventionLevel.FUTURE_VIEW, (first as InterventionDecision.Show).level)
    }

    // --- Quota (FR-I6, AC "3 meaningful/day") ---

    @Test
    fun quotaExhausted_suppressesWithoutRecording() = runTest {
        val interventions = FakeInterventionRepository()
        val tasks = FakeTaskRepository().apply { insert(task()) }
        val engine = engine(interventions, tasks)

        // Spend the whole daily quota with three meaningful interventions.
        repeat(InterventionEngine.MAX_DAILY_INTERVENTIONS) {
            val id = engine.recordShown(engine.evaluate(signal()) as InterventionDecision.Show)
            engine.recordAction(id, InterventionAction.DISMISS)
        }
        assertEquals(0, engine.quotaRemaining())

        val decision = engine.evaluate(signal())
        assertEquals(InterventionDecision.Suppress(SuppressReason.QUOTA_EXHAUSTED), decision)
        // A suppressed detection consumes no quota (Detection != Attempt).
        assertEquals(0, engine.quotaRemaining())
    }

    @Test
    fun expiredIntervention_doesNotConsumeQuota() = runTest {
        val interventions = FakeInterventionRepository()
        val tasks = FakeTaskRepository().apply { insert(task()) }
        val engine = engine(interventions, tasks)

        val id = engine.recordShown(engine.evaluate(signal()) as InterventionDecision.Show)
        engine.recordAction(id, InterventionAction.EXPIRE)

        assertEquals(InterventionEngine.MAX_DAILY_INTERVENTIONS, engine.quotaRemaining())
    }

    @Test
    fun interactedIntervention_consumesOneQuota() = runTest {
        val interventions = FakeInterventionRepository()
        val tasks = FakeTaskRepository().apply { insert(task()) }
        val engine = engine(interventions, tasks)

        val id = engine.recordShown(engine.evaluate(signal()) as InterventionDecision.Show)
        engine.recordAction(id, InterventionAction.START_FOCUS)

        assertEquals(InterventionEngine.MAX_DAILY_INTERVENTIONS - 1, engine.quotaRemaining())
    }

    @Test
    fun quotaResetsOnNewLocalDay() = runTest {
        val interventions = FakeInterventionRepository()
        val tasks = FakeTaskRepository().apply { insert(task()) }
        val engine = engine(interventions, tasks)

        repeat(InterventionEngine.MAX_DAILY_INTERVENTIONS) {
            val id = engine.recordShown(engine.evaluate(signal()) as InterventionDecision.Show)
            engine.recordAction(id, InterventionAction.DISMISS)
        }
        assertEquals(0, engine.quotaRemaining())

        // Next day, same wall-clock-ish offset → fresh quota.
        val tomorrow = fixedNow + 24 * 60 * 60 * 1000L
        assertEquals(InterventionEngine.MAX_DAILY_INTERVENTIONS, engine.quotaRemaining(tomorrow))
    }

    // --- Cooldown after "Later" (FR-I4, AC "Later → 2h cooldown") ---

    @Test
    fun later_setsTwoHourCooldownForSameContext() = runTest {
        val interventions = FakeInterventionRepository()
        val tasks = FakeTaskRepository().apply { insert(task()) }
        val engine = engine(interventions, tasks)

        val id = engine.recordShown(engine.evaluate(signal()) as InterventionDecision.Show)
        engine.recordAction(id, InterventionAction.LATER)

        assertTrue(engine.cooldownActive("com.tiktok"))
        // A different app/context is unaffected.
        assertFalse(engine.cooldownActive("com.instagram"))
    }

    @Test
    fun duringCooldown_sameContextIsSuppressed() = runTest {
        val interventions = FakeInterventionRepository()
        val tasks = FakeTaskRepository().apply { insert(task()) }
        val engine = engine(interventions, tasks)

        val id = engine.recordShown(engine.evaluate(signal()) as InterventionDecision.Show)
        engine.recordAction(id, InterventionAction.LATER)

        // 30 minutes later, still within the 2-hour window → SUPPRESS("cooldown").
        val thirtyMinLater = fixedNow + 30 * 60 * 1000L
        assertEquals(
            InterventionDecision.Suppress(SuppressReason.COOLDOWN),
            engine.evaluate(signal("com.tiktok"), now = thirtyMinLater),
        )
        // A different context is NOT suppressed by this cooldown.
        assertTrue(engine.evaluate(signal("com.instagram"), now = thirtyMinLater) is InterventionDecision.Show)
    }

    @Test
    fun afterTwoHours_cooldownExpires() = runTest {
        val interventions = FakeInterventionRepository()
        val tasks = FakeTaskRepository().apply { insert(task()) }
        val engine = engine(interventions, tasks)

        val id = engine.recordShown(engine.evaluate(signal()) as InterventionDecision.Show)
        engine.recordAction(id, InterventionAction.LATER)

        val justOverTwoHours = fixedNow + InterventionEngine.COOLDOWN_AFTER_LATER_MS + 1
        assertFalse(engine.cooldownActive("com.tiktok", justOverTwoHours))
        assertTrue(engine.evaluate(signal("com.tiktok"), now = justOverTwoHours) is InterventionDecision.Show)
    }

    // --- recordShown / recordAction persist the right statuses (FR-I5) ---

    @Test
    fun recordAction_mapsEachActionToItsStatus() = runTest {
        val interventions = FakeInterventionRepository()
        val tasks = FakeTaskRepository().apply { insert(task()) }
        val engine = engine(interventions, tasks)

        val interacted = engine.recordShown(engine.evaluate(signal()) as InterventionDecision.Show)
        engine.recordAction(interacted, InterventionAction.START_FOCUS)
        assertEquals(InterventionStatus.INTERACTED, interventions.getById(interacted)!!.status)

        val dismissed = engine.recordShown(engine.evaluate(signal()) as InterventionDecision.Show)
        engine.recordAction(dismissed, InterventionAction.DISMISS)
        assertEquals(InterventionStatus.DISMISSED, interventions.getById(dismissed)!!.status)

        val expired = engine.recordShown(engine.evaluate(signal()) as InterventionDecision.Show)
        engine.recordAction(expired, InterventionAction.EXPIRE)
        assertEquals(InterventionStatus.EXPIRED, interventions.getById(expired)!!.status)
    }
}
