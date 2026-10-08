package com.anchor.app.core.domain.usecase.task

import com.anchor.app.core.common.FakeIdProvider
import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.domain.model.RecurrenceRule
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.testutil.FakeTaskDefinitionRepository
import com.anchor.app.testutil.FakeTaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for task use cases (docs 11-testing-strategy.md §2.1 Smart Todo).
 * Deterministic via [FakeTimeProvider] + [FakeIdProvider] (§5).
 */
class TaskUseCasesTest {

    private val fixedNow: Long =
        ZonedDateTime.of(2026, 10, 7, 9, 30, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun fakes() = Triple(
        FakeTaskRepository(),
        FakeTaskDefinitionRepository(),
        FakeTimeProvider(fixedNow, ZoneOffset.UTC),
    )

    // FR-T1/T2/T5: create stores the task and appears for today.
    @Test
    fun createTask_savesTaskWithAllFields() = runTest {
        val (tasks, defs, time) = fakes()
        val useCase = CreateTaskUseCase(tasks, defs, FakeIdProvider(), time)

        val deadline = fixedNow + 60_000
        val result = useCase(
            CreateTaskInput(
                title = "Kerjakan tugas Web",
                description = "Bab 3",
                deadline = deadline,
                priority = TaskPriority.HIGH,
                estimatedDurationMin = 120,
                category = "Study",
            ),
        )

        assertTrue(result is CreateTaskResult.Success)
        val saved = tasks.tasks.value.single()
        assertEquals("Kerjakan tugas Web", saved.title)
        assertEquals(TaskPriority.HIGH, saved.priority)
        assertEquals(120, saved.estimatedDurationMin)
        assertEquals("Study", saved.category)
        assertEquals(deadline, saved.deadline)
        assertEquals(TaskStatus.TODO, saved.status)
    }

    // FR §10.1 Edge Cases: empty title cannot be saved.
    @Test
    fun createTask_rejectsEmptyTitle() = runTest {
        val (tasks, defs, time) = fakes()
        val useCase = CreateTaskUseCase(tasks, defs, FakeIdProvider(), time)

        val result = useCase(CreateTaskInput(title = "   "))

        assertTrue(result is CreateTaskResult.EmptyTitle)
        assertTrue(tasks.tasks.value.isEmpty())
    }

    // FR-T7: a recurring create also stores a definition and stamps the occurrence date.
    @Test
    fun createRecurringTask_storesDefinitionAndLinkedInstance() = runTest {
        val (tasks, defs, time) = fakes()
        val useCase = CreateTaskUseCase(tasks, defs, FakeIdProvider(), time)

        useCase(CreateTaskInput(title = "Daily standup", recurrenceRule = RecurrenceRule.DAILY))

        val definition = defs.definitions.value.single()
        val task = tasks.tasks.value.single()
        assertEquals(RecurrenceRule.DAILY, definition.recurrenceRule)
        assertNotNull(task.definitionId)
        assertEquals(definition.id, task.definitionId)
        assertNotNull(task.definitionOccurrenceDate)
    }

    // FR-T8: complete sets DONE, records completedAt, progress 100.
    @Test
    fun completeTask_setsDoneAndTimestamp() = runTest {
        val (tasks, defs, time) = fakes()
        val create = CreateTaskUseCase(tasks, defs, FakeIdProvider(), time)
        val complete = CompleteTaskUseCase(tasks, time)
        val id = (create(CreateTaskInput(title = "Task")) as CreateTaskResult.Success).taskId

        complete(id)

        val task = tasks.tasks.value.single { it.id == id }
        assertEquals(TaskStatus.DONE, task.status)
        assertEquals(100, task.progressPercent)
        assertEquals(fixedNow, task.completedAt)
    }

    // FR-T9: progress is computed from subtasks; empty → 0.
    @Test
    fun subtaskProgress_isComputedAndEmptyIsZero() = runTest {
        val (tasks, defs, time) = fakes()
        val create = CreateTaskUseCase(tasks, defs, FakeIdProvider(), time)
        val manage = ManageSubtasksUseCase(tasks, FakeIdProvider())
        val id = (create(CreateTaskInput(title = "Task")) as CreateTaskResult.Success).taskId

        assertEquals(0, tasks.tasks.value.single { it.id == id }.progressPercent)

        manage.add(id, "Step 1")
        manage.add(id, "Step 2")
        assertEquals(0, tasks.tasks.value.single { it.id == id }.progressPercent)

        val first = tasks.getSubtasks(id).first()
        manage.toggle(first)
        assertEquals(50, tasks.tasks.value.single { it.id == id }.progressPercent)
    }

    // FR-T12: reschedule updates deadline and records a RescheduleEvent.
    @Test
    fun reschedule_recordsEvent() = runTest {
        val (tasks, defs, time) = fakes()
        val create = CreateTaskUseCase(tasks, defs, FakeIdProvider(), time)
        val reschedule = RescheduleTaskUseCase(tasks, defs, FakeIdProvider("res"), time)
        val id = (create(CreateTaskInput(title = "Task", deadline = fixedNow - 1000)) as CreateTaskResult.Success).taskId

        val newDeadline = fixedNow + 86_400_000
        reschedule(id, newDeadline)

        assertEquals(newDeadline, tasks.tasks.value.single { it.id == id }.deadline)
        val event = defs.reschedules.value.single()
        assertEquals(fixedNow - 1000, event.fromDeadline)
        assertEquals(newDeadline, event.toDeadline)
        assertEquals(id, event.taskId)
    }

    // FR-T7: a freshly created recurring task already covers today, so the generator must NOT
    // add a duplicate for the same occurrence day (AC "no double instance"). This guards the
    // occurrence-date unit consistency between CreateTaskUseCase (epoch day) and the generator.
    @Test
    fun recurringGenerator_doesNotDuplicateFreshlyCreatedTask() = runTest {
        val (tasks, defs, time) = fakes()
        val create = CreateTaskUseCase(tasks, defs, FakeIdProvider(), time)
        create(CreateTaskInput(title = "Daily", recurrenceRule = RecurrenceRule.DAILY))

        val generate = GenerateRecurringTasksUseCase(tasks, defs, FakeIdProvider("gen"), time)

        val firstRun = generate(time.today())
        val secondRun = generate(time.today())

        assertEquals(0, firstRun)
        assertEquals(0, secondRun)
        // Exactly one instance for today — no duplicate.
        assertEquals(1, tasks.tasks.value.size)
        val todayEpoch = time.today().toEpochDay()
        assertEquals(todayEpoch, tasks.tasks.value.single().definitionOccurrenceDate)
    }

    // FR-T7: a recurring task created with a deadline uses the LOCAL DAY of that deadline as its
    // occurrence key, matching the generator's `today.toEpochDay()` guard.
    @Test
    fun recurringTask_occurrenceDateIsEpochDayOfDeadline() = runTest {
        val (tasks, defs, time) = fakes()
        val create = CreateTaskUseCase(tasks, defs, FakeIdProvider(), time)
        val deadline = fixedNow + 5 * 60 * 60 * 1000L // same local day

        create(CreateTaskInput(title = "Daily", deadline = deadline, recurrenceRule = RecurrenceRule.DAILY))

        val saved = tasks.tasks.value.single()
        assertEquals(time.epochDayOf(deadline), saved.definitionOccurrenceDate)
        // And the generator agrees nothing new is due today.
        val generate = GenerateRecurringTasksUseCase(tasks, defs, FakeIdProvider("gen"), time)
        assertEquals(0, generate(time.today()))
    }

    // FR-T7 / AC "no double instance": second run on the same day adds nothing.
    @Test
    fun recurringGenerator_noDuplicateForSameOccurrence() = runTest {
        val (tasks, defs, time) = fakes()
        val definition = com.anchor.app.core.domain.model.TaskDefinition(
            id = "def-1",
            title = "Daily",
            recurrenceRule = RecurrenceRule.DAILY,
            createdAt = fixedNow,
        )
        defs.insert(definition)
        val generate = GenerateRecurringTasksUseCase(tasks, defs, FakeIdProvider("gen"), time)

        generate(time.today())
        generate(time.today())

        val occurrences = tasks.tasks.value.count {
            it.definitionId == "def-1" && it.definitionOccurrenceDate == time.today().toEpochDay()
        }
        assertEquals(1, occurrences)
    }

    // Non-recurring tasks never produce extra instances.
    @Test
    fun recurringGenerator_ignoresNonRecurringDefinitions() = runTest {
        val (tasks, defs, time) = fakes()
        val definition = com.anchor.app.core.domain.model.TaskDefinition(
            id = "def-2",
            title = "One-off template",
            recurrenceRule = RecurrenceRule.NONE,
            createdAt = fixedNow,
        )
        defs.insert(definition)
        val generate = GenerateRecurringTasksUseCase(tasks, defs, FakeIdProvider("gen"), time)

        assertEquals(0, generate(time.today()))
        assertTrue(tasks.tasks.value.isEmpty())
    }

    // Overdue detection is a pure function of deadline + now (FR-T11).
    @Test
    fun overdue_isDerivedFromDeadline() = runTest {
        val (tasks, defs, time) = fakes()
        val create = CreateTaskUseCase(tasks, defs, FakeIdProvider(), time)
        val overdueId = (create(CreateTaskInput(title = "Late", deadline = fixedNow - 1)) as CreateTaskResult.Success).taskId
        val futureId = (create(CreateTaskInput(title = "Later", deadline = fixedNow + 1)) as CreateTaskResult.Success).taskId

        val overdue = tasks.observeOverdue(fixedNow).first()
        assertEquals(listOf(overdueId), overdue.map { it.id })
        assertNull(overdue.find { it.id == futureId })
    }
}
