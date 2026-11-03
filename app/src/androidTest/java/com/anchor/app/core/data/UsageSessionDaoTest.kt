package com.anchor.app.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anchor.app.core.data.dao.UsageSessionDao
import com.anchor.app.core.data.entity.UsageSessionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DAO tests for [UsageSessionDao] on an in-memory Room database (docs 11 §1/§2.4). Verifies
 * the overlap range query, idempotent (IGNORE) insert, and the retention pruning — all of
 * which the usage pipeline depends on.
 */
@RunWith(AndroidJUnit4::class)
class UsageSessionDaoTest {

    private lateinit var db: NemoDatabase
    private lateinit var dao: UsageSessionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NemoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.usageSessionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun session(
        id: String,
        pkg: String = "com.app",
        start: Long,
        end: Long,
    ) = UsageSessionEntity(
        id = id,
        packageName = pkg,
        startTime = start,
        endTime = end,
        durationMs = end - start,
        collectedAt = 0L,
    )

    @Test
    fun getBetween_returnsOverlappingSessions() = runTest {
        // Session crossing midnight belongs to both days (docs 03 §3.2).
        dao.insertAll(
            listOf(
                session("before", start = 0, end = 90),
                session("overlap-left", start = 50, end = 150),
                session("inside", start = 120, end = 180),
                session("after", start = 500, end = 600),
            ),
        )

        val ids = dao.getBetween(100, 200).map { it.id }.toSet()
        assertEquals(setOf("overlap-left", "inside"), ids)
    }

    @Test
    fun getBetween_boundariesAreExclusiveOnBothSides() = runTest {
        dao.insertAll(
            listOf(
                session("ends-at-from", start = 0, end = 100), // end == from → excluded
                session("starts-at-to", start = 200, end = 300), // start == to → excluded
                session("inside", start = 120, end = 180),
            ),
        )

        val ids = dao.getBetween(100, 200).map { it.id }.toSet()
        assertEquals(setOf("inside"), ids)
    }

    @Test
    fun insertAll_isIdempotentOnDuplicateId() = runTest {
        val s = session("dup", start = 10, end = 20)
        dao.insertAll(listOf(s))
        dao.insertAll(listOf(s.copy(durationMs = 999))) // conflicting id ignored

        assertEquals(1, dao.count())
        // Original row is preserved (IGNORE keeps the first insert).
        assertEquals(10L, dao.getBetween(0, 100).single().durationMs)
    }

    @Test
    fun earliestAndLatestStart_trackRange() = runTest {
        assertNull(dao.earliestStartTime())
        assertNull(dao.latestStartTime())

        dao.insertAll(
            listOf(
                session("a", start = 100, end = 200),
                session("b", start = 500, end = 600),
                session("c", start = 300, end = 400),
            ),
        )

        assertEquals(100L, dao.earliestStartTime())
        assertEquals(500L, dao.latestStartTime())
    }

    @Test
    fun latestEndTime_tracksFurthestCollectedInstant_notMaxStart() = runTest {
        // The collection watermark must be MAX(endTime): a long session that started early but ends
        // latest must set the watermark, or apps used after it would be re-queried / truncated.
        assertNull(dao.latestEndTime())

        dao.insertAll(
            listOf(
                session("long-early-start", start = 100, end = 900),
                session("short-late-start", start = 500, end = 600),
            ),
        )

        assertEquals(900L, dao.latestEndTime())
        assertEquals(500L, dao.latestStartTime())
    }

    @Test
    fun deleteOlderThan_prunesByStartTime_andReturnsCount() = runTest {
        dao.insertAll(
            listOf(
                session("old-1", start = 10, end = 20),
                session("old-2", start = 50, end = 60),
                session("keep-1", start = 100, end = 110),
                session("keep-2", start = 200, end = 210),
            ),
        )

        val deleted = dao.deleteOlderThan(100)
        assertEquals(2, deleted)
        assertEquals(2, dao.count())
        assertEquals(setOf("keep-1", "keep-2"), dao.getBetween(0, 1000).map { it.id }.toSet())
    }
}
