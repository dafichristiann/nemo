package com.anchor.app.core.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.anchor.app.core.data.entity.DailyUsageSummaryEntity
import kotlinx.coroutines.flow.Flow

/** DAO for permanent per-day aggregates (docs 03-database-schema.md §3.2). */
@Dao
interface DailyUsageSummaryDao {

    @Upsert
    suspend fun upsert(summary: DailyUsageSummaryEntity)

    @Query("SELECT * FROM daily_usage_summary WHERE dateEpochDay = :epochDay")
    suspend fun getByDay(epochDay: Long): DailyUsageSummaryEntity?

    @Query("SELECT * FROM daily_usage_summary WHERE dateEpochDay >= :fromEpochDay ORDER BY dateEpochDay")
    suspend fun getFrom(fromEpochDay: Long): List<DailyUsageSummaryEntity>

    @Query("SELECT * FROM daily_usage_summary ORDER BY dateEpochDay DESC")
    fun observeAll(): Flow<List<DailyUsageSummaryEntity>>

    @Query("DELETE FROM daily_usage_summary")
    suspend fun clear()
}
