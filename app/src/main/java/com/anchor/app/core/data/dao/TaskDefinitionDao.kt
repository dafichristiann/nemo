package com.anchor.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.anchor.app.core.data.entity.TaskDefinitionEntity
import kotlinx.coroutines.flow.Flow

/** DAO for [TaskDefinitionEntity] (docs 03 §2.1). */
@Dao
interface TaskDefinitionDao {

    @Query("SELECT * FROM task_definition WHERE archivedAt IS NULL")
    fun observeActive(): Flow<List<TaskDefinitionEntity>>

    /** Active recurring definitions, for the generator (PRD FR-T7). */
    @Query(
        """
        SELECT * FROM task_definition
        WHERE archivedAt IS NULL AND recurrenceRule != 'NONE'
        """,
    )
    suspend fun getRecurring(): List<TaskDefinitionEntity>

    @Query("SELECT * FROM task_definition WHERE id = :id")
    suspend fun getById(id: String): TaskDefinitionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(definition: TaskDefinitionEntity)

    @Upsert
    suspend fun upsert(definition: TaskDefinitionEntity)

    @Query("DELETE FROM task_definition")
    suspend fun clear()
}
