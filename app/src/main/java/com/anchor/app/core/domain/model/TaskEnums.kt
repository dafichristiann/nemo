package com.anchor.app.core.domain.model

/**
 * Priority of a task (docs/superpowers/design/03-database-schema.md §2.2).
 * Stored as String in Room so new values never need a migration.
 */
enum class TaskPriority { LOW, MEDIUM, HIGH, URGENT }

/**
 * Lifecycle status of a task (docs 03 §2.2, PRD §10.1 Task States):
 * `TODO → IN_PROGRESS → DONE → ARCHIVED`.
 */
enum class TaskStatus { TODO, IN_PROGRESS, DONE, ARCHIVED }

/**
 * Recurrence rule on a [TaskDefinition] (docs 03 §2.1). Phase 1 supports
 * `DAILY` and `WEEKLY` (PRD FR-T7).
 */
enum class RecurrenceRule { NONE, DAILY, WEEKLY }
