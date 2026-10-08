package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Recurring template (docs/superpowers/design/03-database-schema.md §2.1). */
@Entity(tableName = "task_definition")
data class TaskDefinitionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val defaultPriority: String = "MEDIUM",
    val defaultDurationMin: Int? = null,
    val category: String? = null,
    val recurrenceRule: String = "NONE",
    val recurrenceMetaJson: String? = null,
    val createdAt: Long,
    val archivedAt: Long? = null,
)
