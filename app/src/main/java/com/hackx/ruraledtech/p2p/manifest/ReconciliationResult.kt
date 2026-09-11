package com.hackx.ruraledtech.p2p.manifest

data class ReconciliationResult(
    val toRequest: List<PackageDescriptor>,
    val toOffer: List<PackageDescriptor>
)
