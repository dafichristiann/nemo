package com.anchor.app.core.data.mapper

import com.anchor.app.core.data.entity.AppCategoryEntity
import com.anchor.app.core.data.entity.DailyUsageSummaryEntity
import com.anchor.app.core.data.entity.UsageSessionEntity
import com.anchor.app.core.data.entity.WeeklyUsageSummaryEntity
import com.anchor.app.core.domain.model.AppBaseline
import com.anchor.app.core.domain.model.AppCategory
import com.anchor.app.core.domain.model.AppUsage
import com.anchor.app.core.domain.model.DailyUsageSummary
import com.anchor.app.core.domain.model.UsageSession
import com.anchor.app.core.domain.model.WeeklyUsageSummary
import org.json.JSONArray
import org.json.JSONObject

/**
 * Entity ↔ domain mapping for the usage/wellbeing tables (docs 03 §3).
 *
 * JSON blobs (`topAppsJson`, `categoryBreakdownJson`, `baselineJson`, `trendJson`) are
 * encoded/decoded here. Decoding is defensive: a malformed blob yields an empty structure
 * instead of crashing (graceful degradation, docs 02 §6).
 */

// --- UsageSession ---

fun UsageSessionEntity.toDomain(): UsageSession = UsageSession(
    id = id,
    packageName = packageName,
    startTime = startTime,
    endTime = endTime,
    durationMs = durationMs,
    collectedAt = collectedAt,
)

fun UsageSession.toEntity(): UsageSessionEntity = UsageSessionEntity(
    id = id,
    packageName = packageName,
    startTime = startTime,
    endTime = endTime,
    durationMs = durationMs,
    collectedAt = collectedAt,
)

// --- DailyUsageSummary ---

fun DailyUsageSummaryEntity.toDomain(): DailyUsageSummary = DailyUsageSummary(
    dateEpochDay = dateEpochDay,
    totalScreenTimeMs = totalScreenTimeMs,
    distractionRatio = distractionRatio,
    topApps = decodeTopApps(topAppsJson),
    categoryBreakdown = decodeCategoryBreakdown(categoryBreakdownJson),
    sessionCount = sessionCount,
    computedAt = computedAt,
)

fun DailyUsageSummary.toEntity(): DailyUsageSummaryEntity = DailyUsageSummaryEntity(
    dateEpochDay = dateEpochDay,
    totalScreenTimeMs = totalScreenTimeMs,
    distractionRatio = distractionRatio,
    topAppsJson = encodeTopApps(topApps),
    categoryBreakdownJson = encodeCategoryBreakdown(categoryBreakdown),
    sessionCount = sessionCount,
    computedAt = computedAt,
)

// --- WeeklyUsageSummary ---

fun WeeklyUsageSummaryEntity.toDomain(): WeeklyUsageSummary = WeeklyUsageSummary(
    isoWeek = isoWeek,
    totalScreenTimeMs = totalScreenTimeMs,
    dailyAvgMs = dailyAvgMs,
    baseline = decodeBaseline(baselineJson),
    trend = decodeTrend(trendJson),
    computedAt = computedAt,
)

fun WeeklyUsageSummary.toEntity(): WeeklyUsageSummaryEntity = WeeklyUsageSummaryEntity(
    isoWeek = isoWeek,
    totalScreenTimeMs = totalScreenTimeMs,
    dailyAvgMs = dailyAvgMs,
    baselineJson = encodeBaseline(baseline),
    trendJson = encodeTrend(trend),
    computedAt = computedAt,
)

// --- AppCategory ---

fun AppCategoryEntity.toDomain(): AppCategory = AppCategory(
    packageName = packageName,
    category = category,
    isDistraction = isDistraction,
    overrideSource = overrideSource,
)

fun AppCategory.toEntity(): AppCategoryEntity = AppCategoryEntity(
    packageName = packageName,
    category = category,
    isDistraction = isDistraction,
    overrideSource = overrideSource,
)

// --- JSON codecs ---

private fun encodeTopApps(apps: List<AppUsage>): String {
    val array = JSONArray()
    apps.forEach { app ->
        array.put(
            JSONObject()
                .put("pkg", app.packageName)
                .put("ms", app.totalTimeMs)
                .put("sessions", app.sessionCount),
        )
    }
    return array.toString()
}

private fun decodeTopApps(json: String): List<AppUsage> = runCatching {
    val array = JSONArray(json)
    (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        AppUsage(
            packageName = obj.getString("pkg"),
            totalTimeMs = obj.optLong("ms", 0L),
            sessionCount = obj.optInt("sessions", 0),
        )
    }
}.getOrDefault(emptyList())

private fun encodeCategoryBreakdown(map: Map<String, Long>): String {
    val obj = JSONObject()
    map.forEach { (category, ms) -> obj.put(category, ms) }
    return obj.toString()
}

private fun decodeCategoryBreakdown(json: String): Map<String, Long> = runCatching {
    val obj = JSONObject(json)
    val result = mutableMapOf<String, Long>()
    obj.keys().asSequence().forEach { key -> result[key] = obj.optLong(key, 0L) }
    result
}.getOrDefault(emptyMap())

private fun encodeBaseline(baselines: List<AppBaseline>): String {
    val obj = JSONObject()
    baselines.forEach { b ->
        obj.put(
            b.packageName,
            JSONObject()
                .put("avgSessionMs", b.avgSessionMs)
                .put("stdDevMs", b.stdDevMs)
                .put("sampleDays", b.sampleDays),
        )
    }
    return obj.toString()
}

private fun decodeBaseline(json: String): List<AppBaseline> = runCatching {
    val obj = JSONObject(json)
    val packages = obj.keys().asSequence().toList()
    packages.map { pkg ->
        val entry = obj.getJSONObject(pkg)
        AppBaseline(
            packageName = pkg,
            avgSessionMs = entry.optLong("avgSessionMs", 0L),
            stdDevMs = entry.optLong("stdDevMs", 0L),
            sampleDays = entry.optInt("sampleDays", 0),
        )
    }
}.getOrDefault(emptyList())

private fun encodeTrend(trend: List<Long>): String {
    val array = JSONArray()
    trend.forEach { array.put(it) }
    return array.toString()
}

private fun decodeTrend(json: String): List<Long> = runCatching {
    val array = JSONArray(json)
    (0 until array.length()).map { i -> array.optLong(i, 0L) }
}.getOrDefault(emptyList())
