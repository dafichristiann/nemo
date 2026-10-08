package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Audit row for one intervention attempt (docs/superpowers/design/03-database-schema.md §5.1,
 * PRD FR-I5). It is the source of truth for both the daily quota and the "Later" cooldown.
 *
 * Quota counting (docs 03 §5, PRD §10.4):
 *  - `INTERACTED` and `DISMISSED` consume quota;
 *  - `EXPIRED` (user never reacted) does **not** consume quota.
 *
 * Cooldown: the last row for a given `contextPackage` whose action was "Later"
 * (`resolvedAt` within the window) blocks re-intervention for that context.
 *
 * Enums are stored as `String` (docs 03 §0 point 6) so adding values needs no migration.
 */
@Entity(
    tableName = "intervention_log",
    indices = [
        Index("shownAt"),
        Index("contextPackage"),
    ],
)
data class InterventionLogEntity(
    @PrimaryKey val id: String,
    /** 1–4 (Gentle / Contextual / Future View / Focus). */
    val level: Int,
    val channel: String,
    val contextPackage: String?,
    val relatedTaskId: String?,
    val status: String,
    val shownAt: Long,
    val resolvedAt: Long?,
    /** True when the user chose "Later" — drives the 2-hour per-context cooldown. */
    val cooldownTriggered: Boolean = false,
)
