package com.anchor.app.core.domain.usecase.insight

import com.anchor.app.core.domain.model.InsightPayload

/**
 * Optional AI insight source (FR-AI2, PRD §10.8). Only ever called when the user has opted in and
 * the device is online; returns null when unavailable so the caller falls back to rule-based.
 *
 * Implementations must:
 *  - send **only** [InsightPayload] (aggregate), never raw usage (docs 09-privacy §6);
 *  - validate the response against the schema and return null on mismatch (FR-AI4, §7-07);
 *  - never throw to the caller (offline/timeout/quota → null → fallback).
 */
interface AiInsightSource {
    /**
     * Attempts an AI insight for [payload]. Returns null when AI is disabled, offline, quota is
     * exhausted, or the response fails schema validation.
     */
    suspend fun generate(payload: InsightPayload): InsightDraft?
}
