package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A step inside a task (docs/superpowers/design/03-database-schema.md §2.3).
 * Cascades when its parent [TaskEntity] is deleted (§8).
 */
@Entity(
    tableName = "subtask",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class SubtaskEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val title: String,
    val isDone: Boolean = false,
    val orderIndex: Int = 0,
)
