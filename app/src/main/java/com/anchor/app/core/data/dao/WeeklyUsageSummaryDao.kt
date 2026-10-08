package com.anchor.app.core.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.anchor.app.core.data.entity.WeeklyUsageSummaryEntity
import kotlinx.coroutines.flow.Flow

/** DAO for permanent per-week aggregates (docs 03-database-schema.md §3.3). */
@Dao
interface WeeklyUsageSummaryDao {

    @Upsert
    suspend fun upsert(summary: WeeklyUsageSummaryEntity)

    @Query("SELECT * FROM weekly_usage_summary WHERE isoWeek = :isoWeek")
    suspend fun getByWeek(isoWeek: String): WeeklyUsageSummaryEntity?

    @Query("SELECT * FROM weekly_usage_summary ORDER BY isoWeek DESC")
    fun observeAll(): Flow<List<WeeklyUsageSummaryEntity>>

    @Query("SELECT * FROM weekly_usage_summary ORDER BY isoWeek DESC LIMIT 1")
    suspend fun latest(): WeeklyUsageSummaryEntity?

    @Query("DELETE FROM weekly_usage_summary")
    suspend fun clear()
}
