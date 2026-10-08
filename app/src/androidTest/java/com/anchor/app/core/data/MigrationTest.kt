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
}
