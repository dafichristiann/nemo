package com.anchor.app.platform.ai

import com.anchor.app.BuildConfig
import com.anchor.app.core.domain.model.InsightPayload
import com.anchor.app.core.domain.usecase.insight.AiInsightSource
import com.anchor.app.core.domain.usecase.insight.InsightDraft
import com.anchor.app.core.domain.usecase.insight.InsightSchemaValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real (opt-in) AI insight source backed by the **Gemini REST API** (PRD v0.3 §10.8, §13.1).
 *
 * Guarantees:
 *  - **Aggregate-only egress.** The only thing sent is a text prompt built from [InsightPayload]
 *    (counts/time aggregates). No raw session, task title, timestamp or device identity exists
 *    in the payload type, so sending raw usage is impossible by construction (docs 09-privacy §2).
 *  - **Fail-soft.** Any failure (no key, offline, timeout, HTTP error, malformed JSON, schema
 *    mismatch) returns `null` → the caller falls back to rule-based (FR-AI4, FR-AI8). This method
 *    never throws.
 *  - **No hidden egress.** When [BuildConfig.GEMINI_API_KEY] is blank this short-circuits before
 *    any network call, so an unconfigured build stays 100% offline even with AI toggled on.
 *
 * Implemented with [HttpURLConnection] + `org.json` (both in the Android platform) to avoid
 * pulling a networking/JSON dependency (docs 02 §1 keeps the footprint small).
 */
@Singleton
class GeminiInsightSource @Inject constructor() : AiInsightSource {

    override suspend fun generate(payload: InsightPayload): InsightDraft? {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) return null // unconfigured → rule-based, no network attempt
        if (!InsightPayload.passesPrivacyAudit(payload)) return null

        return withContext(Dispatchers.IO) {
            runCatching { requestDraft(key, payload) }
                .getOrNull()
                ?.takeIf { InsightSchemaValidator.isValid(it) }
        }
    }

    private fun requestDraft(key: String, payload: InsightPayload): InsightDraft? {
        val requestBody = buildRequestBody(payload)
        // Try each model in order; a retired/overloaded model must not force a fallback when the
        // next one would succeed (free-tier aliases can 404 or 503 transiently, FR-AI8).
        for (model in MODELS) {
            val draft = runCatching { callModel(key, model, requestBody) }.getOrNull()
            if (draft != null) return draft
        }
        return null
    }

    private fun callModel(key: String, model: String, requestBody: JSONObject): InsightDraft? {
        // The API key is sent in the `x-goog-api-key` header, NOT as a `?key=` URL query param:
        // URLs are more likely to be logged/persisted by proxies and crash reporters than headers.
        val url = URL("$ENDPOINT/$model:generateContent")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", key)
        }

        try {
            connection.outputStream.use { out ->
                out.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            if (connection.responseCode !in 200..299) return null // 429/403/404/5xx → try next

            val body = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            return parseDraft(body)
        } finally {
            connection.disconnect()
        }
    }

    /** Wraps the structured prompt + a strict JSON schema instruction in a Gemini request. */
    private fun buildRequestBody(payload: InsightPayload): JSONObject {
        val prompt = buildString {
            appendLine("You are Anchor, a calm, supportive, non-judgmental focus assistant.")
            appendLine("Interpret ONLY the aggregate stats below. Do not invent data.")
            appendLine("Tone: concise, encouraging, never blame. Language: English.")
            appendLine()
            appendLine("Aggregate stats for the day:")
            appendLine("- Tasks planned: ${payload.tasksPlanned}")
            appendLine("- Tasks completed: ${payload.tasksCompleted}")
            appendLine("- Focus time: ${payload.focusTimeMs} ms")
            append("- Screen time: ${payload.screenTimeMs?.let { "$it ms" } ?: "no data"}")
            appendLine()
            append("- Longest distraction: ${payload.longestDistractionMs?.let { "$it ms" } ?: "no data"}")
            appendLine()
            appendLine("- Top distraction category: ${payload.topDistractionCategory ?: "no data"}")
            appendLine()
            appendLine("Reply with ONLY minified JSON matching exactly this schema:")
            appendLine("""{"summary": string, "patterns": string[], "risk_level": "low"|"medium"|"high", "suggestions": string[], "recommended_action": "start_focus"|null}""")
            append("Do not add commentary outside the JSON.")
        }

        val generationConfig = JSONObject().apply {
            // Ask for deterministic JSON output (no creative drift).
            put("temperature", 0.4)
            put("responseMimeType", "application/json")
        }

        return JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            }))
            put("generationConfig", generationConfig)
        }
    }

    /** Extracts the model's JSON text from the Gemini response envelope and maps it to a draft. */
    private fun parseDraft(responseBody: String): InsightDraft? {
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: return null
        val text = candidates.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val json = JSONObject(extractJsonObject(text))
        return InsightDraft(
            summary = json.optString("summary").trim(),
            patterns = json.optJSONArray("patterns").toStringList(),
            riskLevel = json.optString("risk_level").takeIf { it.isNotBlank() && it != "null" },
            suggestions = json.optJSONArray("suggestions").toStringList(),
            recommendedAction = json.optString("recommended_action")
                .takeIf { it.isNotBlank() && it != "null" },
        )
    }

    /** Tolerates stray prose around the JSON object (defensive, in case responseMimeType is ignored). */
    private fun extractJsonObject(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { i ->
            optString(i).trim().takeIf { it.isNotBlank() }
        }
    }

    private companion object {
        const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"

        /**
         * Models tried in order. Prefer the stable `-latest` aliases (they track the current
         * Flash model and survive version pinning/retirement); the explicit pinned names are
         * kept as a secondary attempt. A retired or overloaded model simply moves to the next.
         */
        val MODELS = listOf(
            "gemini-flash-lite-latest",
            "gemini-flash-latest",
        )
        const val TIMEOUT_MS = 15_000
    }
}
