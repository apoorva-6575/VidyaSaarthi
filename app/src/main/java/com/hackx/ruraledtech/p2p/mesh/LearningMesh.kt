package com.hackx.ruraledtech.p2p.mesh

import kotlinx.coroutines.flow.Flow
import com.hackx.ruraledtech.domain.model.ContentRequirement

interface LearningMesh {
    // Commands
    fun startMesh()
    fun stopMesh()
    suspend fun requestContent(requirement: ContentRequirement): Boolean
    
    // Observability for UI
    fun observeMeshState(): Flow<MeshState>
    fun observeTransfers(): Flow<List<TransferTask>>
}
