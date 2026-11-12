package com.anchor.app.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anchor.app.core.data.NemoDatabase
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
    fun provideNemoDatabase(
        @ApplicationContext context: Context,
    ): NemoDatabase = Room.databaseBuilder(
        context,
        NemoDatabase::class.java,
        NemoDatabase.NAME,
    )
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .addMigrations(*NemoDatabase.ALL_MIGRATIONS)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.setForeignKeyConstraintsEnabled(true)
            }
        })
        .build()

    @Provides
    fun provideAppSettingsDao(database: NemoDatabase): AppSettingsDao =
        database.appSettingsDao()

    @Provides
    fun provideTaskDao(database: NemoDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTaskDefinitionDao(database: NemoDatabase): TaskDefinitionDao =
        database.taskDefinitionDao()

    @Provides
    fun provideSubtaskDao(database: NemoDatabase): SubtaskDao = database.subtaskDao()

    @Provides
    fun provideRescheduleEventDao(database: NemoDatabase): RescheduleEventDao =
        database.rescheduleEventDao()

    @Provides
    fun provideUsageSessionDao(database: NemoDatabase): UsageSessionDao =
        database.usageSessionDao()

    @Provides
    fun provideDailyUsageSummaryDao(database: NemoDatabase): DailyUsageSummaryDao =
        database.dailyUsageSummaryDao()

    @Provides
    fun provideWeeklyUsageSummaryDao(database: NemoDatabase): WeeklyUsageSummaryDao =
        database.weeklyUsageSummaryDao()

    @Provides
    fun provideAppCategoryDao(database: NemoDatabase): AppCategoryDao =
        database.appCategoryDao()

    @Provides
    fun provideInterventionLogDao(database: NemoDatabase): InterventionLogDao =
        database.interventionLogDao()

    @Provides
    fun provideFocusSessionDao(database: NemoDatabase): FocusSessionDao =
        database.focusSessionDao()

    @Provides
    fun provideTaskDependencyDao(database: NemoDatabase): TaskDependencyDao =
        database.taskDependencyDao()

    @Provides
    fun provideInsightDao(database: NemoDatabase): InsightDao = database.insightDao()
}
