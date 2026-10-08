package com.anchor.app.core.data.repository

import com.anchor.app.core.data.dao.InsightDao
import com.anchor.app.core.data.mapper.toDomain
import com.anchor.app.core.data.mapper.toEntity
import com.anchor.app.core.domain.model.Insight
import com.anchor.app.core.domain.repository.InsightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Room-backed [InsightRepository] (docs 02-architecture.md §3.1). */
class InsightRepositoryImpl @Inject constructor(
    private val dao: InsightDao,
) : InsightRepository {

    override suspend fun insert(insight: Insight) = dao.insert(insight.toEntity())

    override suspend fun getById(id: String): Insight? = dao.getById(id)?.toDomain()

    override suspend fun dailyReflection(dateEpochDay: Long): Insight? =
        dao.dailyReflection(dateEpochDay)?.toDomain()

    override fun observeAll(): Flow<List<Insight>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun forDay(dateEpochDay: Long): List<Insight> =
        dao.forDay(dateEpochDay).map { it.toDomain() }

    override suspend fun deleteDailyReflection(dateEpochDay: Long) =
        dao.deleteDailyReflection(dateEpochDay)

    override suspend fun clear() = dao.clear()
}
