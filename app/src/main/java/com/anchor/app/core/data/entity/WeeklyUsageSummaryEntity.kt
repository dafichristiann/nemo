package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-ISO-week aggregation — permanent (docs 03-database-schema.md §3.3).
 *
 * `baselineJson` holds the personal per-app baseline `{pkg: {avgSessionMs, stdDev, sampleDays}}`
 * used by drift detection (PRD §10.3). `trendJson` holds the 7-day series for charts.
 */
@Entity(tableName = "weekly_usage_summary")
data class WeeklyUsageSummaryEntity(
    @PrimaryKey val isoWeek: String,
    val totalScreenTimeMs: Long,
    val dailyAvgMs: Long,
    val baselineJson: String,
    val trendJson: String,
    val computedAt: Long,
)
