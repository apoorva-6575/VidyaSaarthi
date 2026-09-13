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

    /** Every installed package regardless of owner — for device-wide needs like P2P manifest advertising (MeshController) where the device must offer everything it holds, not just one profile's content. */
    @Query("SELECT * FROM content_packages WHERE state = 'INSTALLED' ORDER BY packageId")
    fun observeInstalled(): Flow<List<ContentPackageEntity>>

    @Query("SELECT * FROM content_packages WHERE state = 'INSTALLED' ORDER BY packageId")
    suspend fun getInstalled(): List<ContentPackageEntity>

    /** Learner-scoped: only shared (receivedByLearnerId IS NULL) or personally-received packages — used by student-facing screens so profiles sharing a device don't see each other's material. */
    @Query("SELECT * FROM content_packages WHERE state = 'INSTALLED' AND (receivedByLearnerId IS NULL OR receivedByLearnerId = :learnerId) ORDER BY packageId")
    fun observeInstalledForLearner(learnerId: String): Flow<List<ContentPackageEntity>>

    @Query("SELECT * FROM content_packages WHERE state = 'INSTALLED' AND (receivedByLearnerId IS NULL OR receivedByLearnerId = :learnerId) ORDER BY packageId")
    suspend fun getInstalledForLearner(learnerId: String): List<ContentPackageEntity>

    @Query("SELECT * FROM content_packages WHERE packageId = :packageId ORDER BY version DESC LIMIT 1")
    suspend fun getLatest(packageId: String): ContentPackageEntity?

    @Query("DELETE FROM content_packages WHERE packageId = :packageId")
    suspend fun deletePackage(packageId: String)
}
