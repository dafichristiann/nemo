package com.anchor.app.core.domain.usecase.futureview

import com.anchor.app.core.common.FakeIdProvider
import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.domain.model.DependencyType
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.repository.TaskDependencyRepository.AddDependencyResult
import com.anchor.app.testutil.FakeTaskDependencyRepository
import com.anchor.app.testutil.FakeTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for Future View + Domino (FR-FV1..FV5, FR-DE1..DE5; AC §10.6-10.7,
 * docs 07-screens.md §2.11). Deterministic via fakes (§11-testing §5).
 */
class FutureViewAndDominoTest {

    private val utc = ZoneOffset.UTC
    // 2026-10-07 15:10 UTC — the PRD §10.6 example moment.
    private val fixedNow: Long =
        ZonedDateTime.of(2026, 10, 7, 15, 10, 0, 0, utc).toInstant().toEpochMilli()

    private fun time() = FakeTimeProvider(fixedNow, utc)

    private fun task(
        id: String,
        title: String = "Task $id",
        durationMin: Int? = null,
        deadline: Long? = null,
    ) = Task(
        id = id,
        title = title,
        priority = TaskPriority.HIGH,
        estimatedDurationMin = durationMin,
        deadline = deadline,
        createdAt = fixedNow,
    )

    // --- FutureViewCalculator (FR-FV1, FV2, FV5) ---

    @Test
    fun withDurationAndDeadline_projectsBothScenarios() {
        val deadline = fixedNow + 5 * 60 * 60 * 1000L // 20:10
        val view = FutureViewCalculator(time())(
            task("t1", durationMin = 120, deadline = deadline),
        )

        assertEquals(120, view.estimatedDurationMin)
        assertEquals(fixedNow, view.startNow.startAt)
        assertEquals(fixedNow + 120 * 60_000L, view.startNow.endAt)
        assertEquals(deadline, view.startNow.deadlineAt)
        // Delay scenario starts later than "now" but never before now.
        assertTrue(view.delay.startAt > fixedNow)
        // No assumptions: nothing to flag.
        assertTrue(view.assumptions.isEmpty())
    }

    @Test
    fun withoutDuration_usesDefaultAndFlagsAssumption() {
        val view = FutureViewCalculator(time())(
            task("t1", durationMin = null, deadline = fixedNow + 60 * 60 * 1000L),
        )

        assertEquals(FutureViewCalculator.DEFAULT_TASK_DURATION_MIN, view.estimatedDurationMin)
        assertTrue(view.usingAssumedDuration)
        assertTrue(FutureViewCalculator.ASSUMPTION_DURATION in view.assumptions)
    }

    @Test
    fun withoutDeadline_usesSameDayDefaultAndFlagsAssumption() {
        val view = FutureViewCalculator(time())(task("t1", durationMin = 30))

        assertTrue(view.usingAssumedDeadline)
        assertNotNull(view.startNow.deadlineAt)
        // 23:59 same day (UTC) — the locked heuristic (PRD §10.6), not 23:00.
        val expectedDeadline = time().startOfDayMillis(fixedNow) + (23 * 60 + 59) * 60 * 1000L
        assertEquals(expectedDeadline, view.startNow.deadlineAt)
    }

    @Test
    fun delayScenario_neverStartsBeforeNow() {
        // A duration longer than the remaining day: the delay still cannot start before "now".
        val view = FutureViewCalculator(time())(
            task("t1", durationMin = 600, deadline = null),
        )
        assertTrue(view.delay.startAt >= fixedNow)
        assertTrue(view.delay.endAt > view.delay.startAt)
    }

    // --- ComputeDominoEffectUseCase (FR-DE1..DE4) ---

    @Test
    fun domino_listsDownstreamTasksWithDepths() = runTest {
        val tasks = FakeTaskRepository()
        val deps = FakeTaskDependencyRepository()
        tasks.insert(task("a", "Web Assignment"))
        tasks.insert(task("b", "Review"))
        tasks.insert(task("c", "Submit"))
        deps.add("a", "b", DependencyType.BLOCKS)
        deps.add("b", "c", DependencyType.BLOCKS)

        val domino = ComputeDominoEffectUseCase(tasks, deps)("a")!!

        assertEquals("Web Assignment", domino.root.title)
        assertEquals(2, domino.affectedCount)
        assertEquals(listOf("b", "c"), domino.affected.map { it.taskId })
        assertEquals(listOf(1, 2), domino.affected.map { it.depth })
    }

    @Test
    fun domino_isEmptyWhenNoDependencies() = runTest {
        val tasks = FakeTaskRepository()
        tasks.insert(task("a", "Solo task"))
        val domino = ComputeDominoEffectUseCase(tasks, FakeTaskDependencyRepository())("a")!!

        assertEquals(0, domino.affectedCount)
    }

    @Test
    fun domino_nullWhenTaskMissing() = runTest {
        val result = ComputeDominoEffectUseCase(FakeTaskRepository(), FakeTaskDependencyRepository())("missing")
        assertNull(result)
    }

    // --- Anti-cycle (FR-DE1, PRD §10.7 AC) ---

    @Test
    fun addDependency_rejectsDirectCycle() = runTest {
        val deps = FakeTaskDependencyRepository()
        assertTrue(deps.add("a", "b") is AddDependencyResult.Added)

        // b -> a would close a -> b -> a.
        val result = deps.add("b", "a")
        assertTrue(result is AddDependencyResult.Cycle)
        assertEquals(1, deps.getAll().size) // nothing persisted.
    }

    @Test
    fun addDependency_rejectsIndirectCycle() = runTest {
        val deps = FakeTaskDependencyRepository()
        deps.add("a", "b")
        deps.add("b", "c")

        // c -> a closes a -> b -> c -> a.
        assertTrue(deps.add("c", "a") is AddDependencyResult.Cycle)
        assertEquals(2, deps.getAll().size)
    }

    @Test
    fun addDependency_rejectsSelfCycle() = runTest {
        val deps = FakeTaskDependencyRepository()
        val result = deps.add("a", "a")
        assertTrue(result is AddDependencyResult.Cycle)
        assertTrue(deps.getAll().isEmpty())
    }

    @Test
    fun addDependency_allowsDiamondWithoutCycle() = runTest {
        val deps = FakeTaskDependencyRepository()
        deps.add("a", "b")
        deps.add("a", "c")
        // b -> d and c -> d is a diamond, not a cycle.
        assertTrue(deps.add("b", "d") is AddDependencyResult.Added)
        assertTrue(deps.add("c", "d") is AddDependencyResult.Added)
        assertEquals(4, deps.getAll().size)
    }
}
