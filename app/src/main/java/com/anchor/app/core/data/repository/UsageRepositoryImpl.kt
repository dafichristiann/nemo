package com.anchor.app.core.data.repository

import com.anchor.app.core.data.dao.AppCategoryDao
import com.anchor.app.core.data.dao.DailyUsageSummaryDao
import com.anchor.app.core.data.dao.UsageSessionDao
import com.anchor.app.core.data.dao.WeeklyUsageSummaryDao
import com.anchor.app.core.data.mapper.toDomain
import com.anchor.app.core.data.mapper.toEntity
import com.anchor.app.core.domain.model.AppCategory
import com.anchor.app.core.domain.model.DailyUsageSummary
import com.anchor.app.core.domain.model.UsageSession
import com.anchor.app.core.domain.model.WeeklyUsageSummary
import com.anchor.app.core.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed [UsageRepository] (docs 02-architecture.md §3.1). Pure mapping; all policy
 * (retention window, aggregation windows) lives in use cases / workers so it stays testable.
 */
class UsageRepositoryImpl @Inject constructor(
    private val sessionDao: UsageSessionDao,
    private val dailyDao: DailyUsageSummaryDao,
    private val weeklyDao: WeeklyUsageSummaryDao,
    private val categoryDao: AppCategoryDao,
) : UsageRepository {

    override suspend fun insertSessions(sessions: List<UsageSession>) =
        sessionDao.insertAll(sessions.map { it.toEntity() })

    override suspend fun sessionsBetween(from: Long, to: Long): List<UsageSession> =
        sessionDao.getBetween(from, to).map { it.toDomain() }

    override fun observeSessions(): Flow<List<UsageSession>> =
        sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun earliestSessionStart(): Long? = sessionDao.earliestStartTime()

    override suspend fun latestSessionStart(): Long? = sessionDao.latestStartTime()

    override suspend fun sessionCount(): Int = sessionDao.count()

    override suspend fun pruneSessionsBefore(cutoff: Long): Int = sessionDao.deleteOlderThan(cutoff)

    override suspend fun upsertDailySummary(summary: DailyUsageSummary) =
        dailyDao.upsert(summary.toEntity())

    override suspend fun dailySummary(epochDay: Long): DailyUsageSummary? =
        dailyDao.getByDay(epochDay)?.toDomain()

    override suspend fun dailySummariesFrom(fromEpochDay: Long): List<DailyUsageSummary> =
        dailyDao.getFrom(fromEpochDay).map { it.toDomain() }

    override fun observeDailySummaries(): Flow<List<DailyUsageSummary>> =
        dailyDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun upsertWeeklySummary(summary: WeeklyUsageSummary) =
        weeklyDao.upsert(summary.toEntity())

    override suspend fun weeklySummary(isoWeek: String): WeeklyUsageSummary? =
        weeklyDao.getByWeek(isoWeek)?.toDomain()

    override suspend fun latestWeeklySummary(): WeeklyUsageSummary? = weeklyDao.latest()?.toDomain()

    override fun observeWeeklySummaries(): Flow<List<WeeklyUsageSummary>> =
        weeklyDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun upsertAppCategory(category: AppCategory) =
        categoryDao.upsert(category.toEntity())

    override suspend fun upsertAppCategories(categories: List<AppCategory>) =
        categoryDao.upsertAll(categories.map { it.toEntity() })

    override suspend fun appCategories(): List<AppCategory> =
        categoryDao.getAll().map { it.toDomain() }

    override fun observeAppCategories(): Flow<List<AppCategory>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun clearAll() {
        sessionDao.clear()
        dailyDao.clear()
        weeklyDao.clear()
        categoryDao.clear()
    }
}
