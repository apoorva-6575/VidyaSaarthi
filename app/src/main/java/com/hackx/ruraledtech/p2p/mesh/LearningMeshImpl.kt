package com.hackx.ruraledtech.p2p.mesh

import android.util.Log
import com.hackx.ruraledtech.p2p.connection.P2PConnectionManager
import com.hackx.ruraledtech.p2p.transfer.TransferManager
import com.hackx.ruraledtech.domain.model.ContentRequirement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

import javax.inject.Inject

class LearningMeshImpl @Inject constructor(
    private val connectionManager: P2PConnectionManager,
    private val meshController: MeshController,
    private val transferManager: TransferManager,
) : LearningMesh {

    private val TAG = "LearningMeshImpl"

    private val _isMeshActive = MutableStateFlow(false)

    override fun startMesh() {
        Log.d(TAG, "Starting Learning Mesh... Advertising & Discovering.")
        _isMeshActive.value = true
        meshController.syncManifestFromDatabase()
        connectionManager.startAdvertising("RuralEdTech-Node")
        connectionManager.startDiscovery()
    }

    override fun stopMesh() {
        Log.d(TAG, "Stopping Learning Mesh.")
        _isMeshActive.value = false
        connectionManager.stopAdvertising()
        connectionManager.stopDiscovery()
    }

    override suspend fun requestContent(requirement: ContentRequirement): Boolean {
        Log.d(TAG, "Requested content: ${requirement.packageId}. Searching mesh...")
        meshController.broadcastRequest(requirement.packageId, 0)
        return true
    }

    override fun broadcastPackage(packageId: String) {
        Log.d(TAG, "Proactively broadcasting package $packageId via P2P Mesh.")
        meshController.broadcastPackage(packageId)
    }

    override fun observeMeshState(): Flow<MeshState> = kotlinx.coroutines.flow.combine(
        _isMeshActive,
        meshController.connectedEndpointsFlow,
        transferManager.transfersFlow
    ) { isActive, endpoints, transfers ->
        when {
            !isActive -> MeshState.OFFLINE
            transfers.any { it.state == TransferState.TRANSFERRING || it.state == TransferState.VERIFYING } -> MeshState.TRANSFERRING
            endpoints.isNotEmpty() -> MeshState.CONNECTED
            else -> MeshState.DISCOVERING
        }
    }

    override fun observeTransfers(): Flow<List<TransferTask>> = transferManager.transfersFlow

    override fun observeAvailablePeerPackages(): Flow<List<com.hackx.ruraledtech.p2p.manifest.PackageDescriptor>> =
        meshController.availablePeerPackagesFlow

    override fun observeConnectedEndpoints(): Flow<List<String>> =
        meshController.connectedEndpointsFlow.map { it.toList() }
}
