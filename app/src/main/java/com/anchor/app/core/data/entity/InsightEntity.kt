package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A generated insight / daily reflection (docs/superpowers/design/03-database-schema.md §6.1,
 * PRD §10.8-10.9). `source` records whether it came from the optional AI or the mandatory
 * rule-based fallback, so the UI can attribute it honestly (FR-AI5, PRD §10.8).
 *
 * Enums are stored as `String` (docs 03 §0 point 6). JSON columns hold arrays/objects; decoding
 * is defensive so a malformed blob never crashes the app.
 */
@Entity(
    tableName = "insight",
    indices = [Index("dateEpochDay"), Index("type")],
)
data class InsightEntity(
    @PrimaryKey val id: String,
    val type: String,
    val dateEpochDay: Long,
    val source: String,
    val summary: String,
    val patternsJson: String,
    val riskLevel: String?,
    val suggestionsJson: String,
    val recommendedAction: String?,
    val createdAt: Long,
)
