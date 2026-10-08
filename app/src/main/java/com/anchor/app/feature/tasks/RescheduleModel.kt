package com.anchor.app.feature.tasks

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure day-offset options offered by [RescheduleSheet]. Kept separate from the composable
 * so the deadline math ([resolveDeadlineMillis]) is unit-testable without a UI runtime.
 */
enum class RescheduleDay(val offsetDays: Long) {
    TODAY(0),
    TOMORROW(1),
    PLUS_3(3),
    PLUS_7(7),
}

/** Popular deadline hours shown as quick chips. */
val RESCHEDULE_HOURS: List<Int> = listOf(9, 12, 18, 23)

/**
 * Resolves a (day, hour) choice into an epoch-millis deadline, or null when there is no
 * deadline. Deterministic: all inputs are explicit — no system clock or default zone reads.
 */
fun resolveDeadlineMillis(day: RescheduleDay, hour: Int, today: LocalDate, zone: ZoneId): Long =
    today.plusDays(day.offsetDays)
        .atTime(LocalTime.of(hour, 0))
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
