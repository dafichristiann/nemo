package com.anchor.app.core.di

import com.anchor.app.core.domain.blocking.BlockingStrategy
import com.anchor.app.platform.blocking.GentleBlockingStrategy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the focus [BlockingStrategy]. Phase 3 ships Gentle (no blocking) first; a real
 * Accessibility-backed strategy replaces this binding when the feature-flagged spike lands
 * (Spike B / docs 13), without changing the domain contract.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BlockingModule {

    @Binds
    @Singleton
    abstract fun bindBlockingStrategy(impl: GentleBlockingStrategy): BlockingStrategy
}
