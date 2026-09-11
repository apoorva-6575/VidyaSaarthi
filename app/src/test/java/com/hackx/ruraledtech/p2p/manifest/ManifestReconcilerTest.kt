package com.hackx.ruraledtech.p2p.manifest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestReconcilerTest {

    private val reconciler = ManifestReconciler()

    @Test
    fun `test reconciliation logic`() {
        val local = ContentManifest(
            deviceId = "DeviceA",
            protocolVersion = 1,
            packages = listOf(
                PackageDescriptor("math-v1", 1, "hash1", 100L), // Outdated
                PackageDescriptor("science-v2", 2, "hash2", 200L) // Newer
            )
        )

        val remote = ContentManifest(
            deviceId = "DeviceB",
            protocolVersion = 1,
            packages = listOf(
                PackageDescriptor("math-v1", 3, "hash3", 150L), // Newer
                PackageDescriptor("science-v2", 1, "hash4", 180L), // Outdated
                PackageDescriptor("hindi-v1", 1, "hash5", 300L) // New to local
            )
        )

        val result = reconciler.reconcile(local, remote)

        // Local should request Math v3 (update) and Hindi v1 (new)
        assertEquals(2, result.toRequest.size)
        assertTrue(result.toRequest.any { it.packageId == "math-v1" && it.version == 3 })
        assertTrue(result.toRequest.any { it.packageId == "hindi-v1" })

        // Local should offer Science v2 (update for remote)
        assertEquals(1, result.toOffer.size)
        assertTrue(result.toOffer.any { it.packageId == "science-v2" && it.version == 2 })
    }
}
