package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One continuous app-usage session — RAW data, pruned after the retention window
 * (docs/superpowers/design/03-database-schema.md §3.1, §10).
 *
 * Standalone by design: no foreign keys, so writes stay fast and pruning is trivial.
 * Indexed on `startTime` (pruning + range queries) and `packageName` (per-app rollups).
 */
@Entity(
    tableName = "usage_session",
    indices = [
        Index("startTime"),
        Index("packageName"),
    ],
)
data class UsageSessionEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val collectedAt: Long,
)
