package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_groups")
data class ClassGroupEntity(
    @PrimaryKey val classId: String,
    val name: String,
    val grade: String?,
    val subject: String?,
    val teacherId: String,
    val lastSynced: Long
)
