package com.hackx.ruraledtech.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ContentPackageDto(
    val id: String,
    val version: Int,
    val subject: String,
    val grade: String,
    val language: String,
    val checksum: String,
    val size: Long,
    val status: String,
    val metadata_json: JsonObject
)
