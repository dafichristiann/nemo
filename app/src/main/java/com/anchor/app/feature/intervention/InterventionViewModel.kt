package com.anchor.app.feature.intervention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.InterventionAction
import com.anchor.app.core.domain.usecase.intervention.InterventionEngine
import com.anchor.app.platform.notification.InterventionDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the in-app intervention surface — screen 04 (docs/superpowers/design/07-screens.md §2.8,
 * 04-intervention-engine.md §3.5). It only exposes what the overlay needs and forwards the user's
 * choice to [InterventionEngine.recordAction], which handles quota + cooldown effects.
 */
@HiltViewModel
class InterventionViewModel @Inject constructor(
    private val dispatcher: InterventionDispatcher,
    private val engine: InterventionEngine,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    val uiState: StateFlow<InterventionUiState> = dispatcher.activeIntervention
        .map { active ->
            if (active == null) {
                InterventionUiState.Hidden
            } else {
                InterventionUiState.Visible(
                    logId = active.logId,
                    level = active.level,
                    relatedTaskId = active.relatedTaskId,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InterventionUiState.Hidden)

    /** [Start Focus] (FR-I7): records INTERACTED and routes to the focus flow. */
    fun onStartFocus(onNavigateToFocus: (taskId: String?) -> Unit) {
        val current = (uiState.value as? InterventionUiState.Visible) ?: return
        viewModelScope.launch {
            engine.recordAction(current.logId, InterventionAction.START_FOCUS, timeProvider.nowMillis())
            dispatcher.clearActive()
            onNavigateToFocus(current.relatedTaskId)
        }
    }

    /** [Later] (FR-I7): records DISMISSED and arms a 2-hour cooldown for this context. */
    fun onLater() {
        resolve(InterventionAction.LATER)
    }

    /** Plain dismiss: records DISMISSED, no cooldown. */
    fun onDismiss() {
        resolve(InterventionAction.DISMISS)
    }

    /** Surface auto-hide / timeout: records EXPIRED, which costs no quota (docs 04 §3.5). */
    fun onExpire() {
        resolve(InterventionAction.EXPIRE)
    }

    private fun resolve(action: InterventionAction) {
        val current = (uiState.value as? InterventionUiState.Visible) ?: return
        viewModelScope.launch {
            engine.recordAction(current.logId, action, timeProvider.nowMillis())
            dispatcher.clearActive()
        }
    }
}

/** UI state for screen 04 (kept deliberately tiny; copy lives in string resources). */
sealed interface InterventionUiState {
    data object Hidden : InterventionUiState

    data class Visible(
        val logId: String,
        val level: Int,
        val relatedTaskId: String?,
    ) : InterventionUiState
}
