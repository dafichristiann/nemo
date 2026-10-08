package com.anchor.app.core.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.anchor.app.core.data.entity.AppCategoryEntity
import kotlinx.coroutines.flow.Flow

/** DAO for app category config (docs 03-database-schema.md §3.4). */
@Dao
interface AppCategoryDao {

    @Upsert
    suspend fun upsert(category: AppCategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<AppCategoryEntity>)

    @Query("SELECT * FROM app_category WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): AppCategoryEntity?

    @Query("SELECT * FROM app_category")
    suspend fun getAll(): List<AppCategoryEntity>

    @Query("SELECT * FROM app_category")
    fun observeAll(): Flow<List<AppCategoryEntity>>

    @Query("DELETE FROM app_category")
    suspend fun clear()
}
