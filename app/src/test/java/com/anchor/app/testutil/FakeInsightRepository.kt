package com.anchor.app.testutil

import com.anchor.app.core.domain.model.Insight
import com.anchor.app.core.domain.repository.InsightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [InsightRepository] fake (docs 11-testing-strategy.md §5). */
class FakeInsightRepository : InsightRepository {
    val insights = MutableStateFlow<List<Insight>>(emptyList())

    override suspend fun insert(insight: Insight) {
        insights.value = insights.value.filterNot { it.id == insight.id } + insight
    }

    override suspend fun getById(id: String): Insight? = insights.value.find { it.id == id }

    override suspend fun dailyReflection(dateEpochDay: Long): Insight? =
        insights.value.firstOrNull {
            it.dateEpochDay == dateEpochDay && it.type == com.anchor.app.core.domain.model.InsightType.DAILY_REFLECTION
        }

    override fun observeAll(): Flow<List<Insight>> = insights.map { list -> list.sortedByDescending { it.createdAt } }

    override suspend fun forDay(dateEpochDay: Long): List<Insight> =
        insights.value.filter { it.dateEpochDay == dateEpochDay }.sortedByDescending { it.createdAt }

    override suspend fun deleteDailyReflection(dateEpochDay: Long) {
        insights.value = insights.value.filterNot {
            it.dateEpochDay == dateEpochDay && it.type == com.anchor.app.core.domain.model.InsightType.DAILY_REFLECTION
        }
    }

    override suspend fun clear() {
        insights.value = emptyList()
    }
}

/** Scriptable [com.anchor.app.core.domain.usecase.insight.AiInsightSource] fake. */
class FakeAiInsightSource(
    var draft: com.anchor.app.core.domain.usecase.insight.InsightDraft? = null,
    var throwOnGenerate: Boolean = false,
) : com.anchor.app.core.domain.usecase.insight.AiInsightSource {
    var calls = 0
        private set
    var lastPayload: com.anchor.app.core.domain.model.InsightPayload? = null
        private set

    override suspend fun generate(
        payload: com.anchor.app.core.domain.model.InsightPayload,
    ): com.anchor.app.core.domain.usecase.insight.InsightDraft? {
        calls++
        lastPayload = payload
        if (throwOnGenerate) throw RuntimeException("network down")
        return draft
    }
}
