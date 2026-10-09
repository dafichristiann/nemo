package com.anchor.app.core.domain.usecase.insight

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.InsightPayload
import com.anchor.app.core.domain.model.TaskStatus
import com.anchor.app.core.domain.repository.FocusRepository
import com.anchor.app.core.domain.repository.TaskRepository
import com.anchor.app.core.domain.repository.UsageRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Builds the aggregated-only [InsightPayload] for a day (FR-AI1, docs 09-privacy §6).
 *
 * Critically, it reads **only** daily/weekly aggregates and focus session totals — never raw
 * `UsageSession` rows, never task titles. This is the single place the privacy boundary is
 * crossed, and the resulting type cannot carry private data (see [InsightPayload]).
 *
 * Missing usage data is represented as `null` (not zero) so downstream generation and the UI can
 * hide the section instead of showing a misleading zero (PRD §10.9 Edge Cases).
 */
class BuildInsightPayloadUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val usageRepository: UsageRepository,
    private val focusRepository: FocusRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(dateEpochDay: Long = timeProvider.todayEpochDay()): InsightPayload {
        val tasks = taskRepository.observeAll().first()
        val tasksPlanned = tasks.count { it.status != TaskStatus.ARCHIVED }
        val tasksCompleted = tasks.count { it.status == TaskStatus.DONE && it.completedAt != null }

        val startOfDay = timeProvider.startOfDayMillisForEpochDay(dateEpochDay)
        val endOfDay = timeProvider.endOfDayMillisForEpochDay(dateEpochDay)
        val focusMs = focusRepository.totalDurationBetween(startOfDay, endOfDay)

        val daily = usageRepository.dailySummary(dateEpochDay)
        val categoryByPkg = usageRepository.appCategories().associateBy { it.packageName }

        val screenTimeMs = daily?.totalScreenTimeMs?.takeIf { it > 0L }

        // Longest distraction session, derived from per-day aggregates (never raw sessions).
        val longestDistraction = daily?.topApps?.maxByOrNull { it.totalTimeMs }
            ?.takeIf { app -> categoryByPkg[app.packageName]?.isDistraction == true }
        val distractionCategory = longestDistraction?.let { app ->
            categoryByPkg[app.packageName]?.category?.let(::humanizeCategory)
        }

        return InsightPayload(
            dateEpochDay = dateEpochDay,
            tasksCompleted = tasksCompleted,
            tasksPlanned = tasksPlanned,
            focusTimeMs = focusMs,
            screenTimeMs = screenTimeMs,
            longestDistractionMs = longestDistraction?.totalTimeMs,
            topDistractionCategory = distractionCategory,
            // Productive hours are not tracked per-hour yet; keep empty (no fabrication).
            productiveHours = emptyList(),
        )
    }

    private fun humanizeCategory(category: String): String =
        category.lowercase().replaceFirstChar { it.uppercase() }
}
