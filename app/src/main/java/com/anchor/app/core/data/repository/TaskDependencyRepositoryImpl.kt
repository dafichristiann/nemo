package com.anchor.app.core.data.repository

import com.anchor.app.core.common.IdProvider
import com.anchor.app.core.data.dao.TaskDependencyDao
import com.anchor.app.core.data.mapper.toDomain
import com.anchor.app.core.data.mapper.toEntity
import com.anchor.app.core.domain.model.DependencyType
import com.anchor.app.core.domain.model.TaskDependency
import com.anchor.app.core.domain.repository.TaskDependencyRepository
import com.anchor.app.core.domain.repository.TaskDependencyRepository.AddDependencyResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Room-backed [TaskDependencyRepository] (docs 02-architecture.md §3.1).
 *
 * Cycle prevention lives here (docs 03 §8: "dicek di domain, bukan DB trigger"): before persisting
 * `from → to` it walks downstream from `to`; if it reaches `from`, the edge would close a loop and
 * is refused (PRD §10.7 AC). The traversal also returns the offending path for a clear explanation.
 */
class TaskDependencyRepositoryImpl @Inject constructor(
    private val dao: TaskDependencyDao,
    private val idProvider: IdProvider,
) : TaskDependencyRepository {

    override fun observeAll(): Flow<List<TaskDependency>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getAll(): List<TaskDependency> = dao.getAll().map { it.toDomain() }

    override suspend fun downstreamOf(taskId: String): List<TaskDependency> =
        dao.getByFrom(taskId).map { it.toDomain() }

    override suspend fun add(
        fromTaskId: String,
        toTaskId: String,
        type: DependencyType,
    ): AddDependencyResult {
        if (fromTaskId == toTaskId) {
            // A self-edge is the smallest cycle; refuse it explicitly.
            return AddDependencyResult.Cycle(listOf(fromTaskId, toTaskId))
        }

        val cyclePath = findCyclePath(from = toTaskId, target = fromTaskId)
        if (cyclePath != null) {
            // The new edge `from → to` would close `to → … → from`.
            return AddDependencyResult.Cycle(listOf(fromTaskId) + cyclePath)
        }

        val dependency = TaskDependency(
            id = idProvider.newId(),
            fromTaskId = fromTaskId,
            toTaskId = toTaskId,
            type = type,
        )
        dao.insert(dependency.toEntity())
        return AddDependencyResult.Added(dependency)
    }

    override suspend fun remove(id: String) = dao.deleteById(id)

    override suspend fun clear() = dao.clear()

    /**
     * Breadth-first search following `from → to` edges starting at [from]. Returns the path
     * `[from, …, target]` when [target] is reachable, else null. Bounded by the visited set so
     * an existing (should-not-happen) cycle can never loop forever.
     */
    private suspend fun findCyclePath(from: String, target: String): List<String>? {
        val adjacency: Map<String, List<String>> = dao.getAll()
            .groupBy({ it.fromTaskId }, { it.toTaskId })

        val visited = mutableSetOf(from)
        val queue = ArrayDeque<List<String>>()
        queue.add(listOf(from))

        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val node = path.last()
            if (node == target) return path
            adjacency[node].orEmpty().forEach { next ->
                if (visited.add(next)) {
                    queue.add(path + next)
                }
            }
        }
        return null
    }
}
