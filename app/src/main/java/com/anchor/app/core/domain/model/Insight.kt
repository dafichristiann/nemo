package com.anchor.app.core.domain.model

/** Kind of insight (docs/superpowers/design/03-database-schema.md §6.1). */
enum class InsightType { DAILY_REFLECTION, PATTERN, SUGGESTION }

/** Where an insight came from — attributed honestly in the UI (PRD §10.8, FR-AI5). */
enum class InsightSource { AI, RULE_BASED }

/**
 * A structured insight (docs 03 §6.1, PRD §10.8). The shape mirrors the AI schema exactly so the
 * same model stores rule-based and AI results.
 *
 * Hard rules honoured upstream (not here): only aggregated data feeds generation, the AI is
 * optional, and every field must be supported by data (FR-AI1, FR-AI4, FR-AI6).
 *
 * `@Immutable`: the list fields are read-only lists that are never mutated after construction.
 */
@androidx.compose.runtime.Immutable
data class Insight(
    val id: String,
    val type: InsightType,
    val dateEpochDay: Long,
    val source: InsightSource,
    val summary: String,
    val patterns: List<String>,
    val riskLevel: String?,
    val suggestions: List<String>,
    val recommendedAction: String?,
    val createdAt: Long,
)
