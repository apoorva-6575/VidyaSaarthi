package com.hackx.ruraledtech.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_events")
data class SyncEventEntity(
    @PrimaryKey val eventId: String,
    val learnerId: String,
    val deviceId: String,
    val eventType: String,
    val timestamp: Long,
    val payloadJson: String,
    val syncStatus: String,
)
