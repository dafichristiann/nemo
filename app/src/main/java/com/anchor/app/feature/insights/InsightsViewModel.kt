package com.anchor.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.app.core.common.DispatcherProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.Insight
import com.anchor.app.core.domain.model.InsightSource
import com.anchor.app.core.domain.usecase.insight.BuildInsightPayloadUseCase
import com.anchor.app.core.domain.usecase.insight.GenerateDailyReflectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** A "Screen time / distraction" block is shown only when real usage data exists (PRD §10.9). */
@androidx.compose.runtime.Immutable
data class InsightsUiState(
    val dateLabel: LocalDate? = null,
    val hasAnyContent: Boolean = false,
    val tasksCompleted: Int = 0,
    val tasksPlanned: Int = 0,
    val focusTimeMs: Long = 0L,
    /** Null means "no usage data" → hide the screen-time block (never show a misleading 0). */
    val screenTimeMs: Long? = null,
    val longestDistractionMs: Long? = null,
    val longestDistractionLabel: String? = null,
    val insight: Insight? = null,
    val loading: Boolean = true,
)

/**
 * Insights ViewModel (screen 08, PRD §10.8-10.9).
 *
 * Generates (or reuses the cached) daily reflection and assembles the calm summary. It reads the
 * same aggregate payload the generator uses, so the UI never shows a number the insight couldn't
 * support. Missing usage data is surfaced as null → the section is hidden (AC §10.9).
 */
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val generateDailyReflection: GenerateDailyReflectionUseCase,
    private val buildPayload: BuildInsightPayloadUseCase,
    private val timeProvider: TimeProvider,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        load(force = false)
    }

    /** Regenerates the reflection on demand (FR-AI3 "on-demand"). */
    fun refresh() = load(force = true)

    private fun load(force: Boolean) = viewModelScope.launch(dispatchers.io) {
        val today = timeProvider.todayEpochDay()
        val insight = generateDailyReflection(today, force = force)
        val payload = buildPayload(today)

        // Honesty: only claim there is content when something actually happened.
        val hasAnyContent = payload.tasksPlanned > 0 || payload.tasksCompleted > 0 ||
            payload.focusTimeMs > 0 || payload.screenTimeMs != null

        _uiState.value = InsightsUiState(
            dateLabel = LocalDate.ofEpochDay(today),
            hasAnyContent = hasAnyContent,
            tasksCompleted = payload.tasksCompleted,
            tasksPlanned = payload.tasksPlanned,
            focusTimeMs = payload.focusTimeMs,
            screenTimeMs = payload.screenTimeMs,
            longestDistractionMs = payload.longestDistractionMs,
            longestDistractionLabel = payload.topDistractionCategory,
            insight = insight,
            loading = false,
        )
    }

    /** Whether the shown insight came from AI — exposed for honest attribution (FR-AI5). */
    fun insightSourceIsAi(insight: Insight): Boolean = insight.source == InsightSource.AI
}
