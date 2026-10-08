package com.anchor.app.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anchor.app.core.data.dao.AppCategoryDao
import com.anchor.app.core.data.dao.AppSettingsDao
import com.anchor.app.core.data.dao.DailyUsageSummaryDao
import com.anchor.app.core.data.dao.FocusSessionDao
import com.anchor.app.core.data.dao.InsightDao
import com.anchor.app.core.data.dao.InterventionLogDao
import com.anchor.app.core.data.dao.RescheduleEventDao
import com.anchor.app.core.data.dao.SubtaskDao
import com.anchor.app.core.data.dao.TaskDao
import com.anchor.app.core.data.dao.TaskDefinitionDao
import com.anchor.app.core.data.dao.TaskDependencyDao
import com.anchor.app.core.data.dao.UsageSessionDao
import com.anchor.app.core.data.dao.WeeklyUsageSummaryDao
import com.anchor.app.core.data.entity.AppCategoryEntity
import com.anchor.app.core.data.entity.AppSettingsEntity
import com.anchor.app.core.data.entity.DailyUsageSummaryEntity
import com.anchor.app.core.data.entity.FocusSessionEntity
import com.anchor.app.core.data.entity.InsightEntity
import com.anchor.app.core.data.entity.InterventionLogEntity
import com.anchor.app.core.data.entity.RescheduleEventEntity
import com.anchor.app.core.data.entity.SubtaskEntity
import com.anchor.app.core.data.entity.TaskDefinitionEntity
import com.anchor.app.core.data.entity.TaskDependencyEntity
import com.anchor.app.core.data.entity.TaskEntity
import com.anchor.app.core.data.entity.UsageSessionEntity
import com.anchor.app.core.data.entity.WeeklyUsageSummaryEntity

/**
 * Anchor's Room database — the single source of truth (docs 02-architecture.md §0).
 *
 * Schema versioning (docs 03-database-schema.md §9):
 *  - v1: `app_settings` (Sprint 0)
 *  - v2: + `task_definition`, `task_instance`, `subtask`, `reschedule_event` (Sprint 1)
 *  - v3: + `usage_session`, `daily_usage_summary`, `weekly_usage_summary`, `app_category` (Sprint 2)
 *  - v4: + `intervention_log` (Sprint 4)
 *  - v5: + `focus_session` (Sprint 5)
 *  - v6: + `task_dependency` (Sprint 6)
 *  - v7: + `insight` (Sprint 7)
 *
 * `exportSchema = true` writes JSON schema files to `app/schemas` (KSP `room.schemaLocation`).
 * Migrations are explicit; there is no destructive fallback.
 */
