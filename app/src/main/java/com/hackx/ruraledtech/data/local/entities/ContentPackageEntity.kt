package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity
import androidx.room.TypeConverters
import com.hackx.ruraledtech.data.local.database.Converters

@Entity(tableName = "content_packages", primaryKeys = ["packageId", "version"])
@TypeConverters(Converters::class)
data class ContentPackageEntity(
    val packageId: String,
    val version: Int,
    val subject: String,
    val grade: Int,
    val languages: List<String>,
    val sizeBytes: Long,
    val checksum: String,
    val installedAt: Long?,
    val state: String,
    val priority: String,
)
