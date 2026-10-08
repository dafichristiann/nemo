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
 * How a task reached DONE (PRD v0.5 §10.1 "Effort-based Completion [LOCKED v0.5]").
 *
 *  - [VERIFIED] — completed by finishing a full Focus Session for the task (the normal path);
 *  - [MANUAL]   — completed by another path (e.g. recurring worker, future flows).
 *
 * Stored as `String?` in Room; `null` means "legacy row / not completed" and is never
 * backfilled by the migration. Parsing is defensive so an unknown value never crashes.
 */
enum class CompletionSource { MANUAL, VERIFIED }

/**
 * Recurrence rule on a [TaskDefinition] (docs 03 §2.1). Phase 1 supports
 * `DAILY` and `WEEKLY` (PRD FR-T7).
 */
enum class RecurrenceRule { NONE, DAILY, WEEKLY }
