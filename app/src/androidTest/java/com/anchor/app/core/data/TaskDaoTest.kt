package com.anchor.app.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anchor.app.core.data.dao.TaskDao
import com.anchor.app.core.data.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DAO tests for [TaskDao] on an in-memory Room database (docs 11 §1/§2.1). Verifies the
 * All/Today/Upcoming/Completed/Overdue filters and the recurring idempotency query — the
 * SQL that must match the domain semantics exactly.
 */
@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var db: NemoDatabase
    private lateinit var dao: TaskDao

    private val day = 1_000_000_000L
    private val startOfDay = day
    private val endOfDay = day + 86_400_000L
    private val todayEpochDay = 42L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NemoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.taskDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun task(
        id: String,
        deadline: Long? = null,
        status: String = "TODO",
        priority: String = "MEDIUM",
        createdAt: Long = day,
        completedAt: Long? = null,
        definitionId: String? = null,
        occurrenceDate: Long? = null,
    ) = TaskEntity(
        id = id,
        title = id,
        deadline = deadline,
        status = status,
        priority = priority,
        createdAt = createdAt,
        completedAt = completedAt,
        definitionId = definitionId,
        definitionOccurrenceDate = occurrenceDate,
    )

    @Test
    fun observeAll_excludesArchived_andPutsDoneLast() = runTest {
        dao.insert(task("open", deadline = startOfDay + 10))
        dao.insert(task("done", status = "DONE", completedAt = day + 5))
        dao.insert(task("archived", status = "ARCHIVED"))

        val ids = dao.observeAll().first().map { it.id }
        assertTrue("archived hidden", "archived" !in ids)
        assertEquals("done last", "done", ids.last())
        assertEquals(setOf("open", "done"), ids.toSet())
    }

    @Test
    fun observeToday_includesDeadlineInWindow_andOpenOnly() = runTest {
        dao.insert(task("in-day", deadline = startOfDay + 3_600_000))
        dao.insert(task("at-start", deadline = startOfDay))
        dao.insert(task("at-end-exclusive", deadline = endOfDay))
        dao.insert(task("done-in-day", deadline = startOfDay + 100, status = "DONE"))
        dao.insert(task("before", deadline = startOfDay - 1))

        val ids = dao.observeToday(startOfDay, endOfDay, todayEpochDay).first().map { it.id }.toSet()
        assertEquals(setOf("in-day", "at-start"), ids)
    }

    @Test
    fun observeToday_includesRecurringInstanceWithoutDeadline() = runTest {
        // F2: a recurring task with no deadline ("Morning run", DAILY) must still show Today
        // when its occurrence date is today.
        dao.insert(task("recur-today", occurrenceDate = todayEpochDay))
        dao.insert(task("recur-other-day", occurrenceDate = todayEpochDay - 1))

        val ids = dao.observeToday(startOfDay, endOfDay, todayEpochDay).first().map { it.id }.toSet()
        assertEquals(setOf("recur-today"), ids)
    }

    @Test
    fun observeUpcoming_includesEndOfDayOnward_openOnly() = runTest {
        dao.insert(task("today", deadline = startOfDay + 100))
        dao.insert(task("tomorrow", deadline = endOfDay))
        dao.insert(task("done-future", deadline = endOfDay + 100, status = "DONE"))

        val ids = dao.observeUpcoming(endOfDay).first().map { it.id }.toSet()
        assertEquals(setOf("tomorrow"), ids)
    }

    @Test
    fun observeCompleted_ordersByCompletedAtDesc() = runTest {
        dao.insert(task("older", status = "DONE", completedAt = day + 1))
        dao.insert(task("newer", status = "DONE", completedAt = day + 9))

        val ids = dao.observeCompleted().first().map { it.id }
        assertEquals(listOf("newer", "older"), ids)
    }

    @Test
    fun observeOverdue_strictlyBeforeNow_openOnly() = runTest {
        dao.insert(task("late", deadline = day - 1))
        dao.insert(task("now", deadline = day))
        dao.insert(task("done-late", deadline = day - 1, status = "DONE"))

        val ids = dao.observeOverdue(day).first().map { it.id }.toSet()
        assertEquals(setOf("late"), ids)
    }

    @Test
    fun counts_trackOpenAndCompleted() = runTest {
        dao.insert(task("a"))
        dao.insert(task("b", status = "IN_PROGRESS"))
        dao.insert(task("c", status = "DONE", completedAt = day))

        assertEquals(2, dao.observeOpenCount().first())
        assertEquals(1, dao.observeCompletedCount().first())
    }

    @Test
    fun countForDefinitionOnDate_isIdempotencyGuard() = runTest {
        // task_instance.definitionId is a FK to task_definition, so the template must exist.
        db.taskDefinitionDao().insert(
            com.anchor.app.core.data.entity.TaskDefinitionEntity(
                id = "def",
                title = "Daily",
                recurrenceRule = "DAILY",
                createdAt = day,
            ),
        )
        dao.insert(task("d1", definitionId = "def", occurrenceDate = 42))
        dao.insert(task("d3", definitionId = "def", occurrenceDate = 43))

        // At most one instance per (definitionId, occurrenceDate) — the unique index enforces it.
        assertEquals(1, dao.countForDefinitionOnDate("def", 42))
        assertEquals(1, dao.countForDefinitionOnDate("def", 43))
        assertEquals(0, dao.countForDefinitionOnDate("other", 42))
    }

    @Test
    fun definitionDelete_keepsTaskWithNulledLink() = runTest {
        // SET_NULL: removing a template must not destroy instance history.
        db.taskDefinitionDao().insert(
            com.anchor.app.core.data.entity.TaskDefinitionEntity(
                id = "def",
                title = "Daily",
                recurrenceRule = "DAILY",
                createdAt = day,
            ),
        )
        dao.insert(task("d1", definitionId = "def", occurrenceDate = 42))

        db.taskDefinitionDao().clear()

        val kept = dao.observeById("d1").first()
        assertTrue(kept != null)
        assertEquals(null, kept!!.definitionId)
    }

    @Test
    fun observeAll_ordersBySemanticPriority_notAlphabetical() = runTest {
        // M7 regression: priority is stored as TEXT; ORDER BY priority DESC would sort
        // alphabetically (URGENT > MEDIUM > LOW > HIGH), burying HIGH below LOW. The query must
        // order by semantic rank instead: URGENT, HIGH, MEDIUM, LOW.
        dao.insert(task("low", priority = "LOW", createdAt = day + 1))
        dao.insert(task("high", priority = "HIGH", createdAt = day + 2))
        dao.insert(task("urgent", priority = "URGENT", createdAt = day + 3))
        dao.insert(task("medium", priority = "MEDIUM", createdAt = day + 4))

        val ids = dao.observeAll().first().map { it.id }
        assertEquals(listOf("urgent", "high", "medium", "low"), ids)
    }

    @Test
    fun observeToday_ordersBySemanticPriority() = runTest {
        dao.insert(task("low", priority = "LOW", deadline = startOfDay + 10))
        dao.insert(task("high", priority = "HIGH", deadline = startOfDay + 20))

        val ids = dao.observeToday(startOfDay, endOfDay, todayEpochDay).first().map { it.id }
        assertEquals(listOf("high", "low"), ids)
    }

    @Test
    fun recurringInsert_ignoresDuplicateOccurrenceAtomically() = runTest {
        // C1 regression: the unique index + IGNORE keeps a single instance per
        // (definitionId, definitionOccurrenceDate), making the guard atomic.
        db.taskDefinitionDao().insert(
            com.anchor.app.core.data.entity.TaskDefinitionEntity(
                id = "def",
                title = "Daily",
                recurrenceRule = "DAILY",
                createdAt = day,
            ),
        )
        val first = dao.insertIgnoringDuplicate(task("i1", definitionId = "def", occurrenceDate = 42))
        val second = dao.insertIgnoringDuplicate(task("i2", definitionId = "def", occurrenceDate = 42))

        assertTrue("first insert wins", first != -1L)
        assertEquals("duplicate ignored", -1L, second)
        assertEquals(1, dao.countForDefinitionOnDate("def", 42))
    }

    @Test
    fun deleteById_removesOnlyThatRow() = runTest {
        dao.insert(task("keep"))
        dao.insert(task("drop"))

        dao.deleteById("drop")

        val ids = dao.observeAll().first().map { it.id }.toSet()
        assertEquals(setOf("keep"), ids)
    }

    @Test
    fun completionSource_roundTrips_andDefaultsNull() = runTest {
        // PRD v0.5 §10.1: verified completions persist their source; unfinished tasks stay null.
        dao.insert(task("manual", status = "DONE", completedAt = day).copy(completionSource = "MANUAL"))
        dao.insert(task("verified", status = "DONE", completedAt = day).copy(completionSource = "VERIFIED"))
        dao.insert(task("open"))

        assertEquals("MANUAL", dao.observeById("manual").first()?.completionSource)
        assertEquals("VERIFIED", dao.observeById("verified").first()?.completionSource)
        assertEquals(null, dao.observeById("open").first()?.completionSource)
    }
}
