package com.anchor.app.core.di

import com.anchor.app.platform.blocking.BlockingState
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry point for components that are **not** Hilt-injectable base classes — notably
 * [com.anchor.app.platform.accessibility.NemoAccessibilityService], which extends
 * `AccessibilityService` (unsupported by `@AndroidEntryPoint`). The service reaches
 * [BlockingState] through this seam instead.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BlockingEntryPoint {
    fun blockingState(): BlockingState
}
