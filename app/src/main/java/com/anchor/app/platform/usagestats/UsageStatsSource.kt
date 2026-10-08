package com.anchor.app.platform.usagestats

/** One continuous app-usage interval reported by the platform. */
data class UsageEvent(
    val packageName: String,
    val startTime: Long,
    val endTime: Long,
) {
    val durationMs: Long get() = (endTime - startTime).coerceAtLeast(0L)
}

/** Total time a single app was in the foreground within a query window. */
data class AppTotal(
    val packageName: String,
    val totalTimeMs: Long,
)

/**
 * Thin, testable wrapper around Android's `UsageStatsManager`
 * (docs/superpowers/design/02-architecture.md §4.1, 03-database-schema.md §3.1).
 *
 * The domain and workers depend on this interface, never on the Android API directly, so
 * aggregation logic can be unit-tested with fakes (11-testing-strategy.md §5).
 *
 * Granularity caveat (PRD §10.2): `UsageStatsManager` is aggregated and sampled, and some
 * OEMs restrict it. Callers must treat empty results as "no data", never as "no usage".
 */
interface UsageStatsSource {

    /** True when the user has granted `PACKAGE_USAGE_STATS` (special access). */
    fun hasPermission(): Boolean

    /**
     * Continuous usage events between [startMillis] and [endMillis] (exclusive end),
     * ordered by start time. Empty if permission is missing or the OEM reports nothing.
     */
    fun queryEvents(startMillis: Long, endMillis: Long): List<UsageEvent>

    /**
     * Aggregated foreground time per app between [startMillis] and [endMillis].
     * Used for the daily screen-time total and top-app breakdown.
     */
    fun queryTotals(startMillis: Long, endMillis: Long): List<AppTotal>
}
