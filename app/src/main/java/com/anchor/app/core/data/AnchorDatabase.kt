package com.anchor.app.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anchor.app.core.data.dao.AppSettingsDao
import com.anchor.app.core.data.dao.RescheduleEventDao
import com.anchor.app.core.data.dao.SubtaskDao
import com.anchor.app.core.data.dao.TaskDao
import com.anchor.app.core.data.dao.TaskDefinitionDao
import com.anchor.app.core.data.entity.AppSettingsEntity
import com.anchor.app.core.data.entity.RescheduleEventEntity
import com.anchor.app.core.data.entity.SubtaskEntity
import com.anchor.app.core.data.entity.TaskDefinitionEntity
import com.anchor.app.core.data.entity.TaskEntity

/**
 * Anchor's Room database — the single source of truth (docs 02-architecture.md §0).
 *
 * Schema versioning (docs 03-database-schema.md §9):
 *  - v1: `app_settings` (Sprint 0)
 *  - v2: + `task_definition`, `task_instance`, `subtask`, `reschedule_event` (Sprint 1)
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
    ],
    version = 2,
    exportSchema = true,
)
abstract class AnchorDatabase : RoomDatabase() {
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun taskDefinitionDao(): TaskDefinitionDao
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun rescheduleEventDao(): RescheduleEventDao

    companion object {
        const val NAME = "anchor.db"

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
    }
}
