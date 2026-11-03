package com.anchor.app.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anchor.app.core.data.dao.InterventionLogDao
import com.anchor.app.core.data.entity.InterventionLogEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DAO tests on an in-memory Room database (docs 11 §1/§2.4). Verifies the quota and cooldown
 * queries that underpin the Intervention Engine — the parts that must match the real SQL exactly.
 */
@RunWith(AndroidJUnit4::class)
class InterventionLogDaoTest {

    private lateinit var db: NemoDatabase
    private lateinit var dao: InterventionLogDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NemoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.interventionLogDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun log(
        id: String,
        status: String,
        shownAt: Long,
        cooldown: Boolean = false,
        resolvedAt: Long? = null,
        pkg: String? = "com.tiktok",
    ) = InterventionLogEntity(
        id = id,
        level = 1,
        channel = "NOTIFICATION",
        contextPackage = pkg,
        relatedTaskId = null,
        status = status,
        shownAt = shownAt,
        resolvedAt = resolvedAt,
        cooldownTriggered = cooldown,
    )

    @Test
    fun quotaUsed_excludesExpired() = runTest {
        val day = 1_000_000L
        dao.insert(log("a", "INTERACTED", shownAt = day + 1))
        dao.insert(log("b", "DISMISSED", shownAt = day + 2))
        dao.insert(log("c", "EXPIRED", shownAt = day + 3))

        // Only INTERACTED + DISMISSED count toward quota.
        assertEquals(2, dao.quotaUsedBetween(day, day + 10_000))
    }

    @Test
    fun quotaUsed_respectsDayWindow() = runTest {
        val day = 1_000_000L
        dao.insert(log("a", "DISMISSED", shownAt = day - 1)) // before window
        dao.insert(log("b", "DISMISSED", shownAt = day + 5))
        dao.insert(log("c", "DISMISSED", shownAt = day + 10_000)) // at/after end (exclusive)

        assertEquals(1, dao.quotaUsedBetween(day, day + 10_000))
    }

    @Test
    fun latestCooldown_returnsNewestLaterOnly() = runTest {
        dao.insert(log("a", "DISMISSED", shownAt = 1, cooldown = true, resolvedAt = 100, pkg = "com.x"))
        dao.insert(log("b", "DISMISSED", shownAt = 2, cooldown = true, resolvedAt = 300, pkg = "com.x"))
        dao.insert(log("c", "DISMISSED", shownAt = 3, cooldown = false, resolvedAt = 999, pkg = "com.x"))

        assertEquals(300L, dao.latestCooldownAt("com.x"))
        assertNull(dao.latestCooldownAt("com.other"))
    }

    @Test
    fun countMeaningfulAtOrAbove_countsOnlyInteractedDismissed() = runTest {
        dao.insert(log("a", "INTERACTED", shownAt = 1).copy(level = 3))
        dao.insert(log("b", "DISMISSED", shownAt = 2).copy(level = 4))
        dao.insert(log("c", "EXPIRED", shownAt = 3).copy(level = 4))

        assertEquals(2, dao.countMeaningfulAtOrAbove(3))
        assertEquals(2, dao.countMeaningfulAtOrAbove(1))
    }
}
