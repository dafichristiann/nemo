package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * App category + distraction flag — user-overridable CONFIG
 * (docs 03-database-schema.md §3.4, PRD FR-S3).
 *
 * `overrideSource` is `AUTO` (assigned by a heuristic) or `USER` (set explicitly); a USER
 * value must never be overwritten by an AUTO pass.
 */
@Entity(tableName = "app_category")
data class AppCategoryEntity(
    @PrimaryKey val packageName: String,
    val category: String,
    val isDistraction: Boolean = false,
    val overrideSource: String = SOURCE_AUTO,
) {
    companion object {
        const val SOURCE_AUTO = "AUTO"
        const val SOURCE_USER = "USER"
    }
}
