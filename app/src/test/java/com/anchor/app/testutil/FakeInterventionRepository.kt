package com.anchor.app.testutil

import com.anchor.app.core.domain.model.InterventionLog
import com.anchor.app.core.domain.model.InterventionStatus
import com.anchor.app.core.domain.repository.InterventionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [InterventionRepository] fake (docs 11-testing-strategy.md §5). Mirrors the Room
 * query semantics that back the daily quota and per-context cooldown (docs 04 §3.1): `EXPIRED`
 * rows never count toward quota, and the cooldown uses the newest "Later" resolution.
 */
class FakeInterventionRepository : InterventionRepository {
    val logs = MutableStateFlow<List<InterventionLog>>(emptyList())

    override suspend fun insert(log: InterventionLog) {
        logs.value = logs.value.filterNot { it.id == log.id } + log
    }

    override suspend fun getById(id: String): InterventionLog? = logs.value.find { it.id == id }

    override fun observeAll(): Flow<List<InterventionLog>> =
        logs.map { list -> list.sortedByDescending { it.shownAt } }

    override suspend fun quotaUsedBetween(startOfDay: Long, endOfDay: Long): Int =
        logs.value.count {
            (it.status == InterventionStatus.INTERACTED || it.status == InterventionStatus.DISMISSED) &&
                it.shownAt >= startOfDay && it.shownAt < endOfDay
        }

    override suspend fun latestCooldownAt(contextPackage: String): Long? =
        logs.value
            .filter { it.contextPackage == contextPackage && it.cooldownTriggered }
            .mapNotNull { it.resolvedAt }
            .maxOrNull()

    override suspend fun countMeaningfulAtOrAbove(level: Int): Int =
        logs.value.count {
            (it.status == InterventionStatus.INTERACTED || it.status == InterventionStatus.DISMISSED) &&
                it.level >= level
        }

    override suspend fun latestMeaningfulShownAt(): Long? =
        logs.value
            .filter { it.status == InterventionStatus.INTERACTED || it.status == InterventionStatus.DISMISSED }
            .maxOfOrNull { it.shownAt }

    override suspend fun resolve(
        id: String,
        status: InterventionStatus,
        resolvedAt: Long,
        cooldownTriggered: Boolean,
    ) {
        logs.value = logs.value.map {
            if (it.id == id) {
                it.copy(status = status, resolvedAt = resolvedAt, cooldownTriggered = cooldownTriggered)
            } else {
                it
            }
        }
    }

    override suspend fun clear() {
        logs.value = emptyList()
    }
}
