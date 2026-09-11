package com.hackx.ruraledtech.domain.integration

/**
 * Seam between Group 2 learning engine and local content repository / Group 3 P2P mesh.
 * Allows the adaptive engine to check whether required content packages exist locally.
 */
interface ContentAvailabilityProvider {
    suspend fun isPackageAvailable(packageId: String): Boolean
    suspend fun getAvailablePackageIds(): List<String>
}
