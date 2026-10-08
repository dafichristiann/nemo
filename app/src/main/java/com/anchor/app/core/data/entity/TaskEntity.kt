package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Concrete task (docs/superpowers/design/03-database-schema.md §2.2).
 *
 * Indexes on `definitionId`, `status`, `deadline` (§2.2). `definitionId` references a
 * [TaskDefinitionEntity] but is nullable (ad-hoc tasks have no template) and is set to
 * NULL on delete so a removed template never destroys instance history.
 */
@Entity(
    tableName = "task_instance",
    foreignKeys = [
        ForeignKey(
            entity = TaskDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["definitionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("definitionId"),
        Index("status"),
        Index("deadline"),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val definitionId: String? = null,
    val title: String,
    val description: String? = null,
    val deadline: Long? = null,
    val priority: String = "MEDIUM",
    val estimatedDurationMin: Int? = null,
    val category: String? = null,
    val status: String = "TODO",
    val createdAt: Long,
    val completedAt: Long? = null,
    val progressPercent: Int = 0,
    val parentTaskId: String? = null,
    val definitionOccurrenceDate: Long? = null,
    /**
     * How the task reached DONE (PRD v0.5 §10.1). `null` for legacy rows / unfinished tasks;
     * added in schema v10 via `MIGRATION_9_10` (no backfill).
     */
    val completionSource: String? = null,
)
