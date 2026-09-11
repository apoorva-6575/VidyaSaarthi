package com.hackx.ruraledtech.domain.model

data class ContentPackage(
    val packageId: String,
    val version: Int,
    val subject: String,
    val grade: Int,
    val languages: List<String>,
    val sizeBytes: Long,
    val checksum: String,
    val installedAt: Long?,
    val state: ContentPackageState,
    val priority: String = "normal",
)

enum class ContentPackageState {
    NOT_INSTALLED,
    AVAILABLE,
    INSTALLING,
    VERIFYING,
    INSTALLED,
    UPDATE_AVAILABLE,
    FAILED,
    PENDING_TRANSFER,
}

sealed class ContentLookupResult {
    data class Available(val lesson: Lesson) : ContentLookupResult()
    data class NotAvailableLocally(val packageId: String) : ContentLookupResult()
    data class Pending(val packageId: String) : ContentLookupResult()
    data class Downloading(val packageId: String, val progress: Float) : ContentLookupResult()
    data object Error : ContentLookupResult()
}
