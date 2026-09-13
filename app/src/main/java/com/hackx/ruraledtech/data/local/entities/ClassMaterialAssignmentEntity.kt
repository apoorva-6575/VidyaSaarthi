package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "class_material_assignments")
data class ClassMaterialAssignmentEntity(
    @PrimaryKey val assignmentId: String,
    val classId: String,
    val packageId: String,
    val version: Int,
    val teacherId: String,
    val sharedAt: Long
)
