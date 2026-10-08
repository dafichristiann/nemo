package com.anchor.app.core.di

import com.anchor.app.BuildConfig
import com.anchor.app.core.domain.blocking.BlockingStrategy
import com.anchor.app.platform.blocking.AccessibilityBlockingStrategy
import com.anchor.app.platform.blocking.BlockingGate
import com.anchor.app.platform.blocking.GentleBlockingStrategy
import com.anchor.app.platform.blocking.ProductionBlockingGate
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the focus [BlockingStrategy] (docs 04 §2.5, Spike B / docs 13, PRD v0.4 §10.5).
 *
 * With the build flag ON we select the real [AccessibilityBlockingStrategy]; with it OFF the app
 * keeps [GentleBlockingStrategy] (no-op) so nothing can block. Either way the domain contract is
 * unchanged and [com.anchor.app.core.domain.usecase.focus.StartFocusUseCase] still degrades to
 * Gentle whenever the strategy reports unavailable (FR-F8).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BlockingModule {

    @Binds
    @Singleton
    abstract fun bindBlockingGate(impl: ProductionBlockingGate): BlockingGate

    companion object {

        @Provides
        @Singleton
        fun provideBlockingStrategy(
            gentle: GentleBlockingStrategy,
            strict: AccessibilityBlockingStrategy,
        ): BlockingStrategy =
            if (BuildConfig.FOCUS_BLOCKING_ENABLED) strict else gentle
    }
}
