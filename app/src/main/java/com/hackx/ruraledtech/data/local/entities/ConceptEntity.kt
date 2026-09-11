package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "concepts")
data class ConceptEntity(
    @PrimaryKey val conceptId: String,
    val subject: String,
    val grade: Int,
    val parentConceptId: String?,
    val name: String,
)
