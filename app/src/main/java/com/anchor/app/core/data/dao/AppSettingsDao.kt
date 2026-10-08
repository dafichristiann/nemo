package com.anchor.app.core.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.anchor.app.core.data.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

/** DAO for the single-row [AppSettingsEntity] (docs 03-database-schema.md §7). */
@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = ${AppSettingsEntity.SINGLETON_ID}")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = ${AppSettingsEntity.SINGLETON_ID}")
    suspend fun get(): AppSettingsEntity?

    @Upsert
    suspend fun upsert(settings: AppSettingsEntity)

    @Query("DELETE FROM app_settings")
    suspend fun clear()
}
