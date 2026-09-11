package com.hackx.ruraledtech.p2p.manifest

import kotlinx.serialization.Serializable

@Serializable
data class ContentManifest(
    val deviceId: String,
    val protocolVersion: Int,
    val packages: List<PackageDescriptor>
)

@Serializable
data class PackageDescriptor(
    val packageId: String,
    val version: Int,
    val checksum: String,
    val sizeBytes: Long
)
