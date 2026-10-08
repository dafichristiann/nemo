package com.anchor.app.core.domain.model

/** A raw app-usage session (docs/superpowers/design/03-database-schema.md §3.1). */
data class UsageSession(
    val id: String,
    val packageName: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val collectedAt: Long,
)

/** Compact per-app usage entry stored inside `topAppsJson` (docs 03 §3.2). */
data class AppUsage(
    val packageName: String,
    val totalTimeMs: Long,
    val sessionCount: Int,
)

/** A permanent per-day aggregate (docs 03 §3.2). */
data class DailyUsageSummary(
    val dateEpochDay: Long,
    val totalScreenTimeMs: Long,
    val distractionRatio: Float?,
    val topApps: List<AppUsage>,
    val categoryBreakdown: Map<String, Long>,
    val sessionCount: Int,
    val computedAt: Long,
)

/** Per-app personal baseline used by drift detection (PRD §10.3, docs 03 §3.3). */
data class AppBaseline(
    val packageName: String,
    val avgSessionMs: Long,
    val stdDevMs: Long,
    val sampleDays: Int,
)

/** A permanent per-week aggregate (docs 03 §3.3). */
data class WeeklyUsageSummary(
    val isoWeek: String,
    val totalScreenTimeMs: Long,
    val dailyAvgMs: Long,
    val baseline: List<AppBaseline>,
    val trend: List<Long>,
    val computedAt: Long,
)

/** App category + distraction flag (docs 03 §3.4, PRD FR-S3). */
data class AppCategory(
    val packageName: String,
    val category: String,
    val isDistraction: Boolean,
    val overrideSource: String,
) {
    companion object {
        const val SOURCE_AUTO = "AUTO"
        const val SOURCE_USER = "USER"
    }
}
