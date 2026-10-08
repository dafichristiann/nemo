package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-day aggregation of usage — permanent (docs 03-database-schema.md §3.2).
 *
 * `dateEpochDay` is the local epoch-day key (see `TimeProvider.epochDayOf`). Rich breakdowns
 * are stored as JSON strings to keep the schema forward-compatible without migrations.
 */
@Entity(tableName = "daily_usage_summary")
data class DailyUsageSummaryEntity(
    @PrimaryKey val dateEpochDay: Long,
    val totalScreenTimeMs: Long,
    val distractionRatio: Float? = null,
    val topAppsJson: String,
    val categoryBreakdownJson: String,
    val sessionCount: Int,
    val computedAt: Long,
)
