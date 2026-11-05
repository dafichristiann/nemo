package com.anchor.app.core.domain.usecase.intervention

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.InterventionAction
import com.anchor.app.core.domain.repository.InterventionRepository
import javax.inject.Inject

/**
 * Type representing user response to an intervention notification.
 */
enum class InterventionResponse {
    OPENED,
    DISMISSED,
}

/**
 * Records that an intervention has been responded to (e.g. notification body tap).
 */
class RecordInterventionResponseUseCase @Inject constructor(
    private val interventionEngine: InterventionEngine,
    private val interventionRepository: InterventionRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(
        logId: String,
        response: InterventionResponse = InterventionResponse.OPENED,
    ) {
        when (response) {
            InterventionResponse.OPENED -> {
                interventionEngine.recordAction(
                    logId = logId,
                    action = InterventionAction.START_FOCUS,
                    now = timeProvider.nowMillis(),
                )
            }
            InterventionResponse.DISMISSED -> {
                interventionEngine.recordAction(
                    logId = logId,
                    action = InterventionAction.DISMISS,
                    now = timeProvider.nowMillis(),
                )
            }
        }
    }
}
