package com.anchor.app.core.di

import com.anchor.app.core.domain.usecase.insight.AiInsightSource
import com.anchor.app.platform.ai.GeminiInsightSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the optional [AiInsightSource] (FR-AI2, PRD v0.3 §10.8/§13.1).
 *
 * The shipped build uses [GeminiInsightSource] (real AI). It degrades to a no-op by itself:
 * when `BuildConfig.GEMINI_API_KEY` is blank (or the request fails), it returns null and the
 * generator falls back to the rule-based source — so behaviour without a key is identical to
 * the previous `NoOpAiInsightSource`, and nothing leaves the device.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InsightModule {

    @Binds
    @Singleton
    abstract fun bindAiInsightSource(impl: GeminiInsightSource): AiInsightSource
}
