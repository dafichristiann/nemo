package com.anchor.app.core.domain.model

/** Kind of task dependency (docs/superpowers/design/03-database-schema.md §2.5). */
enum class DependencyType { BLOCKS, RELATED }

/**
 * A dependency edge (docs 03 §2.5, PRD §10.7). [fromTaskId] precedes/affects [toTaskId].
 * Used to build the Domino Effect chain. Stored as `String` in Room.
 */
data class TaskDependency(
    val id: String,
    val fromTaskId: String,
    val toTaskId: String,
    val type: DependencyType,
)
