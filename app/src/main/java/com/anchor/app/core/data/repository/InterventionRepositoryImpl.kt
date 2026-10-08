package com.anchor.app.core.data.repository

import com.anchor.app.core.data.dao.InterventionLogDao
import com.anchor.app.core.data.mapper.toDomain
import com.anchor.app.core.data.mapper.toEntity
import com.anchor.app.core.domain.model.InterventionLog
import com.anchor.app.core.domain.model.InterventionStatus
import com.anchor.app.core.domain.repository.InterventionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed [InterventionRepository] (docs 02-architecture.md §3.1). Pure mapping; quota and
 * cooldown *policy* live in the [com.anchor.app.core.domain.usecase.intervention.InterventionEngine]
 * so it stays testable with fake repositories.
 */
class InterventionRepositoryImpl @Inject constructor(
    private val dao: InterventionLogDao,
) : InterventionRepository {

    override suspend fun insert(log: InterventionLog) = dao.insert(log.toEntity())

    override suspend fun getById(id: String): InterventionLog? = dao.getById(id)?.toDomain()

    override fun observeAll(): Flow<List<InterventionLog>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun quotaUsedBetween(startOfDay: Long, endOfDay: Long): Int =
        dao.quotaUsedBetween(startOfDay, endOfDay)

    override suspend fun latestCooldownAt(contextPackage: String): Long? =
        dao.latestCooldownAt(contextPackage)

    override suspend fun countMeaningfulAtOrAbove(level: Int): Int =
        dao.countMeaningfulAtOrAbove(level)

    override suspend fun latestMeaningfulShownAt(): Long? = dao.latestMeaningfulShownAt()

    override suspend fun resolve(
        id: String,
        status: InterventionStatus,
        resolvedAt: Long,
        cooldownTriggered: Boolean,
    ) = dao.resolve(id, status.name, resolvedAt, cooldownTriggered)

    override suspend fun clear() = dao.clear()
}
