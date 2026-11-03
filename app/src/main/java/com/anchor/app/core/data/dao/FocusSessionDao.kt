package com.anchor.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anchor.app.core.data.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

/** DAO for [FocusSessionEntity] (docs/superpowers/design/03-database-schema.md §4.1). */
@Dao
interface FocusSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_session WHERE id = :id")
    suspend fun getById(id: String): FocusSessionEntity?

    /** The most recent session still in progress (ACTIVE/PAUSED), if any. */
    @Query("SELECT * FROM focus_session WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY startedAt DESC LIMIT 1")
    suspend fun getCurrent(): FocusSessionEntity?

    @Query("SELECT * FROM focus_session ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_session WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY startedAt DESC LIMIT 1")
    fun observeCurrent(): Flow<FocusSessionEntity?>

    /** Total focused milliseconds for sessions started in `[from, to)`. */
    @Query("SELECT COALESCE(SUM(actualDurationMs), 0) FROM focus_session WHERE startedAt >= :from AND startedAt < :to")
    suspend fun totalDurationBetween(from: Long, to: Long): Long

    @Query(
        """
        UPDATE focus_session
        SET status = :status, actualDurationMs = :actualDurationMs,
            endedAt = :endedAt, fallbackReason = :fallbackReason
        WHERE id = :id
        """,
    )
    suspend fun updateState(
        id: String,
        status: String,
        actualDurationMs: Long,
        endedAt: Long?,
        fallbackReason: String?,
    )

    @Query("UPDATE focus_session SET rating = :rating WHERE id = :id")
    suspend fun updateRating(id: String, rating: Int)

    @Query("SELECT * FROM focus_session WHERE rating IS NOT NULL AND startedAt >= :since ORDER BY startedAt ASC")
    suspend fun getRatedSessionsSince(since: Long): List<FocusSessionEntity>

    @Query("DELETE FROM focus_session")
    suspend fun clear()
}
