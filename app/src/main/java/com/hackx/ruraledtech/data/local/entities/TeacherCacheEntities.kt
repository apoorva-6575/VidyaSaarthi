package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity

/**
 * Last-known snapshot of GET /teacher/dashboard, so the teacher's summary numbers remain
 * visible offline instead of the screen just failing (explicit requirement: "dashboard
 * should show cached data offline when available").
 */
@Entity(tableName = "teacher_dashboard_cache", primaryKeys = ["teacherId"])
data class TeacherDashboardCacheEntity(
    val teacherId: String,
    val classesCount: Int,
    val learnersCount: Int,
    val conceptAveragesJson: String,
    val cachedAt: Long,
)

/** Same idea as [TeacherDashboardCacheEntity] but for one class's GET .../analytics response. */
@Entity(tableName = "class_analytics_cache", primaryKeys = ["classId"])
data class ClassAnalyticsCacheEntity(
    val classId: String,
    val generatedAt: String,
    val classAveragesJson: String,
    val learnerMetricsJson: String,
    val cachedAt: Long,
)
