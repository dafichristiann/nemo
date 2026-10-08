package com.anchor.app.core.data.repository

import com.anchor.app.core.data.dao.RescheduleEventDao
import com.anchor.app.core.data.dao.TaskDefinitionDao
import com.anchor.app.core.data.mapper.toDomain
import com.anchor.app.core.data.mapper.toEntity
import com.anchor.app.core.domain.model.RescheduleEvent
import com.anchor.app.core.domain.model.TaskDefinition
import com.anchor.app.core.domain.repository.TaskDefinitionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Room-backed [TaskDefinitionRepository] (docs 02-architecture.md §3.1). */
@Singleton
class TaskDefinitionRepositoryImpl @Inject constructor(
    private val definitionDao: TaskDefinitionDao,
    private val rescheduleEventDao: RescheduleEventDao,
) : TaskDefinitionRepository {

    override fun observeActiveDefinitions(): Flow<List<TaskDefinition>> =
        definitionDao.observeActive().map { list -> list.map { it.toDomain() } }

    override suspend fun getRecurringDefinitions(): List<TaskDefinition> =
        definitionDao.getRecurring().map { it.toDomain() }

    override suspend fun getById(id: String): TaskDefinition? =
        definitionDao.getById(id)?.toDomain()

    override suspend fun insert(definition: TaskDefinition) =
        definitionDao.insert(definition.toEntity())

    override suspend fun upsert(definition: TaskDefinition) =
        definitionDao.upsert(definition.toEntity())

    override fun observeRescheduleEvents(taskId: String): Flow<List<RescheduleEvent>> =
        rescheduleEventDao.observeForTask(taskId).map { list -> list.map { it.toDomain() } }

    override fun observeRescheduleCount(): Flow<Int> = rescheduleEventDao.observeCount()

    override suspend fun recordReschedule(event: RescheduleEvent) =
        rescheduleEventDao.insert(event.toEntity())

    override suspend fun clear() {
        rescheduleEventDao.clear()
        definitionDao.clear()
    }
}
