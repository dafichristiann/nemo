package com.anchor.app.core.domain.usecase.futureview

import com.anchor.app.core.domain.model.DominoEffect
import com.anchor.app.core.domain.model.DominoNode
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.repository.TaskDependencyRepository
import com.anchor.app.core.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Builds the Domino Effect chain for a task (docs/superpowers/design/07-screens.md §2.11,
 * PRD §10.7, FR-DE1..DE5).
 *
 * Walks `from → to` dependency edges breadth-first from [task], so the result lists every task
 * that *may* be affected, with its depth. A visited set guards against any accidental cycle so
 * the traversal always terminates. A task with no dependents yields an empty chain (not an error)
 * — the UI shows a clear empty state (PRD §10.7 Edge Cases).
 */
class ComputeDominoEffectUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val dependencyRepository: TaskDependencyRepository,
) {

    suspend operator fun invoke(taskId: String): DominoEffect? {
        val rootTask: Task = taskRepository.getById(taskId) ?: return null
        val allDependencies = dependencyRepository.getAll()
        val adjacency = allDependencies.groupBy({ it.fromTaskId }, { it.toTaskId })

        val affected = mutableListOf<DominoNode>()
        val visited = mutableSetOf(taskId)
        var frontier = listOf(taskId)
        var depth = 0

        while (frontier.isNotEmpty()) {
            depth += 1
            val next = mutableListOf<String>()
            frontier.forEach { current ->
                adjacency[current].orEmpty().forEach { child ->
                    if (visited.add(child)) {
                        taskRepository.getById(child)?.let { childTask ->
                            affected += DominoNode(taskId = childTask.id, title = childTask.title, depth = depth)
                        }
                        next += child
                    }
                }
            }
            frontier = next
        }

        return DominoEffect(
            root = DominoNode(taskId = rootTask.id, title = rootTask.title, depth = 0),
            affected = affected,
        )
    }
}
