package com.anchor.app.feature.tasks

import androidx.lifecycle.SavedStateHandle
import com.anchor.app.core.common.FakeIdProvider
import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.core.domain.usecase.task.ManageSubtasksUseCase
import com.anchor.app.core.domain.usecase.task.RescheduleTaskUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val utc = ZoneOffset.UTC
    private val fixedNow: Long = 1700000000000L

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

    private fun createViewModel(taskId: String): TaskDetailViewModel {
        val handle = SavedStateHandle(mapOf(TaskDetailViewModel.ARG_TASK_ID to taskId))
        return TaskDetailViewModel(
            savedStateHandle = handle,
            taskRepository = tasks,
            rescheduleTask = RescheduleTaskUseCase(tasks, defs, FakeIdProvider("res"), time),
            manageSubtasks = ManageSubtasksUseCase(tasks, FakeIdProvider("sub")),
            timeProvider = time,
            dispatchers = TestDispatcherProvider(dispatcher),
        )
    }

    @Test
    fun initialState_hasIsLoadingTrue() {
        val defaultState = TaskDetailUiState()
        assertTrue(defaultState.isLoading)
        assertNull(defaultState.task)
    }

    @Test
    fun uiState_transitionsFromLoadingToLoaded() = runTest(dispatcher) {
        val taskId = "task-1"
        val testTask = Task(
            id = taskId,
            title = "Test Task",
            priority = TaskPriority.HIGH,
            status = TaskStatus.TODO,
            createdAt = fixedNow,
        )
        tasks.tasks.value = listOf(testTask)

        val vm = createViewModel(taskId)
        val job = backgroundScope.launch { vm.uiState.collect { } }

        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Test Task", state.task?.title)

        job.cancel()
    }
}
