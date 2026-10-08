package com.anchor.app.core.data.mapper

import com.anchor.app.core.data.entity.RescheduleEventEntity
import com.anchor.app.core.data.entity.SubtaskEntity
import com.anchor.app.core.data.entity.TaskDefinitionEntity
import com.anchor.app.core.data.entity.TaskEntity
import com.anchor.app.core.domain.model.RecurrenceRule
import com.anchor.app.core.domain.model.RescheduleEvent
import com.anchor.app.core.domain.model.Subtask
import com.anchor.app.core.domain.model.Task
import com.anchor.app.core.domain.model.TaskDefinition
import com.anchor.app.core.domain.model.TaskPriority
import com.anchor.app.core.domain.model.TaskStatus

/**
 * Entity ↔ domain mapping. Enums are stored as `String` (docs 03 §0 point 6); these
 * helpers parse defensively so an unknown value never crashes (graceful degradation).
 */

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    definitionId = definitionId,
    title = title,
    description = description,
    deadline = deadline,
    priority = priority.toTaskPriority(),
    estimatedDurationMin = estimatedDurationMin,
    category = category,
    status = status.toTaskStatus(),
    createdAt = createdAt,
    completedAt = completedAt,
    progressPercent = progressPercent,
    parentTaskId = parentTaskId,
    definitionOccurrenceDate = definitionOccurrenceDate,
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    definitionId = definitionId,
    title = title,
    description = description,
    deadline = deadline,
    priority = priority.name,
    estimatedDurationMin = estimatedDurationMin,
    category = category,
    status = status.name,
    createdAt = createdAt,
    completedAt = completedAt,
    progressPercent = progressPercent,
    parentTaskId = parentTaskId,
    definitionOccurrenceDate = definitionOccurrenceDate,
)

fun SubtaskEntity.toDomain(): Subtask =
    Subtask(id = id, taskId = taskId, title = title, isDone = isDone, orderIndex = orderIndex)

fun Subtask.toEntity(): SubtaskEntity =
    SubtaskEntity(id = id, taskId = taskId, title = title, isDone = isDone, orderIndex = orderIndex)

fun TaskDefinitionEntity.toDomain(): TaskDefinition = TaskDefinition(
    id = id,
    title = title,
    description = description,
    defaultPriority = defaultPriority.toTaskPriority(),
    defaultDurationMin = defaultDurationMin,
    category = category,
    recurrenceRule = recurrenceRule.toRecurrenceRule(),
    recurrenceMetaJson = recurrenceMetaJson,
    createdAt = createdAt,
    archivedAt = archivedAt,
)

fun TaskDefinition.toEntity(): TaskDefinitionEntity = TaskDefinitionEntity(
    id = id,
    title = title,
    description = description,
    defaultPriority = defaultPriority.name,
    defaultDurationMin = defaultDurationMin,
    category = category,
    recurrenceRule = recurrenceRule.name,
    recurrenceMetaJson = recurrenceMetaJson,
    createdAt = createdAt,
    archivedAt = archivedAt,
)

fun RescheduleEventEntity.toDomain(): RescheduleEvent = RescheduleEvent(
    id = id,
    taskId = taskId,
    fromDeadline = fromDeadline,
    toDeadline = toDeadline,
    reason = reason,
    createdAt = createdAt,
)

fun RescheduleEvent.toEntity(): RescheduleEventEntity = RescheduleEventEntity(
    id = id,
    taskId = taskId,
    fromDeadline = fromDeadline,
    toDeadline = toDeadline,
    reason = reason,
    createdAt = createdAt,
)

private fun String.toTaskPriority(): TaskPriority =
    runCatching { TaskPriority.valueOf(this) }.getOrDefault(TaskPriority.MEDIUM)

private fun String.toTaskStatus(): TaskStatus =
    runCatching { TaskStatus.valueOf(this) }.getOrDefault(TaskStatus.TODO)

private fun String.toRecurrenceRule(): RecurrenceRule =
    runCatching { RecurrenceRule.valueOf(this) }.getOrDefault(RecurrenceRule.NONE)
