package com.hackx.ruraledtech.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hackx.ruraledtech.data.local.entities.ClassAnalyticsCacheEntity
import com.hackx.ruraledtech.data.local.entities.TeacherDashboardCacheEntity

@Dao
interface TeacherCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDashboard(entity: TeacherDashboardCacheEntity)

    @Query("SELECT * FROM teacher_dashboard_cache WHERE teacherId = :teacherId")
    suspend fun getDashboard(teacherId: String): TeacherDashboardCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClassAnalytics(entity: ClassAnalyticsCacheEntity)

    @Query("SELECT * FROM class_analytics_cache WHERE classId = :classId")
    suspend fun getClassAnalytics(classId: String): ClassAnalyticsCacheEntity?
}
