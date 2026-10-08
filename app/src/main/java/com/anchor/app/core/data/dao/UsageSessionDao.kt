package com.anchor.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anchor.app.core.data.entity.UsageSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for RAW `usage_session` rows (docs 03-database-schema.md §3.1).
 *
 * Writes are conflict-ignore so re-collecting an overlapping window stays idempotent
 * (docs 02-architecture.md §5: workers must be idempotent).
 */
@Dao
interface UsageSessionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sessions: List<UsageSessionEntity>)

    /**
     * Sessions that **overlap** [from, to): a session counts if it ends after [from] and
     * starts before [to]. This is what lets a midnight-crossing session be clipped into both
     * days during aggregation (docs 03 §3.2).
     */
    @Query("SELECT * FROM usage_session WHERE endTime > :from AND startTime < :to ORDER BY startTime")
    suspend fun getBetween(from: Long, to: Long): List<UsageSessionEntity>

    @Query("SELECT * FROM usage_session ORDER BY startTime DESC")
    fun observeAll(): Flow<List<UsageSessionEntity>>

    @Query("SELECT MIN(startTime) FROM usage_session")
    suspend fun earliestStartTime(): Long?

    @Query("SELECT MAX(startTime) FROM usage_session")
    suspend fun latestStartTime(): Long?

    @Query("SELECT COUNT(*) FROM usage_session")
    suspend fun count(): Int

    /** Pruning: delete raw rows older than [cutoff] (PRD §10.2 retention, docs 03 §10). */
    @Query("DELETE FROM usage_session WHERE startTime < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query("DELETE FROM usage_session")
    suspend fun clear()
}
