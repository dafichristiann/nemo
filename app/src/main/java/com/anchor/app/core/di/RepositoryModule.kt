package com.anchor.app.core.di

import com.anchor.app.core.data.repository.FocusRepositoryImpl
import com.anchor.app.core.data.repository.InsightRepositoryImpl
import com.anchor.app.core.data.repository.InterventionRepositoryImpl
import com.anchor.app.core.data.repository.SettingsRepositoryImpl
import com.anchor.app.core.data.repository.TaskDefinitionRepositoryImpl
import com.anchor.app.core.data.repository.TaskDependencyRepositoryImpl
import com.anchor.app.core.data.repository.TaskRepositoryImpl
import com.anchor.app.core.data.repository.UsageRepositoryImpl
import com.anchor.app.core.domain.repository.FocusRepository
import com.anchor.app.core.domain.repository.InsightRepository
import com.anchor.app.core.domain.repository.InterventionRepository
import com.anchor.app.core.domain.repository.SettingsRepository
import com.anchor.app.core.domain.repository.TaskDefinitionRepository
import com.anchor.app.core.domain.repository.TaskDependencyRepository
import com.anchor.app.core.domain.repository.TaskRepository
import com.anchor.app.core.domain.repository.UsageRepository
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

    @Binds
    @Singleton
    abstract fun bindUsageRepository(impl: UsageRepositoryImpl): UsageRepository

    @Binds
    @Singleton
    abstract fun bindInterventionRepository(
        impl: InterventionRepositoryImpl,
    ): InterventionRepository

    @Binds
    @Singleton
    abstract fun bindFocusRepository(impl: FocusRepositoryImpl): FocusRepository

    @Binds
    @Singleton
    abstract fun bindTaskDependencyRepository(
        impl: TaskDependencyRepositoryImpl,
    ): TaskDependencyRepository

    @Binds
    @Singleton
    abstract fun bindInsightRepository(impl: InsightRepositoryImpl): InsightRepository
}
