package com.anchor.app.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Injectable coroutine dispatchers so tests can substitute a [TestDispatcher]
 * (docs/superpowers/design/11-testing-strategy.md §5).
 */
interface DispatcherProvider {
    /** Main/UI dispatcher. */
    val main: CoroutineDispatcher

    /** Default dispatcher for CPU-bound work. */
    val default: CoroutineDispatcher

    /** IO dispatcher for disk/database work. */
    val io: CoroutineDispatcher
}

/** Production implementation backed by Kotlin coroutines' default dispatchers. */
class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val io: CoroutineDispatcher = Dispatchers.IO
}
