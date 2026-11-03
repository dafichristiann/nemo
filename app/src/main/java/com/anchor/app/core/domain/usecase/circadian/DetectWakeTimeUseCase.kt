package com.anchor.app.core.domain.usecase.circadian

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.model.UsageSession
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.core.domain.repository.UsageRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.max

enum class WakeConfidence { HIGH, MEDIUM, LOW }

data class WakeTimeResult(
    val time: LocalTime,
    val isManual: Boolean,
    val confidence: WakeConfidence,
)

/**
 * Detects wake time using screen-off gap analysis or manual override (Spec 2026-11-03 §3.1).
 */
class DetectWakeTimeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val usageRepository: UsageRepository,
    private val timeProvider: TimeProvider,
) {
    suspend operator fun invoke(
        nowMillis: Long = timeProvider.nowMillis(),
        zoneId: ZoneId = timeProvider.zoneId(),
    ): WakeTimeResult {
        val today = LocalDate.ofInstant(Instant.ofEpochMilli(nowMillis), zoneId)
        val todayEpoch = today.toEpochDay()

        // 1. Check manual override
        val settings = settingsRepository.get()
        if (settings.manualWakeTimeEpochDay == todayEpoch && settings.manualWakeTimeMinutes != null) {
            val minutes = settings.manualWakeTimeMinutes
            val wakeTime = LocalTime.of((minutes / 60) % 24, minutes % 60)
            return WakeTimeResult(wakeTime, isManual = true, confidence = WakeConfidence.HIGH)
        }

        // 2. Normal condition: longest screen-off gap >= 4 hours in last 24h
        val sessions24h = usageRepository.sessionsBetween(nowMillis - TWENTY_FOUR_HOURS_MS, nowMillis)
        val detectedWakeMillis = findLongestGapWakeTime(sessions24h)
        if (detectedWakeMillis != null) {
            val wakeTime = Instant.ofEpochMilli(detectedWakeMillis).atZone(zoneId).toLocalTime()
            return WakeTimeResult(wakeTime, isManual = false, confidence = WakeConfidence.HIGH)
        }

        // 3. Fallback: median of past 3 days' wake times
        val pastWakeMinutes = mutableListOf<Int>()
        for (dayOffset in 1..3) {
            val windowEnd = nowMillis - dayOffset * TWENTY_FOUR_HOURS_MS
            val windowStart = windowEnd - TWENTY_FOUR_HOURS_MS
            val daySessions = usageRepository.sessionsBetween(windowStart, windowEnd)
            val wakeMillis = findLongestGapWakeTime(daySessions)
            if (wakeMillis != null) {
                val time = Instant.ofEpochMilli(wakeMillis).atZone(zoneId).toLocalTime()
                pastWakeMinutes.add(time.hour * 60 + time.minute)
            }
        }

        if (pastWakeMinutes.isNotEmpty()) {
            pastWakeMinutes.sort()
            val medianMin = pastWakeMinutes[pastWakeMinutes.size / 2]
            val wakeTime = LocalTime.of((medianMin / 60) % 24, medianMin % 60)
            return WakeTimeResult(wakeTime, isManual = false, confidence = WakeConfidence.MEDIUM)
        }

        // 4. Default 08:00
        return WakeTimeResult(DEFAULT_WAKE_TIME, isManual = false, confidence = WakeConfidence.LOW)
    }

    private fun findLongestGapWakeTime(sessions: List<UsageSession>): Long? {
        if (sessions.isEmpty()) return null
        val sorted = sessions.sortedBy { it.startTime }

        // Merge overlapping/consecutive sessions into active intervals
        val merged = mutableListOf<Pair<Long, Long>>()
        for (session in sorted) {
            if (merged.isEmpty()) {
                merged.add(session.startTime to session.endTime)
            } else {
                val last = merged.last()
                if (session.startTime <= last.second) {
                    merged[merged.size - 1] = last.first to max(last.second, session.endTime)
                } else {
                    merged.add(session.startTime to session.endTime)
                }
            }
        }

        var longestGap = 0L
        var wakeTimeMillis: Long? = null

        for (i in 0 until merged.size - 1) {
            val gap = merged[i + 1].first - merged[i].second
            if (gap >= FOUR_HOURS_MS && gap > longestGap) {
                longestGap = gap
                wakeTimeMillis = merged[i + 1].first
            }
        }

        return wakeTimeMillis
    }

    companion object {
        const val FOUR_HOURS_MS = 4 * 60 * 60 * 1000L
        const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L
        val DEFAULT_WAKE_TIME: LocalTime = LocalTime.of(8, 0)
    }
}
