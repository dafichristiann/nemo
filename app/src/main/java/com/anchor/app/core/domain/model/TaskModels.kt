package com.anchor.app.core.domain.model

/** A step inside a task (docs/superpowers/design/03-database-schema.md §2.3). */
data class Subtask(
    val id: String,
    val taskId: String,
    val title: String,
    val isDone: Boolean = false,
    val orderIndex: Int = 0,
)

/** A recurring template (docs 03 §2.1 / PRD §10.1 Recurring Task Architecture). */
data class TaskDefinition(
    val id: String,
    val title: String,
    val description: String? = null,
    val defaultPriority: TaskPriority = TaskPriority.MEDIUM,
    val defaultDurationMin: Int? = null,
    val category: String? = null,
    val recurrenceRule: RecurrenceRule = RecurrenceRule.NONE,
    val recurrenceMetaJson: String? = null,
    val createdAt: Long,
    val archivedAt: Long? = null,
)

/** Audit of a reschedule action, used later for delay-pattern insight (FR-T12). */
data class RescheduleEvent(
    val id: String,
    val taskId: String,
    val fromDeadline: Long?,
    val toDeadline: Long?,
    val reason: String? = null,
    val createdAt: Long,
)
