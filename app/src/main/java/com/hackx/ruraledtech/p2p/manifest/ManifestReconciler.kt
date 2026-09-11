package com.hackx.ruraledtech.p2p.manifest

class ManifestReconciler @javax.inject.Inject constructor() {

    fun reconcile(
        local: ContentManifest,
        remote: ContentManifest
    ): ReconciliationResult {
        val toRequest = mutableListOf<PackageDescriptor>()
        val toOffer = mutableListOf<PackageDescriptor>()

        // Map for O(1) lookups
        val localMap = local.packages.associateBy { it.packageId }
        val remoteMap = remote.packages.associateBy { it.packageId }

        // 1. Find what we need from remote (Requests)
        for (remotePkg in remote.packages) {
            val localPkg = localMap[remotePkg.packageId]
            if (localPkg == null || remotePkg.version > localPkg.version) {
                toRequest.add(remotePkg)
            }
        }

        // 2. Find what we can give to remote (Offers)
        for (localPkg in local.packages) {
            val remotePkg = remoteMap[localPkg.packageId]
            if (remotePkg == null || localPkg.version > remotePkg.version) {
                toOffer.add(localPkg)
            }
        }

        return ReconciliationResult(toRequest, toOffer)
    }
}
