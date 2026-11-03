package com.anchor.app.testutil

import com.anchor.app.core.domain.model.FocusSession
import com.anchor.app.core.domain.model.FocusStatus
import com.anchor.app.core.domain.repository.FocusRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [FocusRepository] fake (docs 11-testing-strategy.md §5). Mirrors the "current"
 * query (newest ACTIVE/PAUSED row) and duration summation used by the focus use cases.
 */
class FakeFocusRepository : FocusRepository {
    val sessions = MutableStateFlow<List<FocusSession>>(emptyList())

    override suspend fun insert(session: FocusSession) {
        sessions.value = sessions.value.filterNot { it.id == session.id } + session
    }

    override suspend fun getById(id: String): FocusSession? = sessions.value.find { it.id == id }

    override suspend fun current(): FocusSession? =
        sessions.value
            .filter { it.status == FocusStatus.ACTIVE || it.status == FocusStatus.PAUSED }
            .maxByOrNull { it.startedAt }

    override fun observeCurrent(): Flow<FocusSession?> =
        sessions.map { list ->
            list.filter { it.status == FocusStatus.ACTIVE || it.status == FocusStatus.PAUSED }
                .maxByOrNull { it.startedAt }
        }

    override fun observeAll(): Flow<List<FocusSession>> =
        sessions.map { list -> list.sortedByDescending { it.startedAt } }

    override suspend fun totalDurationBetween(from: Long, to: Long): Long =
        sessions.value.filter { it.startedAt >= from && it.startedAt < to }.sumOf { it.actualDurationMs }

    override suspend fun updateState(
        id: String,
        status: FocusStatus,
        actualDurationMs: Long,
        endedAt: Long?,
        fallbackReason: String?,
    ) {
        sessions.value = sessions.value.map {
            if (it.id == id) {
                it.copy(
                    status = status,
                    actualDurationMs = actualDurationMs,
                    endedAt = endedAt,
                    fallbackReason = fallbackReason,
                )
            } else {
                it
            }
        }
    }

    override suspend fun updateRating(id: String, rating: Int) {
        sessions.value = sessions.value.map {
            if (it.id == id) it.copy(rating = rating) else it
        }
    }

    override suspend fun getRatedSessionsSince(since: Long): List<FocusSession> =
        sessions.value.filter { it.rating != null && it.startedAt >= since }.sortedBy { it.startedAt }

    override suspend fun clear() {
        sessions.value = emptyList()
    }
}

/** Scriptable [com.anchor.app.core.domain.blocking.BlockingStrategy] fake for tests. */
class FakeBlockingStrategy(
    private var available: Boolean = false,
) : com.anchor.app.core.domain.blocking.BlockingStrategy {
    var engaged: Set<String>? = null
    var engagedMode: com.anchor.app.core.domain.model.FocusMode? = null
    var released = false

    override val effectiveMode: com.anchor.app.core.domain.model.FocusMode
        get() = com.anchor.app.core.domain.model.FocusMode.GENTLE

    fun setAvailable(value: Boolean) { available = value }

    override suspend fun isAvailable(): Boolean = available

    override suspend fun engage(packages: Set<String>): Boolean =
        engage(packages, effectiveMode)

    override suspend fun engage(
        packages: Set<String>,
        mode: com.anchor.app.core.domain.model.FocusMode,
    ): Boolean {
        engaged = packages
        engagedMode = mode
        return available
    }

    override suspend fun release() {
        released = true
    }
}
