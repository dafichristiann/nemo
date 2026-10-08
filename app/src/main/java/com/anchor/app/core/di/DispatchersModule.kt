package com.anchor.app.core.di

import com.anchor.app.core.common.DefaultDispatcherProvider
import com.anchor.app.core.common.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the injectable [DispatcherProvider] (docs 11-testing-strategy.md §5). */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
}
