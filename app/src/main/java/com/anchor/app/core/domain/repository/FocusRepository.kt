package com.anchor.app.core.domain.repository

import com.anchor.app.core.domain.model.FocusSession
import com.anchor.app.core.domain.model.FocusStatus
import kotlinx.coroutines.flow.Flow

/**
 * Focus session repository (docs/superpowers/design/02-architecture.md §3.1). Backs the Focus
 * Setup/Active screens and the foreground service (PRD §10.5).
 */
interface FocusRepository {

    suspend fun insert(session: FocusSession)

    suspend fun getById(id: String): FocusSession?

    /** The in-progress session (ACTIVE/PAUSED) if one exists. */
    suspend fun current(): FocusSession?

    fun observeCurrent(): Flow<FocusSession?>

    fun observeAll(): Flow<List<FocusSession>>

    /** Total focused milliseconds for sessions started in `[from, to)`. */
    suspend fun totalDurationBetween(from: Long, to: Long): Long

    /** Updates lifecycle fields (status / accumulated duration / endedAt / fallback reason / startedAt). */
    suspend fun updateState(
        id: String,
        status: FocusStatus,
        actualDurationMs: Long,
        endedAt: Long?,
        fallbackReason: String?,
        startedAt: Long? = null,
    )

    suspend fun updateRating(id: String, rating: Int)

    suspend fun getRatedSessionsSince(since: Long): List<FocusSession>

    suspend fun clear()
}
