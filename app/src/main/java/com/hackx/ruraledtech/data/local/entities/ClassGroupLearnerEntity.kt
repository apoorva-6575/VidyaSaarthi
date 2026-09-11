package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity

@Entity(tableName = "class_group_learners", primaryKeys = ["classId", "learnerId"])
data class ClassGroupLearnerEntity(
    val classId: String,
    val learnerId: String
)
