package com.anchor.app.platform.ai

import com.anchor.app.core.domain.model.InsightPayload
import com.anchor.app.core.domain.usecase.insight.AiInsightSource
import com.anchor.app.core.domain.usecase.insight.InsightDraft
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder [AiInsightSource] for the local-first build (docs 09-privacy §6).
 *
 * No network endpoint or API key is configured, so it always returns null → the generator falls
 * back to the rule-based source (FR-AI4). This keeps the app 100% offline-capable and makes the
 * "AI is optional and off by default" guarantee structural: even if `aiEnabled` were true, no raw
 * or aggregate data leaves the device until a real source is provided here.
 *
 * A future Gemini-backed source replaces this binding (docs 02 §1) — the domain contract is fixed.
 */
@Singleton
class NoOpAiInsightSource @Inject constructor() : AiInsightSource {
    override suspend fun generate(payload: InsightPayload): InsightDraft? = null
}
