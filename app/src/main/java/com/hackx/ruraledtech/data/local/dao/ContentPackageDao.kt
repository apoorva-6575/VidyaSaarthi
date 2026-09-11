package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackx.ruraledtech.data.local.entities.ContentPackageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentPackageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contentPackage: ContentPackageEntity)

    @Query("SELECT * FROM content_packages WHERE state = 'INSTALLED' ORDER BY packageId")
    fun observeInstalled(): Flow<List<ContentPackageEntity>>

    @Query("SELECT * FROM content_packages WHERE state = 'INSTALLED' ORDER BY packageId")
    suspend fun getInstalled(): List<ContentPackageEntity>

    @Query("SELECT * FROM content_packages WHERE packageId = :packageId ORDER BY version DESC LIMIT 1")
    suspend fun getLatest(packageId: String): ContentPackageEntity?

    @Query("DELETE FROM content_packages WHERE packageId = :packageId")
    suspend fun deletePackage(packageId: String)
}
