package com.anchor.app.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anchor.app.core.data.entity.TaskDependencyEntity
import kotlinx.coroutines.flow.Flow

/** DAO for [TaskDependencyEntity] (docs/superpowers/design/03-database-schema.md §2.5). */
@Dao
interface TaskDependencyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dependency: TaskDependencyEntity)

    @Query("SELECT * FROM task_dependency")
    suspend fun getAll(): List<TaskDependencyEntity>

    @Query("SELECT * FROM task_dependency")
    fun observeAll(): Flow<List<TaskDependencyEntity>>

    /** Rows where [taskId] is the predecessor (tasks that [taskId] may affect). */
    @Query("SELECT * FROM task_dependency WHERE fromTaskId = :taskId")
    suspend fun getByFrom(taskId: String): List<TaskDependencyEntity>

    /** Rows where [taskId] is the dependent (tasks [taskId] depends on). */
    @Query("SELECT * FROM task_dependency WHERE toTaskId = :taskId")
    suspend fun getByTo(taskId: String): List<TaskDependencyEntity>

    @Query("DELETE FROM task_dependency WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM task_dependency")
    suspend fun clear()
}
