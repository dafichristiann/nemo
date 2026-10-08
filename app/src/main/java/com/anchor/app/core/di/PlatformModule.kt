package com.anchor.app.core.di

import android.content.Context
import androidx.work.WorkManager
import com.anchor.app.platform.usagestats.AndroidUsageStatsSource
import com.anchor.app.platform.usagestats.UsageStatsSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Platform-API bindings (docs 02-architecture.md §2, §6). The domain depends on the
 * [UsageStatsSource] interface; the concrete Android implementation is bound here so it can
 * be swapped for a fake in tests.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {

    @Binds
    @Singleton
    abstract fun bindUsageStatsSource(impl: AndroidUsageStatsSource): UsageStatsSource

    companion object {
        @Provides
        @Singleton
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
            WorkManager.getInstance(context)
    }
}