@Database(
    entities = [
        AppSettingsEntity::class,
        TaskDefinitionEntity::class,
        TaskEntity::class,
        SubtaskEntity::class,
        RescheduleEventEntity::class,
        UsageSessionEntity::class,
        DailyUsageSummaryEntity::class,
        WeeklyUsageSummaryEntity::class,
        AppCategoryEntity::class,
        InterventionLogEntity::class,
        FocusSessionEntity::class,
        TaskDependencyEntity::class,
        InsightEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class AnchorDatabase : RoomDatabase() {
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun taskDefinitionDao(): TaskDefinitionDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun rescheduleEventDao(): RescheduleEventDao
    abstract fun usageSessionDao(): UsageSessionDao
    abstract fun dailyUsageSummaryDao(): DailyUsageSummaryDao
    abstract fun weeklyUsageSummaryDao(): WeeklyUsageSummaryDao
    abstract fun appCategoryDao(): AppCategoryDao
    abstract fun interventionLogDao(): InterventionLogDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun taskDependencyDao(): TaskDependencyDao
    abstract fun insightDao(): InsightDao

    companion object {
        const val NAME = "anchor.db"

        /** Current schema version (mirrors the `@Database(version = ...)` above). */
        const val VERSION = 7

        /** All migration steps, in order — used by tests and by [DatabaseModule]. */
        val ALL_MIGRATIONS: Array<Migration>
            get() = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)

        /** v1 → v2: add the Phase-1 task tables (docs 03 §2). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_definition` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `defaultPriority` TEXT NOT NULL,
                        `defaultDurationMin` INTEGER,
                        `category` TEXT,
                        `recurrenceRule` TEXT NOT NULL,
                        `recurrenceMetaJson` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `archivedAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_instance` (
                        `id` TEXT NOT NULL,
                        `definitionId` TEXT,
                        `title` TEXT NOT NULL,
                        `description` TEXT,
                        `deadline` INTEGER,
                        `priority` TEXT NOT NULL,
                        `estimatedDurationMin` INTEGER,
                        `category` TEXT,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `progressPercent` INTEGER NOT NULL,
                        `parentTaskId` TEXT,
                        `definitionOccurrenceDate` INTEGER,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`definitionId`) REFERENCES `task_definition`(`id`)
                            ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_instance_definitionId` ON `task_instance`(`definitionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_instance_status` ON `task_instance`(`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_instance_deadline` ON `task_instance`(`deadline`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subtask` (
                        `id` TEXT NOT NULL,
                        `taskId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `isDone` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`taskId`) REFERENCES `task_instance`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_subtask_taskId` ON `subtask`(`taskId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reschedule_event` (
                        `id` TEXT NOT NULL,
                        `taskId` TEXT NOT NULL,
                        `fromDeadline` INTEGER,
                        `toDeadline` INTEGER,
                        `reason` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`taskId`) REFERENCES `task_instance`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reschedule_event_taskId` ON `reschedule_event`(`taskId`)")
            }
        }

        /** v2 → v3: add the Phase-2 usage/wellbeing tables (docs 03 §3). */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `usage_session` (
                        `id` TEXT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `collectedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_session_startTime` ON `usage_session`(`startTime`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_usage_session_packageName` ON `usage_session`(`packageName`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_usage_summary` (
                        `dateEpochDay` INTEGER NOT NULL,
                        `totalScreenTimeMs` INTEGER NOT NULL,
                        `distractionRatio` REAL,
                        `topAppsJson` TEXT NOT NULL,
                        `categoryBreakdownJson` TEXT NOT NULL,
                        `sessionCount` INTEGER NOT NULL,
                        `computedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`dateEpochDay`)
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `weekly_usage_summary` (
                        `isoWeek` TEXT NOT NULL,
                        `totalScreenTimeMs` INTEGER NOT NULL,
                        `dailyAvgMs` INTEGER NOT NULL,
                        `baselineJson` TEXT NOT NULL,
                        `trendJson` TEXT NOT NULL,
                        `computedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`isoWeek`)
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_category` (
                        `packageName` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `isDistraction` INTEGER NOT NULL,
                        `overrideSource` TEXT NOT NULL,
                        PRIMARY KEY(`packageName`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /** v3 → v4: add the Phase-3 intervention log (docs 03 §5.1). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `intervention_log` (
                        `id` TEXT NOT NULL,
                        `level` INTEGER NOT NULL,
                        `channel` TEXT NOT NULL,
                        `contextPackage` TEXT,
                        `relatedTaskId` TEXT,
                        `status` TEXT NOT NULL,
                        `shownAt` INTEGER NOT NULL,
                        `resolvedAt` INTEGER,
                        `cooldownTriggered` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_intervention_log_shownAt` ON `intervention_log`(`shownAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_intervention_log_contextPackage` ON `intervention_log`(`contextPackage`)")
            }
        }

        /** v4 → v5: add the Phase-3 focus session table (docs 03 §4.1). */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_session` (
                        `id` TEXT NOT NULL,
                        `taskId` TEXT,
                        `mode` TEXT NOT NULL,
                        `plannedDurationMin` INTEGER,
                        `actualDurationMs` INTEGER NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `endedAt` INTEGER,
                        `status` TEXT NOT NULL,
                        `blockedAppsJson` TEXT,
                        `fallbackReason` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_session_startedAt` ON `focus_session`(`startedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_session_taskId` ON `focus_session`(`taskId`)")
            }
        }

        /** v5 → v6: add the Phase-4 task dependency table (docs 03 §2.5). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_dependency` (
                        `id` TEXT NOT NULL,
                        `fromTaskId` TEXT NOT NULL,
                        `toTaskId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`fromTaskId`) REFERENCES `task_instance`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`toTaskId`) REFERENCES `task_instance`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_dependency_fromTaskId` ON `task_dependency`(`fromTaskId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_dependency_toTaskId` ON `task_dependency`(`toTaskId`)")
            }
        }

        /** v6 → v7: add the Phase-5 insight table (docs 03 §6.1). */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `insight` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `patternsJson` TEXT NOT NULL,
                        `riskLevel` TEXT,
                        `suggestionsJson` TEXT NOT NULL,
                        `recommendedAction` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_insight_dateEpochDay` ON `insight`(`dateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_insight_type` ON `insight`(`type`)")
            }
        }
    }
}
