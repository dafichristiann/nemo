package com.anchor.app.core.di

import com.anchor.app.core.common.DefaultIdProvider
import com.anchor.app.core.common.DefaultTimeProvider
import com.anchor.app.core.common.IdProvider
import com.anchor.app.core.common.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * App-wide singletons.
 *
 * [TimeProvider] and [IdProvider] are injectable so domain logic never touches the
 * system clock or randomness directly (docs 11-testing-strategy.md §5).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = DefaultTimeProvider()

    @Provides
    @Singleton
    fun provideIdProvider(): IdProvider = DefaultIdProvider()
}
