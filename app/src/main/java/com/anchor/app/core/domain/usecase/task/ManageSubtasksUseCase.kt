package com.anchor.app.core.domain.usecase.task

import com.anchor.app.core.common.IdProvider
import com.anchor.app.core.domain.model.Subtask
import com.anchor.app.core.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Subtask CRUD + progress recomputation (PRD FR-T6, FR-T9).
 *
 * Progress = round(done / total * 100) when subtasks exist; 0 when there are none
 * (FR-T9: "jika tidak ada → 0/100"). Every mutation refreshes the parent task's
 * `progressPercent` so the UI stays consistent.
 */
class ManageSubtasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val idProvider: IdProvider,
) {
    suspend fun add(taskId: String, title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val existing = taskRepository.getSubtasks(taskId)
        taskRepository.addSubtask(
            Subtask(
                id = idProvider.newId(),
                taskId = taskId,
                title = clean,
                orderIndex = existing.size,
            ),
        )
        recomputeProgress(taskId)
    }

    suspend fun toggle(subtask: Subtask) {
        taskRepository.updateSubtask(subtask.copy(isDone = !subtask.isDone))
        recomputeProgress(subtask.taskId)
    }

    suspend fun delete(subtask: Subtask) {
        taskRepository.deleteSubtask(subtask.id)
        recomputeProgress(subtask.taskId)
    }

    suspend fun rename(subtask: Subtask, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        taskRepository.updateSubtask(subtask.copy(title = clean))
    }

    /** Move a subtask up/down by swapping [orderIndex] with its neighbour. */
    suspend fun move(taskId: String, subtask: Subtask, delta: Int) {
        val list = taskRepository.getSubtasks(taskId).sortedBy { it.orderIndex }
        val index = list.indexOfFirst { it.id == subtask.id }
        if (index < 0) return
        val target = (index + delta).coerceIn(0, list.lastIndex)
        if (target == index) return

        val reordered = list.toMutableList().apply {
            val moved = removeAt(index)
            add(target, moved)
        }
        reordered.forEachIndexed { i, s ->
            if (s.orderIndex != i) taskRepository.updateSubtask(s.copy(orderIndex = i))
        }
    }

    /** Recompute and persist progress for [taskId] (FR-T9). */
    suspend fun recomputeProgress(taskId: String) {
        val subtasks = taskRepository.getSubtasks(taskId)
        val progress = if (subtasks.isEmpty()) {
            0
        } else {
            (subtasks.count { it.isDone } * 100f / subtasks.size).toInt()
        }
        taskRepository.setProgress(taskId, progress)
    }
}
