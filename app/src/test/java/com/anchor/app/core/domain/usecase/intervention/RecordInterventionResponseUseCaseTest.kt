package com.anchor.app.core.domain.usecase.intervention

import com.anchor.app.core.common.FakeIdProvider
import com.anchor.app.core.common.FakeTimeProvider
import com.anchor.app.core.domain.model.InterventionChannel
import com.anchor.app.core.domain.model.InterventionLog
import com.anchor.app.core.domain.model.InterventionStatus
import com.anchor.app.testutil.FakeInterventionRepository
import com.anchor.app.testutil.FakeTaskRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.ZoneOffset

class RecordInterventionResponseUseCaseTest {

    private val timeProvider = FakeTimeProvider(100_000L, ZoneOffset.UTC)
    private val idProvider = FakeIdProvider()
    private val interventionRepository = FakeInterventionRepository()
    private val taskRepository = FakeTaskRepository()

    private val engine = InterventionEngine(
        interventionRepository = interventionRepository,
        taskRepository = taskRepository,
        timeProvider = timeProvider,
        idProvider = idProvider,
    )

    private val useCase = RecordInterventionResponseUseCase(
        interventionEngine = engine,
        interventionRepository = interventionRepository,
        timeProvider = timeProvider,
    )

    @Test
    fun `invoke with OPENED resolves log to INTERACTED`() = runTest {
        val logId = "log-1"
        interventionRepository.insert(
            InterventionLog(
                id = logId,
                level = 1,
                channel = InterventionChannel.NOTIFICATION,
                contextPackage = "com.example.app",
                relatedTaskId = null,
                status = InterventionStatus.EXPIRED,
                shownAt = 90_000L,
                resolvedAt = null,
                cooldownTriggered = false,
            ),
        )

        useCase(logId, InterventionResponse.OPENED)

        val updated = interventionRepository.getById(logId)
        assertNotNull(updated)
        assertEquals(InterventionStatus.INTERACTED, updated?.status)
        assertEquals(100_000L, updated?.resolvedAt)
    }

    @Test
    fun `invoke with DISMISSED resolves log to DISMISSED`() = runTest {
        val logId = "log-2"
        interventionRepository.insert(
            InterventionLog(
                id = logId,
                level = 1,
                channel = InterventionChannel.NOTIFICATION,
                contextPackage = "com.example.app",
                relatedTaskId = null,
                status = InterventionStatus.EXPIRED,
                shownAt = 90_000L,
                resolvedAt = null,
                cooldownTriggered = false,
            ),
        )

        useCase(logId, InterventionResponse.DISMISSED)

        val updated = interventionRepository.getById(logId)
        assertNotNull(updated)
        assertEquals(InterventionStatus.DISMISSED, updated?.status)
        assertEquals(100_000L, updated?.resolvedAt)
    }
}
