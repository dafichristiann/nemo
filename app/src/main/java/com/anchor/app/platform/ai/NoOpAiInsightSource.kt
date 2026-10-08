package com.anchor.app.platform.ai

import com.anchor.app.core.domain.model.InsightPayload
import com.anchor.app.core.domain.usecase.insight.AiInsightSource
import com.anchor.app.core.domain.usecase.insight.InsightDraft
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A no-op [AiInsightSource] that always returns null → the generator falls back to rule-based
 * (FR-AI4). Retained primarily as a test double and as the "no key" reference behaviour; the
 * shipped binding is now [GeminiInsightSource] (PRD v0.3 §10.8), which behaves identically
 * whenever no API key is configured.
 */
@Singleton
class NoOpAiInsightSource @Inject constructor() : AiInsightSource {
    override suspend fun generate(payload: InsightPayload): InsightDraft? = null
}
