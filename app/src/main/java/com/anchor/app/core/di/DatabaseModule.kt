package com.anchor.app.core.di

import android.content.Context
import androidx.room.Room
import com.anchor.app.core.data.AnchorDatabase
import com.anchor.app.core.data.dao.AppCategoryDao
import com.anchor.app.core.data.dao.AppSettingsDao
import com.anchor.app.core.data.dao.DailyUsageSummaryDao
import com.anchor.app.core.data.dao.FocusSessionDao
import com.anchor.app.core.data.dao.InsightDao
import com.anchor.app.core.data.dao.InterventionLogDao
import com.anchor.app.core.data.dao.RescheduleEventDao
import com.anchor.app.core.data.dao.SubtaskDao
import com.anchor.app.core.data.dao.TaskDao
import com.anchor.app.core.data.dao.TaskDefinitionDao
import com.anchor.app.core.data.dao.TaskDependencyDao
import com.anchor.app.core.data.dao.UsageSessionDao
import com.anchor.app.core.data.dao.WeeklyUsageSummaryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Room wiring (docs 02-architecture.md §2, docs 03-database-schema.md).
 *
 * Migrations are registered explicitly; no destructive fallback (docs 03 §9).
 * `exportSchema = true` writes schema JSON to `app/schemas`.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAnchorDatabase(
        @ApplicationContext context: Context,
    ): AnchorDatabase = Room.databaseBuilder(
        context,
        AnchorDatabase::class.java,
        AnchorDatabase.NAME,
    )
        .addMigrations(
            AnchorDatabase.MIGRATION_1_2,
            AnchorDatabase.MIGRATION_2_3,
            AnchorDatabase.MIGRATION_3_4,
            AnchorDatabase.MIGRATION_4_5,
            AnchorDatabase.MIGRATION_5_6,
            AnchorDatabase.MIGRATION_6_7,
        )
        .build()

    @Provides
    fun provideAppSettingsDao(database: AnchorDatabase): AppSettingsDao =
        database.appSettingsDao()

    @Provides
    fun provideTaskDao(database: AnchorDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTaskDefinitionDao(database: AnchorDatabase): TaskDefinitionDao =
        database.taskDefinitionDao()

    @Provides
    fun provideSubtaskDao(database: AnchorDatabase): SubtaskDao = database.subtaskDao()

    @Provides
    fun provideRescheduleEventDao(database: AnchorDatabase): RescheduleEventDao =
        database.rescheduleEventDao()

    @Provides
    fun provideUsageSessionDao(database: AnchorDatabase): UsageSessionDao =
        database.usageSessionDao()

    @Provides
    fun provideDailyUsageSummaryDao(database: AnchorDatabase): DailyUsageSummaryDao =
        database.dailyUsageSummaryDao()

    @Provides
    fun provideWeeklyUsageSummaryDao(database: AnchorDatabase): WeeklyUsageSummaryDao =
        database.weeklyUsageSummaryDao()

    @Provides
    fun provideAppCategoryDao(database: AnchorDatabase): AppCategoryDao =
        database.appCategoryDao()

    @Provides
    fun provideInterventionLogDao(database: AnchorDatabase): InterventionLogDao =
        database.interventionLogDao()

    @Provides
    fun provideFocusSessionDao(database: AnchorDatabase): FocusSessionDao =
        database.focusSessionDao()

    @Provides
    fun provideTaskDependencyDao(database: AnchorDatabase): TaskDependencyDao =
        database.taskDependencyDao()

    @Provides
    fun provideInsightDao(database: AnchorDatabase): InsightDao = database.insightDao()
}
