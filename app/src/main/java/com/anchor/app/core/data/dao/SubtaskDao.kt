package com.anchor.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.anchor.app.core.data.entity.SubtaskEntity
import kotlinx.coroutines.flow.Flow

/** DAO for [SubtaskEntity] (docs/superpowers/design/03-database-schema.md §2.3). */
@Dao
interface SubtaskDao {

    @Query("SELECT * FROM subtask WHERE taskId = :taskId ORDER BY orderIndex ASC")
    fun observeForTask(taskId: String): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtask WHERE taskId = :taskId ORDER BY orderIndex ASC")
    suspend fun getForTask(taskId: String): List<SubtaskEntity>

    @Query("SELECT COUNT(*) FROM subtask WHERE taskId = :taskId")
    suspend fun countForTask(taskId: String): Int

    @Query("SELECT COUNT(*) FROM subtask WHERE taskId = :taskId AND isDone = 1")
    suspend fun countDoneForTask(taskId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(subtask: SubtaskEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(subtasks: List<SubtaskEntity>)

    @Update
    suspend fun update(subtask: SubtaskEntity)

    @Upsert
    suspend fun upsert(subtask: SubtaskEntity)

    @Query("DELETE FROM subtask WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM subtask WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: String)

    @Query("DELETE FROM subtask")
    suspend fun clear()
}
