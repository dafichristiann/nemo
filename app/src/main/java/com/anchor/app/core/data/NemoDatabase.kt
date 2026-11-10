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
 * Nemo's Room database — the single source of truth (docs 02-architecture.md §0).
 *
 * Schema versioning (docs 03-database-schema.md §9):
 *  - v1: `app_settings` (Sprint 0)
 *  - v2: + `task_definition`, `task_instance`, `subtask`, `reschedule_event` (Sprint 1)
 *  - v3: + `usage_session`, `daily_usage_summary`, `weekly_usage_summary`, `app_category` (Sprint 2)
 *  - v4: + `intervention_log` (Sprint 4)
 *  - v5: + `focus_session` (Sprint 5)
 *  - v6: + `task_dependency` (Sprint 6)
 *  - v7: + `insight` (Sprint 7)
 *  - v8: + `app_settings.blockedAppsJson` (FR-S4)
 *  - v9: + `app_settings.focusBlockingEnabled` (FR-F4/F8, PRD v0.4 §10.5)
 *  - v10: + `task_instance.completionSource` (FR-T8, PRD v0.5 §10.1)
 *  - v11: `focus_session.plannedDurationMin` → `plannedDurationSec` (second-precise focus durations)
 *  - v12: + `app_settings.nickname`, `focusArea`, `defaultFocusDurationMin` (onboarding profile)
 *  - v13: unique index on `task_instance(definitionId, definitionOccurrenceDate)` (FR-T7 no double instance)
 *  - v14: + `app_settings.doomscrollAlertEnabled`, `doomscrollThresholdMinutes` (continuous doomscroll alert)
 *  - v15: + `focus_session.rating`, `app_settings.manualWakeTimeEpochDay`, `manualWakeTimeMinutes` (Adaptive Circadian)
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
    version = 15,
    exportSchema = true,
)
abstract class NemoDatabase : RoomDatabase() {
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
        const val NAME = "nemo.db"

        /** Current schema version (mirrors the `@Database(version = ...)` above). */
        const val VERSION = 15

        /** All migration steps, in order — used by tests and by [DatabaseModule]. */
        val ALL_MIGRATIONS: Array<Migration>
            get() = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)

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

        /** v7 → v8: add `blockedAppsJson` to `app_settings` for FR-S4 (Sprint 8). */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `blockedAppsJson` TEXT")
            }
        }

        /** v8 → v9: opt-in toggle for Strict/Custom blocking (FR-F4/F8). Default OFF. */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `focusBlockingEnabled` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v9 → v10: record how a task was completed (PRD v0.5 §10.1). Nullable; legacy rows
         * keep `NULL` (never backfilled) so historical data is left untouched.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `task_instance` ADD COLUMN `completionSource` TEXT")
            }
        }

        /**
         * v10 → v11: store the planned focus duration in **seconds** instead of whole minutes,
         * so the Setup wheel can pick second-precise durations (FR-F1). Existing rows are
         * backfilled at the old resolution (minutes × 60) — no data is lost.
         *
         * Implemented as a table recreate (create new → copy → drop old → rename) rather than
         * `ALTER TABLE ... DROP COLUMN`, which needs SQLite 3.35+ (Android 13+) and would crash
         * on older devices (minSdk 26).
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_session_new` (
                        `id` TEXT NOT NULL,
                        `taskId` TEXT,
                        `mode` TEXT NOT NULL,
                        `plannedDurationSec` INTEGER,
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
                db.execSQL(
                    """
                    INSERT INTO `focus_session_new`
                        (`id`, `taskId`, `mode`, `plannedDurationSec`, `actualDurationMs`,
                         `startedAt`, `endedAt`, `status`, `blockedAppsJson`, `fallbackReason`)
                    SELECT `id`, `taskId`, `mode`, `plannedDurationMin` * 60, `actualDurationMs`,
                           `startedAt`, `endedAt`, `status`, `blockedAppsJson`, `fallbackReason`
                    FROM `focus_session`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `focus_session`")
                db.execSQL("ALTER TABLE `focus_session_new` RENAME TO `focus_session`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_session_startedAt` ON `focus_session`(`startedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_session_taskId` ON `focus_session`(`taskId`)")
            }
        }

        /**
         * v11 → v12: onboarding profile columns for personalization (docs 07-screens.md §2.3b).
         * `nickname` is nullable (legacy rows keep `NULL` = no nickname); `focusArea` and
         * `defaultFocusDurationMin` default to the app defaults so existing installs are unchanged.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `nickname` TEXT")
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `focusArea` TEXT NOT NULL DEFAULT 'COLLEGE'")
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `defaultFocusDurationMin` INTEGER NOT NULL DEFAULT 25")
            }
        }

        /**
         * v12 → v13: enforce the "no double instance" recurring guarantee (FR-T7) at the DB
         * level with a unique index on `(definitionId, definitionOccurrenceDate)`.
         *
         * A pre-existing install may already contain duplicates (the old check-then-act guard
         * was not atomic). Those are collapsed first — keeping the OLDEST row per
         * `(definitionId, definitionOccurrenceDate)` group — so creating the unique index cannot
         * fail. Rows with a NULL definitionId/definitionOccurrenceDate are left untouched
         * (SQLite treats NULLs as distinct in unique indexes).
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    DELETE FROM `task_instance`
                    WHERE `definitionId` IS NOT NULL
                      AND `definitionOccurrenceDate` IS NOT NULL
                      AND EXISTS (
                          SELECT 1 FROM `task_instance` t2
                          WHERE t2.`definitionId` = `task_instance`.`definitionId`
                            AND t2.`definitionOccurrenceDate` = `task_instance`.`definitionOccurrenceDate`
                            AND (t2.`createdAt` < `task_instance`.`createdAt`
                                 OR (t2.`createdAt` = `task_instance`.`createdAt` AND t2.`id` < `task_instance`.`id`))
                      )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_task_instance_definitionId_definitionOccurrenceDate` " +
                        "ON `task_instance`(`definitionId`, `definitionOccurrenceDate`)",
                )
            }
        }

        /**
         * v13 → v14: continuous doomscroll alert toggle and threshold minutes.
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `doomscrollAlertEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `doomscrollThresholdMinutes` INTEGER NOT NULL DEFAULT 20")
            }
        }

        /**
         * v14 → v15: + focus_session.rating, app_settings.manualWakeTimeEpochDay & manualWakeTimeMinutes (Adaptive Circadian).
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `focus_session` ADD COLUMN `rating` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `manualWakeTimeEpochDay` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `manualWakeTimeMinutes` INTEGER DEFAULT NULL")
            }
        }
    }
}
