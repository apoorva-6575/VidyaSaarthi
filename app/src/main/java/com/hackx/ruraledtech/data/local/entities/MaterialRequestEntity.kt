package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "material_requests")
data class MaterialRequestEntity(
    @PrimaryKey val requestId: String,
    val packageId: String,
    val requesterId: String,
    val providerId: String?,
    val status: String,
    val isIncoming: Boolean,
    val lastUpdated: Long
)
