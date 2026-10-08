package com.anchor.app.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row settings table (docs/superpowers/design/03-database-schema.md §7.1).
 *
 * Defaults are privacy-preserving: tracking & AI are OFF until the user opts in
 * (PRD FR-S6, docs 09-privacy §0).
 *
 * Enum-valued fields are stored as `String` (via the enums' `.name`) so adding new
 * values never requires a migration (docs 03 §0 point 6).
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val trackingEnabled: Boolean = false,
    val aiEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val usageRetentionDays: Int = DEFAULT_RETENTION_DAYS,
    val defaultFocusMode: String = "GENTLE",
    val dailyInterventionQuota: Int = 3,
    val cooldownHours: Int = 2,
    val onboardingCompleted: Boolean = false,
    val locale: String = "en",
) {
    companion object {
        const val SINGLETON_ID = 1
        const val DEFAULT_RETENTION_DAYS = 30
    }
}
