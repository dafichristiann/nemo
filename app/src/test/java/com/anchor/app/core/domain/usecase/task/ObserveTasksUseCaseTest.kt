package com.anchor.app.core.domain.usecase.task

import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.testutil.FakeTaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Tests for the observe/filter use cases backing the Home list and overdue banner
 * (docs 11-testing-strategy.md §2.1, PRD FR-T10 / FR-T11). Day boundaries are derived
 * from [FakeTimeProvider] so "Today" is deterministic.
 */
class ObserveTasksUseCaseTest {

    private val utc = ZoneOffset.UTC
    private val fixedNow: Long =
        ZonedDateTime.of(2026, 10, 7, 9, 30, 0, 0, utc).toInstant().toEpochMilli()

    private fun task(
        id: String,
        deadline: Long?,
        status: TaskStatus = TaskStatus.TODO,
    ) = Task(
        id = id,
        title = id,
        deadline = deadline,
        priority = TaskPriority.MEDIUM,
        status = status,
        createdAt = fixedNow,
    )

    private fun setup(): Pair<FakeTaskRepository, FakeTimeProvider> {
        val repo = FakeTaskRepository()
        val time = FakeTimeProvider(fixedNow, utc)
        return repo to time
    }

    @Test
    fun all_returnsEveryTask() = runTest {
        val (repo, time) = setup()
        val startOfDay = time.startOfDayMillis(fixedNow)
        repo.tasks.value = listOf(
            task("today", startOfDay + 3_600_000),
            task("done", startOfDay + 7_200_000, TaskStatus.DONE),
        )

        val useCase = ObserveTasksUseCase(repo, time)
        assertEquals(setOf("today", "done"), useCase(TaskFilter.ALL).first().map { it.id }.toSet())
    }

    @Test
    fun today_includesOnlyOpenTasksWithinLocalDay() = runTest {
        val (repo, time) = setup()
        val startOfDay = time.startOfDayMillis(fixedNow)
        repo.tasks.value = listOf(
            task("in-day", startOfDay + 3_600_000),
            task("late-night-in-day", startOfDay + 23 * 3_600_000L + 59 * 60_000L),
            task("tomorrow", startOfDay + 86_400_000L),
            task("yesterday", startOfDay - 1),
            task("done-in-day", startOfDay + 7_200_000, TaskStatus.DONE),
            task("no-deadline", null),
        )

        val ids = ObserveTasksUseCase(repo, time)(TaskFilter.TODAY).first().map { it.id }.toSet()
        assertEquals(setOf("in-day", "late-night-in-day"), ids)
    }

    @Test
    fun upcoming_includesOpenTasksFromTomorrowOnward() = runTest {
        val (repo, time) = setup()
        val startOfDay = time.startOfDayMillis(fixedNow)
        val endOfToday = startOfDay + 86_400_000L
        repo.tasks.value = listOf(
            task("now", startOfDay + 3_600_000),
            task("tomorrow", endOfToday),
            task("next-week", endOfToday + 7 * 86_400_000L),
            task("done-future", endOfToday + 3_600_000, TaskStatus.DONE),
        )

        val ids = ObserveTasksUseCase(repo, time)(TaskFilter.UPCOMING).first().map { it.id }.toSet()
        assertEquals(setOf("tomorrow", "next-week"), ids)
    }

    @Test
    fun completed_returnsOnlyDoneTasks() = runTest {
        val (repo, time) = setup()
        val startOfDay = time.startOfDayMillis(fixedNow)
        repo.tasks.value = listOf(
            task("open", startOfDay + 3_600_000),
            task("done", startOfDay + 7_200_000, TaskStatus.DONE),
        )

        val ids = ObserveTasksUseCase(repo, time)(TaskFilter.COMPLETED).first().map { it.id }.toSet()
        assertEquals(setOf("done"), ids)
    }

    @Test
    fun overdue_useCase_returnsOpenTasksPastDeadline() = runTest {
        val (repo, time) = setup()
        repo.tasks.value = listOf(
            task("late", fixedNow - 1),
            task("future", fixedNow + 1),
            task("done-late", fixedNow - 1, TaskStatus.DONE),
        )

        val ids = ObserveOverdueTasksUseCase(repo, time)().first().map { it.id }.toSet()
        assertEquals(setOf("late"), ids)
    }

    @Test
    fun overdue_useCase_respectsDayBoundaryInDeviceZone() = runTest {
        // 23:30 UTC Oct 7 is already Oct 8 in UTC+2 → the deadline is "yesterday" there.
        val instant = ZonedDateTime.of(2026, 10, 7, 23, 30, 0, 0, utc).toInstant().toEpochMilli()
        val repo = FakeTaskRepository()
        val time = FakeTimeProvider(instant, java.time.ZoneId.of("+02:00"))
        repo.tasks.value = listOf(task("due-earlier-today", instant - 3_600_000))

        val ids = ObserveOverdueTasksUseCase(repo, time)().first().map { it.id }
        assertEquals(listOf("due-earlier-today"), ids)
    }
}
