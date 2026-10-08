package com.anchor.app.core.data.mapper

import com.anchor.app.core.data.entity.InsightEntity
import com.anchor.app.core.domain.model.Insight
import com.anchor.app.core.domain.model.InsightSource
import com.anchor.app.core.domain.model.InsightType
import org.json.JSONArray

/**
 * Entity ↔ domain mapping for `insight` (docs 03 §6.1). JSON array columns (`patternsJson`,
 * `suggestionsJson`) are encoded/decoded here; decoding is defensive.
 */

fun InsightEntity.toDomain(): Insight = Insight(
    id = id,
    type = type.toInsightType(),
    dateEpochDay = dateEpochDay,
    source = source.toInsightSource(),
    summary = summary,
    patterns = decodeStringList(patternsJson),
    riskLevel = riskLevel,
    suggestions = decodeStringList(suggestionsJson),
    recommendedAction = recommendedAction,
    createdAt = createdAt,
)

fun Insight.toEntity(): InsightEntity = InsightEntity(
    id = id,
    type = type.name,
    dateEpochDay = dateEpochDay,
    source = source.name,
    summary = summary,
    patternsJson = encodeStringList(patterns),
    riskLevel = riskLevel,
    suggestionsJson = encodeStringList(suggestions),
    recommendedAction = recommendedAction,
    createdAt = createdAt,
)

private fun String.toInsightType(): InsightType =
    InsightType.entries.firstOrNull { it.name == this } ?: InsightType.DAILY_REFLECTION

private fun String.toInsightSource(): InsightSource =
    InsightSource.entries.firstOrNull { it.name == this } ?: InsightSource.RULE_BASED

private fun encodeStringList(items: List<String>): String {
    val array = JSONArray()
    items.forEach { array.put(it) }
    return array.toString()
}

private fun decodeStringList(json: String): List<String> = runCatching {
    val array = JSONArray(json)
    (0 until array.length()).map { i -> array.getString(i) }
}.getOrDefault(emptyList())
