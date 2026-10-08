package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A dependency between two tasks (docs/superpowers/design/03-database-schema.md §2.5,
 * PRD §10.7). `fromTaskId` is the task that must happen first; `toTaskId` is the task that
 * may be affected by it.
 *
 * Cycles are rejected in the domain before insert (docs 03 §8, PRD §10.7 AC) — this table has
 * no DB trigger for that; the repository guards it. Rows cascade away with either task.
 */
@Entity(
    tableName = "task_dependency",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromTaskId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["toTaskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("fromTaskId"), Index("toTaskId")],
)
data class TaskDependencyEntity(
    @PrimaryKey val id: String,
    val fromTaskId: String,
    val toTaskId: String,
    val type: String,
)
