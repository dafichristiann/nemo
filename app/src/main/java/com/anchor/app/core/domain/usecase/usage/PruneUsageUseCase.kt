package com.anchor.app.core.domain.usecase.usage

import com.anchor.app.core.common.TimeProvider
import com.anchor.app.core.domain.repository.UsageRepository
import javax.inject.Inject

/**
 * Prunes raw `usage_session` rows older than the retention window (FR-W4, PRD §10.2).
 *
 * Retention is clamped to the locked 14–30 day range (docs 03 §10): a misconfigured value
 * can never delete more history than allowed, nor keep raw data longer than intended.
 */
class PruneUsageUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val timeProvider: TimeProvider,
) {
    /** Returns the number of raw rows deleted. */
    suspend operator fun invoke(retentionDays: Int = DEFAULT_RETENTION_DAYS): Int {
        val days = retentionDays.coerceIn(MIN_RETENTION_DAYS, MAX_RETENTION_DAYS)
        // Cut on the local day boundary (start of the day `days` ago), not `now - days*24h`.
        // The naive multiply drifts across DST and leaves a partial extra day depending on when
        // the worker runs; a fixed local-midnight cutoff keeps retention day-exact.
        val cutoffEpochDay = timeProvider.todayEpochDay() - days
        val cutoff = timeProvider.startOfDayMillisForEpochDay(cutoffEpochDay)
        return usageRepository.pruneSessionsBefore(cutoff)
    }

    companion object {
        const val MIN_RETENTION_DAYS = 14
        const val MAX_RETENTION_DAYS = 30
        const val DEFAULT_RETENTION_DAYS = 30
    }
}
