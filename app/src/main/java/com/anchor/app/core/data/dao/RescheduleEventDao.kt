package com.anchor.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anchor.app.core.data.entity.RescheduleEventEntity
import kotlinx.coroutines.flow.Flow

/** DAO for [RescheduleEventEntity] (docs 03 §2.4). */
@Dao
interface RescheduleEventDao {

    @Query("SELECT * FROM reschedule_event WHERE taskId = :taskId ORDER BY createdAt DESC")
    fun observeForTask(taskId: String): Flow<List<RescheduleEventEntity>>

    @Query("SELECT COUNT(*) FROM reschedule_event")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: RescheduleEventEntity)

    @Query("DELETE FROM reschedule_event")
    suspend fun clear()
}
