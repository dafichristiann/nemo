package com.anchor.app.core.domain.model

/**
 * A concrete, actionable task (docs/superpowers/design/03-database-schema.md §2.2 /
 * PRD §10.1 "Task Instance"). This is the primary unit shown in lists.
 */
data class Task(
    val id: String,
    val definitionId: String? = null,
    val title: String,
    val description: String? = null,
    val deadline: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val estimatedDurationMin: Int? = null,
    val category: String? = null,
    val status: TaskStatus = TaskStatus.TODO,
    val createdAt: Long,
    val completedAt: Long? = null,
    val progressPercent: Int = 0,
    val parentTaskId: String? = null,
    val definitionOccurrenceDate: Long? = null,
) {
    val isDone: Boolean get() = status == TaskStatus.DONE || status == TaskStatus.ARCHIVED

    /**
     * Overdue = has a deadline in the past and is not yet done. Overdue is a **soft,
     * non-judgmental** signal only (PRD §10.1 Overdue Behavior) — never styled as red.
     */
    fun isOverdue(nowMillis: Long): Boolean =
        !isDone && deadline != null && deadline < nowMillis
}
