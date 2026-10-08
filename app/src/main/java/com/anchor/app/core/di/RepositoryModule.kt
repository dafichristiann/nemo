package com.anchor.app.core.di

import com.anchor.app.core.data.repository.SettingsRepositoryImpl
import com.anchor.app.core.data.repository.TaskDefinitionRepositoryImpl
import com.anchor.app.core.data.repository.TaskRepositoryImpl
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.core.domain.repository.TaskDefinitionRepository
import com.anchor.app.core.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds domain repository interfaces to their Room-backed implementations
 * (docs 02-architecture.md §2: `feature → domain ← data`).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindTaskDefinitionRepository(
        impl: TaskDefinitionRepositoryImpl,
    ): TaskDefinitionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
