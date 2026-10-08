package com.anchor.app.core.di

import com.anchor.app.core.domain.usecase.insight.AiInsightSource
import com.anchor.app.platform.ai.NoOpAiInsightSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the optional [AiInsightSource]. The build ships [NoOpAiInsightSource] (always returns null
 * → rule-based fallback) so nothing ever leaves the device by default (docs 09-privacy §6, FR-AI4).
 * A real Gemini source replaces this binding when the opt-in AI feature is enabled.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InsightModule {

    @Binds
    @Singleton
    abstract fun bindAiInsightSource(impl: NoOpAiInsightSource): AiInsightSource
}
