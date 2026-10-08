package com.anchor.app.core.domain.usecase.insight

import com.anchor.app.core.common.IdProvider
import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.Insight
import com.anchor.app.core.domain.model.InsightSource
import com.anchor.app.core.domain.model.InsightType
import com.anchor.app.core.domain.model.InsightPayload
import com.anchor.app.core.domain.repository.InsightRepository
import com.anchor.app.core.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Generates (and caches) the Daily Reflection (FR-DR1..DR5, FR-AI2..AI5).
 *
 * Flow:
 *  1. build the aggregate-only payload ([BuildInsightPayloadUseCase]);
 *  2. if AI is **enabled** and returns a **schema-valid** draft → use it ([InsightSource.AI]);
 *  3. otherwise use the rule-based source (mandatory fallback, FR-AI4);
 *  4. persist an [Insight] with `type = DAILY_REFLECTION` and the honest source.
 *
 * Caching (FR-AI3): returns the existing row for the day unless [force] is true or none exists.
 * Never throws for AI/offline/validation failures — rule-based is always available.
 */
class GenerateDailyReflectionUseCase @Inject constructor(
    private val insightRepository: InsightRepository,
    private val settingsRepository: SettingsRepository,
    private val buildPayload: BuildInsightPayloadUseCase,
    private val ruleBasedSource: RuleBasedInsightSource,
    private val aiInsightSource: AiInsightSource,
    private val timeProvider: TimeProvider,
    private val idProvider: IdProvider,
) {

    suspend operator fun invoke(
        dateEpochDay: Long = timeProvider.todayEpochDay(),
        force: Boolean = false,
    ): Insight {
        if (!force) {
            insightRepository.dailyReflection(dateEpochDay)?.let { return it }
        }

        val payload = buildPayload(dateEpochDay)

        // Only ever attempt AI when the user opted in (privacy by default, FR-S6/AI2).
        val aiEnabled = settingsRepository.get().aiEnabled

        val aiDraft = if (aiEnabled && InsightPayload.passesPrivacyAudit(payload)) {
            runCatching { aiInsightSource.generate(payload) }.getOrNull()
                ?.takeIf { InsightSchemaValidator.isValid(it) }
        } else {
            null
        }

        val source = if (aiDraft != null) InsightSource.AI else InsightSource.RULE_BASED
        val draft = aiDraft ?: ruleBasedSource.generate(payload)

        val insight = Insight(
            id = idProvider.newId(),
            type = InsightType.DAILY_REFLECTION,
            dateEpochDay = dateEpochDay,
            source = source,
            summary = draft.summary,
            patterns = draft.patterns,
            riskLevel = draft.riskLevel,
            suggestions = draft.suggestions,
            recommendedAction = draft.recommendedAction,
            createdAt = timeProvider.nowMillis(),
        )

        // Replace any stale row for the day so the cache stays single-valued.
        insightRepository.deleteDailyReflection(dateEpochDay)
        insightRepository.insert(insight)
        return insight
    }
}
