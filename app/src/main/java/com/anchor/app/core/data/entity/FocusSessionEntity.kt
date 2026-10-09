package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A focus session record (docs/superpowers/design/03-database-schema.md §4.1, PRD §10.5).
 * Kept permanently so Insights can reason about focus time (docs 03 §1).
 *
 * Enums are stored as `String` (docs 03 §0 point 6). `blockedAppsJson` is only meaningful for
 * Strict/Custom modes; `fallbackReason` records when blocking could not engage and the session
 * degraded to Gentle (Spike B, docs 13).
 */
@Entity(
    tableName = "focus_session",
    indices = [Index("startedAt"), Index("taskId")],
)
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val taskId: String?,
    val mode: String,
    val plannedDurationSec: Int?,
    val actualDurationMs: Long,
    val startedAt: Long,
    val endedAt: Long?,
    val status: String,
    val blockedAppsJson: String?,
    val fallbackReason: String?,
)
