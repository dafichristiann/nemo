package com.anchor.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anchor.app.core.data.entity.InsightEntity
import kotlinx.coroutines.flow.Flow

/** DAO for [InsightEntity] (docs/superpowers/design/03-database-schema.md §6.1). */
@Dao
interface InsightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: InsightEntity)

    @Query("SELECT * FROM insight WHERE id = :id")
    suspend fun getById(id: String): InsightEntity?

    /** The daily-reflection row for a given local day, if it exists (cache lookup, FR-AI3). */
    @Query("SELECT * FROM insight WHERE dateEpochDay = :dateEpochDay AND type = 'DAILY_REFLECTION' LIMIT 1")
    suspend fun dailyReflection(dateEpochDay: Long): InsightEntity?

    @Query("SELECT * FROM insight ORDER BY dateEpochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<InsightEntity>>

    /** Insights for a given local day (any type), newest first. */
    @Query("SELECT * FROM insight WHERE dateEpochDay = :dateEpochDay ORDER BY createdAt DESC")
    suspend fun forDay(dateEpochDay: Long): List<InsightEntity>

    @Query("DELETE FROM insight WHERE dateEpochDay = :dateEpochDay AND type = 'DAILY_REFLECTION'")
    suspend fun deleteDailyReflection(dateEpochDay: Long)

    @Query("DELETE FROM insight")
    suspend fun clear()
}
