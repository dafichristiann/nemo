package com.anchor.app.core.domain.usecase.focus

import com.anchor.app.core.common.FakeIdProvider
import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.domain.model.CompletionSource
import com.anchor.app.core.domain.model.FocusMode
import com.anchor.app.core.domain.model.FocusStatus
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.testutil.FakeBlockingStrategy
import com.anchor.app.testutil.FakeFocusRepository
import com.anchor.app.testutil.FakeTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for the Focus use cases (FR-F1..F7, AC §10.5, docs 07-screens.md §2.9-2.10).
 * Deterministic via [FakeTimeProvider] + [FakeIdProvider]; blocking via [FakeBlockingStrategy].
 */
class FocusUseCasesTest {

    private val fixedNow: Long =
        ZonedDateTime.of(2026, 10, 7, 9, 0, 0, 0, ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun time() = FakeTimeProvider(fixedNow, ZoneOffset.UTC)

    private fun task(id: String) = Task(id = id, title = id, createdAt = fixedNow)

    // --- StartFocusUseCase ---

    @Test
    fun start_gentle_recordsActiveSessionWithoutBlocking() = runTest {
        val repo = FakeFocusRepository()
        val blocking = FakeBlockingStrategy(available = false)
        val useCase = StartFocusUseCase(repo, blocking, time(), FakeIdProvider())

        val result = useCase(taskId = "t1", mode = FocusMode.GENTLE, plannedDurationSec = 25 * 60)

        assertEquals(FocusMode.GENTLE, result.effectiveMode)
        assertNull(result.fallbackReason)
        val session = repo.getById(result.sessionId)!!
        assertEquals(FocusStatus.ACTIVE, session.status)
        assertEquals(0L, session.actualDurationMs)
        // The task passed in must be attached to the recorded session (FR-F1/F5).
        assertEquals("t1", session.taskId)
        assertNull(blocking.engaged) // Gentle never engages blocking.
    }

    @Test
    fun start_withoutTask_recordsSessionWithNullTask() = runTest {
        val repo = FakeFocusRepository()
        val useCase = StartFocusUseCase(repo, FakeBlockingStrategy(), time(), FakeIdProvider())

        val result = useCase(taskId = null, mode = FocusMode.GENTLE, plannedDurationSec = 25 * 60)

        assertNull(repo.getById(result.sessionId)!!.taskId)
    }

    @Test
    fun start_strict_fallsBackToGentleWhenBlockingUnavailable() = runTest {
        val repo = FakeFocusRepository()
        val blocking = FakeBlockingStrategy(available = false)
        val useCase = StartFocusUseCase(repo, blocking, time(), FakeIdProvider())

        val result = useCase(taskId = "t1", mode = FocusMode.STRICT, plannedDurationSec = 45 * 60)

        assertEquals(FocusMode.GENTLE, result.effectiveMode)
        assertEquals(StartFocusUseCase.REASON_BLOCKING_UNAVAILABLE, result.fallbackReason)
        val session = repo.getById(result.sessionId)!!
        assertEquals(FocusMode.GENTLE, session.mode)
        assertEquals(StartFocusUseCase.REASON_BLOCKING_UNAVAILABLE, session.fallbackReason)
    }

    @Test
    fun start_strict_keepsStrictWhenBlockingAvailable() = runTest {
        val repo = FakeFocusRepository()
        val blocking = FakeBlockingStrategy(available = true)
        val useCase = StartFocusUseCase(repo, blocking, time(), FakeIdProvider())

        val result = useCase(
            taskId = "t1",
            mode = FocusMode.STRICT,
            plannedDurationSec = 45 * 60,
            blockedApps = setOf("com.tiktok"),
        )

        assertEquals(FocusMode.STRICT, result.effectiveMode)
        assertNull(result.fallbackReason)
        assertEquals(setOf("com.tiktok"), blocking.engaged)
        assertEquals(listOf("com.tiktok"), repo.getById(result.sessionId)!!.blockedApps)
    }

    // --- PauseFocusUseCase ---

    @Test
    fun pause_marksPausedAndStoresElapsed() = runTest {
        val repo = FakeFocusRepository()
        val start = StartFocusUseCase(repo, FakeBlockingStrategy(), time(), FakeIdProvider())
        val id = start("t1", FocusMode.GENTLE, 25 * 60).sessionId

        PauseFocusUseCase(repo, time())(id, elapsedMs = 5 * 60_000L)

        val session = repo.getById(id)!!
        assertEquals(FocusStatus.PAUSED, session.status)
        assertEquals(5 * 60_000L, session.actualDurationMs)
        // A paused session is still "current" so the flow can resume it.
        assertEquals(FocusStatus.PAUSED, repo.current()!!.status)
    }

    // --- FinishFocusUseCase ---

    @Test
    fun finish_recordsDurationAndReleasesBlocking() = runTest {
        val repo = FakeFocusRepository()
        val tasks = FakeTaskRepository()
        tasks.insert(task("t1"))
        val blocking = FakeBlockingStrategy(available = true)
        val start = StartFocusUseCase(repo, blocking, time(), FakeIdProvider())
        val id = start("t1", FocusMode.STRICT, 25 * 60, setOf("com.tiktok")).sessionId

        // Full 25-minute session -> verified -> task completed.
        val result = FinishFocusUseCase(repo, blocking, tasks, time())(id, elapsedMs = 25 * 60_000L)

        val session = repo.getById(id)!!
        assertEquals(FocusStatus.FINISHED, session.status)
        assertEquals(25 * 60_000L, session.actualDurationMs)
        assertEquals("t1", result.taskId)
        assertTrue(result.completedTask)
        assertTrue(blocking.released)
        assertNull(repo.current())
    }

    @Test
    fun finish_fullSession_marksTaskDoneVerified() = runTest {
        val repo = FakeFocusRepository()
        val tasks = FakeTaskRepository()
        tasks.insert(task("t1"))
        val start = StartFocusUseCase(repo, FakeBlockingStrategy(), time(), FakeIdProvider())
        val id = start("t1", FocusMode.GENTLE, 25 * 60).sessionId

        val result = FinishFocusUseCase(repo, FakeBlockingStrategy(), tasks, time())(id, 25 * 60_000L)

        assertTrue(result.completedTask)
        val saved = tasks.getById("t1")!!
        assertEquals(TaskStatus.DONE, saved.status)
        assertEquals(CompletionSource.VERIFIED, saved.completionSource)
        assertEquals(fixedNow, saved.completedAt)
        assertEquals(100, saved.progressPercent)
    }

    @Test
    fun finish_shortSession_stillCompletesTask() = runTest {
        val repo = FakeFocusRepository()
        val tasks = FakeTaskRepository()
        tasks.insert(task("t1"))
        val start = StartFocusUseCase(repo, FakeBlockingStrategy(), time(), FakeIdProvider())
        val id = start("t1", FocusMode.GENTLE, 25 * 60).sessionId

        // Finishing early is an explicit "I'm done" -> the task is completed (updated rule).
        val result = FinishFocusUseCase(repo, FakeBlockingStrategy(), tasks, time())(id, 2 * 60_000L)

        assertTrue(result.completedTask)
        val saved = tasks.getById("t1")!!
        assertEquals(TaskStatus.DONE, saved.status)
        assertEquals(CompletionSource.VERIFIED, saved.completionSource)
    }

    @Test
    fun emergencyExit_doesNotCompleteTaskEvenIfLongEnough() = runTest {
        val repo = FakeFocusRepository()
        val tasks = FakeTaskRepository()
        tasks.insert(task("t1"))
        val start = StartFocusUseCase(repo, FakeBlockingStrategy(), time(), FakeIdProvider())
        val id = start("t1", FocusMode.GENTLE, 25 * 60).sessionId

        val result = FinishFocusUseCase(repo, FakeBlockingStrategy(), tasks, time())(
            id,
            elapsedMs = 30 * 60_000L,
            aborted = true,
        )

        assertFalse(result.completedTask)
        assertEquals(TaskStatus.TODO, tasks.getById("t1")!!.status)
    }

    @Test
    fun finish_sessionWithoutTask_completesNothing() = runTest {
        val repo = FakeFocusRepository()
        val tasks = FakeTaskRepository()
        val start = StartFocusUseCase(repo, FakeBlockingStrategy(), time(), FakeIdProvider())
        val id = start(null, FocusMode.GENTLE, 25 * 60).sessionId

        val result = FinishFocusUseCase(repo, FakeBlockingStrategy(), tasks, time())(id, 25 * 60_000L)

        assertFalse(result.completedTask)
        assertNull(result.taskId)
    }

    @Test
    fun emergencyExit_abortsSessionAndReleasesBlocking() = runTest {
        val repo = FakeFocusRepository()
        val tasks = FakeTaskRepository()
        val blocking = FakeBlockingStrategy(available = true)
        val start = StartFocusUseCase(repo, blocking, time(), FakeIdProvider())
        val id = start("t1", FocusMode.STRICT, 25 * 60, setOf("com.tiktok")).sessionId

        val result = FinishFocusUseCase(repo, blocking, tasks, time())(id, elapsedMs = 2 * 60_000L, aborted = true)

        assertEquals(FocusStatus.ABORTED, repo.getById(id)!!.status)
        assertTrue(result.aborted)
        assertTrue(blocking.released) // Emergency exit must always release blocking.
    }

    @Test
    fun totalDuration_sumsFinishedSessionsInWindow() = runTest {
        val repo = FakeFocusRepository()
        val tasks = FakeTaskRepository()
        tasks.insert(task("t1"))
        tasks.insert(task("t2"))
        val start = StartFocusUseCase(repo, FakeBlockingStrategy(), time(), FakeIdProvider())
        val finish = FinishFocusUseCase(repo, FakeBlockingStrategy(), tasks, time())

        val a = start("t1", FocusMode.GENTLE, 25 * 60).sessionId
        finish(a, 20 * 60_000L)
        val b = start("t2", FocusMode.GENTLE, 25 * 60).sessionId
        finish(b, 15 * 60_000L)

        val total = repo.totalDurationBetween(fixedNow - 1, fixedNow + 1)
        assertEquals(35 * 60_000L, total)
        assertFalse(repo.sessions.value.isEmpty())
    }
}
