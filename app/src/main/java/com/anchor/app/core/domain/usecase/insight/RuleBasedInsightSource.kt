package com.anchor.app.core.domain.usecase.insight

import com.anchor.app.core.domain.model.InsightPayload
import javax.inject.Inject

/**
 * Deterministic, local, always-available insight generator (FR-AI4, mandatory fallback).
 *
 * It states only what the aggregates support (FR-AI6) and never invents patterns when data is
 * thin — in that case it says so plainly (PRD §10.8 Edge Cases). Tone is calm, supportive and
 * non-judgmental (PRD §4, §10.9): observations and one realistic next step, never blame.
 *
 * Text is produced in English (the default locale, PRD §12). It is deliberately constructed from
 * structured facts so a `values-id` translation can localise these templates later.
 */
class RuleBasedInsightSource @Inject constructor() : InsightSource {

    override suspend fun generate(payload: InsightPayload): InsightDraft {
        val patterns = mutableListOf<String>()
        val suggestions = mutableListOf<String>()

        // --- Tasks (always available) ---
        val taskSummary = when {
            payload.tasksPlanned == 0 -> "No tasks were planned today."
            payload.tasksCompleted == 0 -> "You planned ${payload.tasksPlanned} task(s) but haven't completed any yet — that's okay."
            payload.tasksCompleted >= payload.tasksPlanned ->
                "You completed all ${payload.tasksPlanned} of today's tasks."
            else -> "You completed ${payload.tasksCompleted} of ${payload.tasksPlanned} tasks today."
        }
        if (payload.tasksPlanned > 0 && payload.tasksCompleted < payload.tasksPlanned) {
            suggestions += "Tomorrow, start your hardest task within your first productive window."
        }

        // --- Focus time ---
        if (payload.focusTimeMs > 0) {
            patterns += "You logged ${formatMinutes(payload.focusTimeMs)} of focused time today."
        }

        // --- Screen time (only when we actually have usage data; never a misleading zero) ---
        val hasUsage = payload.screenTimeMs != null
        if (hasUsage) {
            val distraction = payload.longestDistractionMs
            if (distraction != null && distraction >= NOTABLE_DISTRACTION_MS) {
                val label = payload.topDistractionCategory ?: "a distraction app"
                patterns += "Your longest distraction session was ${formatMinutes(distraction)} on $label."
                suggestions += "Consider a short focus session before opening $label next time."
            }
            if (payload.screenTimeMs!! > payload.focusTimeMs * SCREEN_TO_FOCUS_ALERT) {
                patterns += "Screen time was much higher than focus time today."
            }
        }

        if (payload.productiveHours.isNotEmpty()) {
            val hours = payload.productiveHours.sorted().joinToString(", ") { "%02d:00".format(it) }
            patterns += "You were most productive around $hours."
        }

        // --- Risk level: low by default; never alarmist (PRD: awareness, not fear) ---
        val riskLevel = if (hasUsage && (payload.longestDistractionMs ?: 0L) >= HIGH_DISTRACTION_MS) {
            "medium"
        } else {
            "low"
        }

        // --- Recommended action: offer focus only when there is something to focus on. ---
        val recommendedAction = if (payload.tasksCompleted < payload.tasksPlanned) "start_focus" else null
        if (payload.tasksCompleted < payload.tasksPlanned) {
            suggestions += "A 25-minute session is a gentle way to make progress."
        }

        return InsightDraft(
            summary = taskSummary,
            patterns = patterns,
            riskLevel = riskLevel,
            suggestions = suggestions.distinct().take(MAX_SUGGESTIONS),
            recommendedAction = recommendedAction,
        )
    }

    private fun formatMinutes(ms: Long): String {
        val total = ms / 60_000L
        val h = total / 60
        val m = total % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    private companion object {
        const val NOTABLE_DISTRACTION_MS = 30 * 60_000L
        const val HIGH_DISTRACTION_MS = 45 * 60_000L
        const val SCREEN_TO_FOCUS_ALERT = 3
        const val MAX_SUGGESTIONS = 3
    }
}
