package com.anchor.app.core.data.repository

import com.anchor.app.core.data.dao.FocusSessionDao
import com.anchor.app.core.data.mapper.toDomain
import com.anchor.app.core.data.mapper.toEntity
import com.anchor.app.core.domain.model.FocusSession
import com.anchor.app.core.domain.model.FocusStatus
import com.anchor.app.core.domain.repository.FocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room-backed [FocusRepository] (docs 02-architecture.md §3.1). */
class FocusRepositoryImpl @Inject constructor(
    private val dao: FocusSessionDao,
) : FocusRepository {

    override suspend fun insert(session: FocusSession) = dao.insert(session.toEntity())

    override suspend fun getById(id: String): FocusSession? = dao.getById(id)?.toDomain()

    override suspend fun current(): FocusSession? = dao.getCurrent()?.toDomain()

    override fun observeCurrent(): Flow<FocusSession?> =
        dao.observeCurrent().map { it?.toDomain() }

    override fun observeAll(): Flow<List<FocusSession>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun totalDurationBetween(from: Long, to: Long): Long =
        dao.totalDurationBetween(from, to)

    override suspend fun updateState(
        id: String,
        status: FocusStatus,
        actualDurationMs: Long,
        endedAt: Long?,
        fallbackReason: String?,
    ) = dao.updateState(id, status.name, actualDurationMs, endedAt, fallbackReason)

    override suspend fun clear() = dao.clear()
}
