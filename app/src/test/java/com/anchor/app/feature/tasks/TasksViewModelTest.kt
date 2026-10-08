package com.anchor.app.feature.tasks

import com.anchor.app.core.common.FakeIdProvider
import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.core.domain.usecase.task.CompleteTaskUseCase
import com.anchor.app.core.domain.usecase.task.CreateTaskUseCase
import com.anchor.app.core.domain.usecase.task.GenerateRecurringTasksUseCase
import com.anchor.app.core.domain.usecase.task.ObserveOverdueTasksUseCase
import com.anchor.app.core.domain.usecase.task.ObserveTasksUseCase
import com.anchor.app.core.domain.usecase.task.RescheduleTaskUseCase
import com.anchor.app.core.domain.usecase.task.TaskFilter
import com.anchor.app.core.domain.model.Task
import com.anchor.app.testutil.FakeTaskDefinitionRepository
import com.anchor.app.testutil.FakeTaskRepository
import com.anchor.app.testutil.TestDispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Tests for [TasksViewModel] — the Home (Smart Todo) screen state (PRD FR-T10/T11).
 * Uses an in-memory fake repository and a single test dispatcher (docs 11 §5).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    private val utc = ZoneOffset.UTC
    private val fixedNow: Long =
        ZonedDateTime.of(2026, 10, 7, 9, 30, 0, 0, utc).toInstant().toEpochMilli()

    private val dispatcher = StandardTestDispatcher()

    private lateinit var tasks: FakeTaskRepository
    private lateinit var defs: FakeTaskDefinitionRepository
    private lateinit var time: FakeTimeProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        tasks = FakeTaskRepository()
        defs = FakeTaskDefinitionRepository()
        time = FakeTimeProvider(fixedNow, utc)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TasksViewModel(
        observeTasks = ObserveTasksUseCase(tasks, time),
        observeOverdue = ObserveOverdueTasksUseCase(tasks, time),
        createTask = CreateTaskUseCase(tasks, defs, FakeIdProvider(), time),
        completeTask = CompleteTaskUseCase(tasks, time),
        rescheduleTask = RescheduleTaskUseCase(tasks, defs, FakeIdProvider("res"), time),
        generateRecurring = GenerateRecurringTasksUseCase(tasks, defs, FakeIdProvider("gen"), time),
        timeProvider = time,
        dispatchers = TestDispatcherProvider(dispatcher),
    )

    /**
     * The Home state is a `WhileSubscribed` flow, so it only ticks while it has a
     * collector. Start one in [runTest]'s `backgroundScope` (auto-cancelled) so
     * `uiState.value` reflects the combine without leaving a dangling coroutine.
     */
    private fun kotlinx.coroutines.test.TestScope.keepCollecting(vm: TasksViewModel) {
        backgroundScope.launch { vm.uiState.collect { } }
    }

    private fun task(id: String, deadline: Long?, status: TaskStatus = TaskStatus.TODO) = Task(
        id = id,
        title = id,
        deadline = deadline,
        priority = TaskPriority.MEDIUM,
        status = status,
        createdAt = fixedNow,
    )

    @Test
    fun defaultFilter_isToday_andRowsReflectIt() = runTest(dispatcher) {
        val startOfDay = time.startOfDayMillis(fixedNow)
        tasks.tasks.value = listOf(
            task("today", startOfDay + 3_600_000),
            task("tomorrow", startOfDay + 86_400_000L),
        )
        val vm = viewModel()
        keepCollecting(vm)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TaskFilter.TODAY, state.filter)
        assertEquals(listOf("today"), state.rows.map { it.task.id })
    }

    @Test
    fun setFilter_switchesVisibleRows() = runTest(dispatcher) {
        val startOfDay = time.startOfDayMillis(fixedNow)
        tasks.tasks.value = listOf(
            task("today", startOfDay + 3_600_000),
            task("tomorrow", startOfDay + 86_400_000L),
        )
        val vm = viewModel()
        keepCollecting(vm)
        advanceUntilIdle()

        vm.setFilter(TaskFilter.UPCOMING)
        advanceUntilIdle()

        assertEquals(TaskFilter.UPCOMING, vm.uiState.value.filter)
        assertEquals(listOf("tomorrow"), vm.uiState.value.rows.map { it.task.id })
    }

    @Test
    fun overdueCount_reflectsOverdueOpenTasks() = runTest(dispatcher) {
        tasks.tasks.value = listOf(
            task("late-1", fixedNow - 1),
            task("late-2", fixedNow - 2),
            task("future", fixedNow + 1),
        )
        val vm = viewModel()
        keepCollecting(vm)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.overdueCount)
        assertTrue(vm.uiState.value.rows.find { it.task.id == "late-1" }?.overdue == true)
    }

    @Test
    fun onCreateTask_addsTaskAndReportsSuccess() = runTest(dispatcher) {
        val vm = viewModel()
        keepCollecting(vm)
        advanceUntilIdle()

        var result: com.anchor.app.core.domain.usecase.task.CreateTaskResult? = null
        vm.onCreateTask(
            title = "Belajar matematika",
            description = null,
            deadline = fixedNow + 3_600_000,
            priority = TaskPriority.HIGH,
            estimatedDurationMin = 60,
            category = null,
            subtaskTitles = emptyList(),
            recurrenceRule = com.anchor.app.core.domain.model.RecurrenceRule.NONE,
            onResult = { result = it },
        )
        advanceUntilIdle()

        assertTrue(result is com.anchor.app.core.domain.usecase.task.CreateTaskResult.Success)
        assertEquals(1, tasks.tasks.value.size)
        assertEquals("Belajar matematika", tasks.tasks.value.single().title)
    }

    @Test
    fun onComplete_marksTaskDone() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("t1", fixedNow + 3_600_000))
        val vm = viewModel()
        keepCollecting(vm)
        advanceUntilIdle()

        vm.onComplete("t1")
        advanceUntilIdle()

        assertEquals(TaskStatus.DONE, tasks.tasks.value.single { it.id == "t1" }.status)
    }

    @Test
    fun onKeepForToday_pushesDeadlineToEndOfToday() = runTest(dispatcher) {
        tasks.tasks.value = listOf(task("late", fixedNow - 86_400_000L))
        val vm = viewModel()
        keepCollecting(vm)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.overdueCount)

        vm.onKeepForToday("late")
        advanceUntilIdle()

        val endOfToday = time.startOfDayMillis(fixedNow) + 86_400_000L - 1
        assertEquals(endOfToday, tasks.tasks.value.single { it.id == "late" }.deadline)
        assertEquals(0, vm.uiState.value.overdueCount)
    }
}
