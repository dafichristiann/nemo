package com.anchor.app.core.domain.repository

import com.anchor.app.core.domain.model.RescheduleEvent
import com.anchor.app.core.domain.model.TaskDefinition
import kotlinx.coroutines.flow.Flow

/** Recurring templates + reschedule audit (docs 02-architecture.md §3.1). */
interface TaskDefinitionRepository {

    fun observeActiveDefinitions(): Flow<List<TaskDefinition>>

    suspend fun getRecurringDefinitions(): List<TaskDefinition>

    suspend fun getById(id: String): TaskDefinition?

    suspend fun insert(definition: TaskDefinition)

    suspend fun upsert(definition: TaskDefinition)

    // --- Reschedule history (FR-T12) ---
    fun observeRescheduleEvents(taskId: String): Flow<List<RescheduleEvent>>

    fun observeRescheduleCount(): Flow<Int>

    suspend fun recordReschedule(event: RescheduleEvent)

    suspend fun clear()
}
