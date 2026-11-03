package com.anchor.app.core.data.mapper

import com.anchor.app.core.data.entity.AppSettingsEntity
import com.anchor.app.core.domain.model.AppSettings
import com.anchor.app.core.domain.model.FocusArea
import org.json.JSONArray

/**
 * Entity ↔ domain mapping for `app_settings` (docs 03 §7.1).
 */
fun AppSettingsEntity.toDomain(): AppSettings = AppSettings(
    trackingEnabled = trackingEnabled,
    aiEnabled = aiEnabled,
    notificationsEnabled = notificationsEnabled,
    usageRetentionDays = usageRetentionDays,
    onboardingCompleted = onboardingCompleted,
    locale = locale,
    blockedApps = decodeBlockedAppsSet(blockedAppsJson),
    focusBlockingEnabled = focusBlockingEnabled,
    nickname = nickname.orEmpty(),
    focusArea = decodeFocusArea(focusArea),
    defaultFocusDurationMin = defaultFocusDurationMin,
    doomscrollAlertEnabled = doomscrollAlertEnabled,
    doomscrollThresholdMinutes = doomscrollThresholdMinutes,
    manualWakeTimeEpochDay = manualWakeTimeEpochDay,
    manualWakeTimeMinutes = manualWakeTimeMinutes,
)

fun AppSettings.toEntity(): AppSettingsEntity = AppSettingsEntity(
    trackingEnabled = trackingEnabled,
    aiEnabled = aiEnabled,
    notificationsEnabled = notificationsEnabled,
    usageRetentionDays = usageRetentionDays,
    onboardingCompleted = onboardingCompleted,
    locale = locale,
    blockedAppsJson = encodeBlockedAppsSet(blockedApps),
    focusBlockingEnabled = focusBlockingEnabled,
    nickname = nickname.ifBlank { null },
    focusArea = focusArea.name,
    defaultFocusDurationMin = defaultFocusDurationMin,
    doomscrollAlertEnabled = doomscrollAlertEnabled,
    doomscrollThresholdMinutes = doomscrollThresholdMinutes,
    manualWakeTimeEpochDay = manualWakeTimeEpochDay,
    manualWakeTimeMinutes = manualWakeTimeMinutes,
)

/** Parses the stored [FocusArea] name, falling back to the default for unknown values. */
private fun decodeFocusArea(raw: String): FocusArea =
    FocusArea.entries.firstOrNull { it.name == raw } ?: FocusArea.COLLEGE

private fun encodeBlockedAppsSet(apps: Set<String>): String? {
    if (apps.isEmpty()) return null
    val array = JSONArray()
    apps.forEach { array.put(it) }
    return array.toString()
}

private fun decodeBlockedAppsSet(json: String?): Set<String> {
    if (json.isNullOrBlank()) return emptySet()
    return runCatching {
        val array = JSONArray(json)
        (0 until array.length()).map { array.getString(it) }.toSet()
    }.getOrDefault(emptySet())
}

