package com.anchor.app.testutil

import com.anchor.app.core.common.IdProvider
import com.anchor.app.core.domain.model.DependencyType
import com.anchor.app.core.domain.model.TaskDependency
import com.anchor.app.core.domain.repository.TaskDependencyRepository
import com.anchor.app.core.domain.repository.TaskDependencyRepository.AddDependencyResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [TaskDependencyRepository] fake (docs 11-testing-strategy.md §5). Mirrors the cycle
 * check of the real implementation so the same behaviour is unit-tested without Room.
 */
class FakeTaskDependencyRepository(
    private val idProvider: IdProvider = com.anchor.app.core.common.FakeIdProvider(),
) : TaskDependencyRepository {

    val dependencies = MutableStateFlow<List<TaskDependency>>(emptyList())

    override fun observeAll(): Flow<List<TaskDependency>> = dependencies

    override suspend fun getAll(): List<TaskDependency> = dependencies.value

    override suspend fun downstreamOf(taskId: String): List<TaskDependency> =
        dependencies.value.filter { it.fromTaskId == taskId }

    override suspend fun add(
        fromTaskId: String,
        toTaskId: String,
        type: DependencyType,
    ): AddDependencyResult {
        if (fromTaskId == toTaskId) {
            return AddDependencyResult.Cycle(listOf(fromTaskId, toTaskId))
        }
        val path = findCyclePath(from = toTaskId, target = fromTaskId)
        if (path != null) {
            return AddDependencyResult.Cycle(listOf(fromTaskId) + path)
        }
        val dependency = TaskDependency(idProvider.newId(), fromTaskId, toTaskId, type)
        dependencies.value = dependencies.value + dependency
        return AddDependencyResult.Added(dependency)
    }

    override suspend fun remove(id: String) {
        dependencies.value = dependencies.value.filterNot { it.id == id }
    }

    override suspend fun clear() {
        dependencies.value = emptyList()
    }

    private fun findCyclePath(from: String, target: String): List<String>? {
        val adjacency = dependencies.value.groupBy({ it.fromTaskId }, { it.toTaskId })
        val visited = mutableSetOf(from)
        val queue = ArrayDeque<List<String>>()
        queue.add(listOf(from))
        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val node = path.last()
            if (node == target) return path
            adjacency[node].orEmpty().forEach { next -> if (visited.add(next)) queue.add(path + next) }
        }
        return null
    }
}
