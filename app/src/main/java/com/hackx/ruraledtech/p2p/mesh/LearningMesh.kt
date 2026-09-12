package com.hackx.ruraledtech.p2p.mesh

import kotlinx.coroutines.flow.Flow
import com.hackx.ruraledtech.domain.model.ContentRequirement

import com.hackx.ruraledtech.p2p.manifest.PackageDescriptor

interface LearningMesh {
    // Commands
    fun startMesh()
    fun stopMesh()
    suspend fun requestContent(requirement: ContentRequirement): Boolean
    fun broadcastPackage(packageId: String)
    
    // Observability for UI
    fun observeMeshState(): Flow<MeshState>
    fun observeTransfers(): Flow<List<TransferTask>>
    fun observeAvailablePeerPackages(): Flow<List<PackageDescriptor>>

    /** Connected peer endpoint IDs — e.g. to let the UI offer "send my passport to this device." */
    fun observeConnectedEndpoints(): Flow<List<String>>
}
