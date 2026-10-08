package com.anchor.app.core.data.mapper

import com.anchor.app.core.data.entity.FocusSessionEntity
import com.anchor.app.core.domain.model.FocusMode
import com.anchor.app.core.domain.model.FocusSession
import com.anchor.app.core.domain.model.FocusStatus
import org.json.JSONArray

/**
 * Entity ↔ domain mapping for `focus_session` (docs 03 §4.1). Enums are stored as `String`;
 * decoding falls back to safe defaults so a malformed row never crashes the app.
 */

fun FocusSessionEntity.toDomain(): FocusSession = FocusSession(
    id = id,
    taskId = taskId,
    mode = mode.toFocusMode(),
    plannedDurationMin = plannedDurationMin,
    actualDurationMs = actualDurationMs,
    startedAt = startedAt,
    endedAt = endedAt,
    status = status.toFocusStatus(),
    blockedApps = decodeBlockedApps(blockedAppsJson),
    fallbackReason = fallbackReason,
)

fun FocusSession.toEntity(): FocusSessionEntity = FocusSessionEntity(
    id = id,
    taskId = taskId,
    mode = mode.name,
    plannedDurationMin = plannedDurationMin,
    actualDurationMs = actualDurationMs,
    startedAt = startedAt,
    endedAt = endedAt,
    status = status.name,
    blockedAppsJson = encodeBlockedApps(blockedApps),
    fallbackReason = fallbackReason,
)

private fun String.toFocusMode(): FocusMode =
    FocusMode.entries.firstOrNull { it.name == this } ?: FocusMode.GENTLE

private fun String.toFocusStatus(): FocusStatus =
    FocusStatus.entries.firstOrNull { it.name == this } ?: FocusStatus.ABORTED

private fun encodeBlockedApps(apps: List<String>): String? {
    if (apps.isEmpty()) return null
    val array = JSONArray()
    apps.forEach { array.put(it) }
    return array.toString()
}

private fun decodeBlockedApps(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        (0 until array.length()).map { i -> array.getString(i) }
    }.getOrDefault(emptyList())
}
