package com.anchor.app.core.domain.repository

import com.anchor.app.core.domain.model.DependencyType
import com.anchor.app.core.domain.model.TaskDependency
import kotlinx.coroutines.flow.Flow

/**
 * Task dependency repository (docs/superpowers/design/02-architecture.md §3.1, PRD §10.7).
 *
 * The implementation must **reject cycles**: adding an edge that would form `A→…→A` returns
 * [AddDependencyResult.Cycle] and persists nothing (PRD §10.7 AC, docs 03 §8).
 */
interface TaskDependencyRepository {

    fun observeAll(): Flow<List<TaskDependency>>

    suspend fun getAll(): List<TaskDependency>

    /** Edges where [taskId] precedes others (its downstream "domino"). */
    suspend fun downstreamOf(taskId: String): List<TaskDependency>

    /**
     * Adds an edge `from → to`, rejecting a cycle. Returns the outcome so the UI can explain
     * why a cycle was refused (PRD §10.7).
     */
    suspend fun add(
        fromTaskId: String,
        toTaskId: String,
        type: DependencyType = DependencyType.BLOCKS,
    ): AddDependencyResult

    suspend fun remove(id: String)

    suspend fun clear()

    /** Outcome of [add]; [Cycle] carries the path that would have closed the loop. */
    sealed interface AddDependencyResult {
        data class Added(val dependency: TaskDependency) : AddDependencyResult

        /** Rejected: would create a cycle. [path] is from `to` back to `from` for explanation. */
        data class Cycle(val path: List<String>) : AddDependencyResult
    }
}
