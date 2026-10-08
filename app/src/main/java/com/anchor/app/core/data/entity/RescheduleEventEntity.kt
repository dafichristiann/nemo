package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Reschedule audit row (docs/superpowers/design/03-database-schema.md §2.4).
 * Cascades with its parent task (§8). Used by insights to detect delay patterns.
 */
@Entity(
    tableName = "reschedule_event",
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
data class RescheduleEventEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val fromDeadline: Long? = null,
    val toDeadline: Long? = null,
    val reason: String? = null,
    val createdAt: Long,
)
