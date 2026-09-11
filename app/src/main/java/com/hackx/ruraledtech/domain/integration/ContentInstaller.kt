package com.hackx.ruraledtech.domain.integration

/**
 * Entry point Group 3 (P2P Learning Mesh) and Group 4 (backend content downloads) call once
 * a content package's bytes have been acquired by whatever transport they own. Group 1 owns
 * everything from here: unpacking, checksum verification, and registering into Room.
 */
interface ContentInstaller {
    suspend fun install(packagePath: String): InstallResult
    suspend fun remove(packageId: String, version: Int)
    suspend fun validate(packagePath: String): ValidationResult
}

sealed class InstallResult {
    data class Success(val packageId: String, val version: Int) : InstallResult()
    data class ChecksumMismatch(val expected: String, val actual: String) : InstallResult()
    data class Corrupted(val reason: String) : InstallResult()
    data class Failed(val reason: String) : InstallResult()
}

sealed class ValidationResult {
    data class Valid(val packageId: String, val version: Int, val checksum: String) : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}
