package com.anchor.app.core.common

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Formats epoch-millis timestamps into short, calm, human-readable strings for the UI.
 *
 * Deterministic: takes a [ZoneId] (from [TimeProvider]) rather than reading the system
 * zone directly. Formatting style is intentionally minimal ("Today 23:59",
 * "Wed, Oct 7"), matching the quiet tone of the design system (06-design-system.md §1).
 */
object DateTimeFormatter {

    /** e.g. "Today 23:59", "Tomorrow 09:00", "Wed, Oct 7 23:59". */
    fun deadlineLabel(millis: Long, now: Long, zone: ZoneId): String {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)
        val today = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone).toLocalDate()
        val time = "%02d:%02d".format(dt.hour, dt.minute)
        return when (dt.toLocalDate()) {
            today -> "Today $time"
            today.plusDays(1) -> "Tomorrow $time"
            today.minusDays(1) -> "Yesterday $time"
            else -> "${dt.dayOfWeek.short()} ${dt.month.short()} ${dt.dayOfMonth}, $time"
        }
    }

    /** e.g. "Completed 10:32". */
    fun completedTimeLabel(millis: Long, zone: ZoneId): String {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)
        return "Completed %02d:%02d".format(dt.hour, dt.minute)
    }

    /** e.g. "Wed, Oct 7". */
    fun dateLabel(millis: Long, zone: ZoneId): String {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)
        return "${dt.dayOfWeek.short()} ${dt.month.short()} ${dt.dayOfMonth}"
    }

    /** e.g. "2h est", "45 min est", "1h 30m est". */
    fun durationLabel(minutes: Int?): String? {
        if (minutes == null || minutes <= 0) return null
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m est"
            hours > 0 -> "${hours}h est"
            else -> "${mins}m est"
        }
    }

    private fun java.time.DayOfWeek.short(): String = name.take(3).replaceFirstChar { it.uppercase() }
    private fun java.time.Month.short(): String = name.take(3).replaceFirstChar { it.uppercase() }
}
