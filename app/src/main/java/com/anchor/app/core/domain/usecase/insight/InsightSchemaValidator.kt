package com.anchor.app.core.domain.usecase.insight

/**
 * Validates an AI-produced [InsightDraft] against the required schema (FR-AI4, docs 09-privacy §6/§7;
 * PRD §10.8 output schema):
 *
 * ```json
 * { "summary": "...", "patterns": [], "risk_level": "low", "suggestions": [], "recommended_action": "start_focus" }
 * ```
 *
 * A response is rejected (→ fallback to rule-based) when:
 *  - the summary is blank;
 *  - the risk level is present but not one of `low|medium|high`;
 *  - any list is empty when it is required to be present (patterns/suggestions may be empty but
 *    must be non-null lists — represented here as an existing list).
 *
 * Pure and deterministic so it is unit-testable without any network.
 */
object InsightSchemaValidator {

    private val ALLOWED_RISK = setOf("low", "medium", "high")

    fun isValid(draft: InsightDraft?): Boolean {
        if (draft == null) return false
        if (draft.summary.isBlank()) return false
        if (draft.riskLevel != null && draft.riskLevel !in ALLOWED_RISK) return false
        // Lists are non-null by type; each entry must be a non-blank string.
        if (draft.patterns.any { it.isBlank() }) return false
        if (draft.suggestions.any { it.isBlank() }) return false
        return true
    }
}
