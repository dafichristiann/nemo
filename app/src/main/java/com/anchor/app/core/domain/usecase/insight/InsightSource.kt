package com.anchor.app.core.domain.usecase.insight

import com.anchor.app.core.domain.model.InsightPayload

/**
 * A candidate insight produced by an [InsightSource], before it is persisted. Mirrors the AI
 * response schema (PRD §10.8) so rule-based and AI results are interchangeable.
 */
data class InsightDraft(
    val summary: String,
    val patterns: List<String>,
    val riskLevel: String?,
    val suggestions: List<String>,
    val recommendedAction: String?,
)

/**
 * Produces an [InsightDraft] from an aggregated [InsightPayload] (docs 04? → PRD §10.8).
 *
 * Implementations:
 *  - [RuleBasedInsightSource] — always available, local, deterministic (mandatory fallback, FR-AI4);
 *  - `AiInsightSource` — optional, opt-in, network (Phase 5, feature-gated).
 *
 * A source must only state what the payload supports (FR-AI6) and must never fabricate patterns
 * when data is thin (PRD §10.8 Edge Cases).
 */
interface InsightSource {
    suspend fun generate(payload: InsightPayload): InsightDraft
}
