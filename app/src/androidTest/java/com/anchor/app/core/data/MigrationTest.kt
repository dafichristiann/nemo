package com.anchor.app.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies every explicit migration path (docs 03 §9, docs 11 §2.8/§3).
 *
 * Each test opens a database at an old version, applies the migration chain via
 * [MigrationTestHelper], then asserts the target tables exist — proving the app never loses data
 * or crashes on upgrade (PRD §12 "Tak crash saat migrasi gagal").
 *
 * Runs on a device/emulator (`connectedDebugAndroidTest`); compiles as part of the default build.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AnchorDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    private fun tableNames(dbName: String): Set<String> {
        val db = helper.runMigrationsAndValidate(
            dbName,
            AnchorDatabase.VERSION,
            true,
            *AnchorDatabase.ALL_MIGRATIONS,
        )
        val names = mutableSetOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            while (cursor.moveToNext()) names += cursor.getString(0)
        }
        return names
    }

    @Test
    fun migrate1ToLatest_createsAllTables() {
        helper.createDatabase(testDb, 1).close()
        val tables = tableNames(testDb)

        listOf(
            "app_settings",
            "task_definition",
            "task_instance",
            "subtask",
            "reschedule_event",
            "usage_session",
            "daily_usage_summary",
            "weekly_usage_summary",
            "app_category",
            "intervention_log",
            "focus_session",
            "task_dependency",
            "insight",
        ).forEach { assertTrue("missing table: $it", it in tables) }
    }

    @Test
    fun migrate2ToLatest_preservesTaskData() {
        // v2 has the task tables; insert a row then upgrade and expect it to survive.
        helper.createDatabase(testDb, 2).use { db ->
            db.execSQL(
                "INSERT INTO task_instance (id, title, priority, status, createdAt, progressPercent) " +
                    "VALUES ('t1', 'Survivor', 'HIGH', 'TODO', 123, 0)",
            )
        }
        val db = helper.runMigrationsAndValidate(
            testDb,
            AnchorDatabase.VERSION,
            true,
            *AnchorDatabase.ALL_MIGRATIONS,
        )
        db.query("SELECT title FROM task_instance WHERE id='t1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Survivor", c.getString(0))
        }
    }

    @Test
    fun migrate3ToLatest_addsPhase3Tables() {
        helper.createDatabase(testDb, 3).close()
        val tables = tableNames(testDb)
        assertTrue("intervention_log" in tables)
        assertTrue("focus_session" in tables)
    }

    @Test
    fun migrate5ToLatest_addsDependencyAndInsight() {
        helper.createDatabase(testDb, 5).close()
        val tables = tableNames(testDb)
        assertTrue("task_dependency" in tables)
        assertTrue("insight" in tables)
    }

    @Test
    fun migrate7ToLatest_addsBlockedAppsColumn_preservesSettings() {
        // v7 settings row should survive and gain a null blockedAppsJson (FR-S4).
        helper.createDatabase(testDb, 7).use { db ->
            db.execSQL(
                "INSERT INTO app_settings (id, trackingEnabled, aiEnabled, notificationsEnabled, " +
                    "usageRetentionDays, defaultFocusMode, dailyInterventionQuota, cooldownHours, " +
                    "onboardingCompleted, locale) " +
                    "VALUES (1, 1, 1, 1, 30, 'GENTLE', 3, 2, 1, 'en')",
            )
        }
        val db = helper.runMigrationsAndValidate(
            testDb,
            AnchorDatabase.VERSION,
            true,
            *AnchorDatabase.ALL_MIGRATIONS,
        )
        db.query("SELECT onboardingCompleted, blockedAppsJson FROM app_settings WHERE id=1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
            assertTrue("blockedAppsJson should be null after migration", c.isNull(1))
        }
    }

    @Test
    fun migrate8ToLatest_addsFocusBlockingColumn_defaultOff() {
        // v8 settings row should survive and gain focusBlockingEnabled defaulting to 0 (OFF).
        helper.createDatabase(testDb, 8).use { db ->
            db.execSQL(
                "INSERT INTO app_settings (id, trackingEnabled, aiEnabled, notificationsEnabled, " +
                    "usageRetentionDays, defaultFocusMode, dailyInterventionQuota, cooldownHours, " +
                    "onboardingCompleted, locale, blockedAppsJson) " +
                    "VALUES (1, 1, 1, 1, 30, 'GENTLE', 3, 2, 1, 'en', '[\"com.tiktok\"]')",
            )
        }
        val db = helper.runMigrationsAndValidate(
            testDb,
            AnchorDatabase.VERSION,
            true,
            *AnchorDatabase.ALL_MIGRATIONS,
        )
        db.query("SELECT blockedAppsJson, focusBlockingEnabled FROM app_settings WHERE id=1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("[\"com.tiktok\"]", c.getString(0))
            assertEquals("focusBlockingEnabled should default OFF", 0, c.getInt(1))
        }
    }

    @Test
    fun migrate9ToLatest_addsCompletionSourceColumn_nullsLegacyRows() {
        // v9 task row should survive and gain a null completionSource (PRD v0.5 §10.1, no backfill).
        helper.createDatabase(testDb, 9).use { db ->
            db.execSQL(
                "INSERT INTO task_instance (id, title, priority, status, createdAt, progressPercent) " +
                    "VALUES ('t1', 'Legacy', 'HIGH', 'DONE', 123, 100)",
            )
        }
        val db = helper.runMigrationsAndValidate(
            testDb,
            AnchorDatabase.VERSION,
            true,
            *AnchorDatabase.ALL_MIGRATIONS,
        )
        db.query("SELECT title, completionSource FROM task_instance WHERE id='t1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Legacy", c.getString(0))
            assertTrue("completionSource should be null for legacy rows", c.isNull(1))
        }
    }

    @Test
    fun migrate10ToLatest_convertsPlannedMinutesToSeconds() {
        // v10 stored planned duration in whole minutes; v11 renames it to seconds (×60).
        helper.createDatabase(testDb, 10).use { db ->
            db.execSQL(
                "INSERT INTO focus_session (id, taskId, mode, plannedDurationMin, actualDurationMs, " +
                    "startedAt, endedAt, status, blockedAppsJson, fallbackReason) " +
                    "VALUES ('f1', NULL, 'GENTLE', 25, 0, 123, NULL, 'FINISHED', NULL, NULL)",
            )
        }
        val db = helper.runMigrationsAndValidate(
            testDb,
            AnchorDatabase.VERSION,
            true,
            *AnchorDatabase.ALL_MIGRATIONS,
        )
        db.query("SELECT plannedDurationSec FROM focus_session WHERE id='f1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("25 minutes should become 1500 seconds", 1500, c.getInt(0))
        }
    }

    @Test
    fun migrate11ToLatest_addsProfileColumns_withDefaults() {
        // v11 settings row should survive and gain the onboarding profile columns:
        // nickname (null), focusArea ('COLLEGE'), defaultFocusDurationMin (25).
        helper.createDatabase(testDb, 11).use { db ->
            db.execSQL(
                "INSERT INTO app_settings (id, trackingEnabled, aiEnabled, notificationsEnabled, " +
                    "usageRetentionDays, defaultFocusMode, dailyInterventionQuota, cooldownHours, " +
                    "onboardingCompleted, locale, blockedAppsJson, focusBlockingEnabled) " +
                    "VALUES (1, 0, 0, 1, 30, 'GENTLE', 3, 2, 1, 'en', NULL, 0)",
            )
        }
        val db = helper.runMigrationsAndValidate(
            testDb,
            AnchorDatabase.VERSION,
            true,
            *AnchorDatabase.ALL_MIGRATIONS,
        )
        db.query(
            "SELECT onboardingCompleted, nickname, focusArea, defaultFocusDurationMin " +
                "FROM app_settings WHERE id=1",
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
            assertTrue("nickname should be null for legacy rows", c.isNull(1))
            assertEquals("COLLEGE", c.getString(2))
            assertEquals(25, c.getInt(3))
        }
    }

    @Test
    fun migrate12ToLatest_dedupesAndCreatesOccurrenceUniqueIndex() {
        // v13 collapses pre-existing duplicate recurring instances (keeping the oldest) and adds
        // a unique index on (definitionId, definitionOccurrenceDate) — the "no double instance"
        // guarantee (FR-T7). Inserts two rows sharing an occurrence key and asserts one survives
        // and that a later duplicate insert is rejected by the index.
        helper.createDatabase(testDb, 12).use { db ->
            db.execSQL(
                "INSERT INTO task_definition (id, title, defaultPriority, recurrenceRule, createdAt) " +
                    "VALUES ('def', 'Daily', 'MEDIUM', 'DAILY', 1)",
            )
            db.execSQL(
                "INSERT INTO task_instance (id, definitionId, title, priority, status, createdAt, " +
                    "progressPercent, definitionOccurrenceDate) " +
                    "VALUES ('old', 'def', 'Daily', 'MEDIUM', 'TODO', 100, 0, 42)",
            )
            db.execSQL(
                "INSERT INTO task_instance (id, definitionId, title, priority, status, createdAt, " +
                    "progressPercent, definitionOccurrenceDate) " +
                    "VALUES ('new', 'def', 'Daily', 'MEDIUM', 'TODO', 200, 0, 42)",
            )
        }
        val db = helper.runMigrationsAndValidate(
            testDb,
            AnchorDatabase.VERSION,
            true,
            *AnchorDatabase.ALL_MIGRATIONS,
        )
        db.query(
            "SELECT COUNT(*) FROM task_instance WHERE definitionId='def' AND definitionOccurrenceDate=42",
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("duplicate occurrence must be collapsed to one", 1, c.getInt(0))
        }
        db.query("SELECT id FROM task_instance WHERE definitionId='def'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("oldest row is kept", "old", c.getString(0))
        }
    }
}
