package com.anchor.app.core.data.mapper

import com.anchor.app.core.data.entity.TaskDependencyEntity
import com.anchor.app.core.domain.model.DependencyType
import com.anchor.app.core.domain.model.TaskDependency

/** Entity ↔ domain mapping for `task_dependency` (docs 03 §2.5). */
fun TaskDependencyEntity.toDomain(): TaskDependency = TaskDependency(
    id = id,
    fromTaskId = fromTaskId,
    toTaskId = toTaskId,
    type = type.toDependencyType(),
)

fun TaskDependency.toEntity(): TaskDependencyEntity = TaskDependencyEntity(
    id = id,
    fromTaskId = fromTaskId,
    toTaskId = toTaskId,
    type = type.name,
)

private fun String.toDependencyType(): DependencyType =
    DependencyType.entries.firstOrNull { it.name == this } ?: DependencyType.BLOCKS
