package com.hackx.ruraledtech.p2p.mesh

import android.util.Log
import com.hackx.ruraledtech.p2p.connection.P2PConnectionManager
import com.hackx.ruraledtech.p2p.integration.ContentRequirement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LearningMeshImpl(
    private val connectionManager: P2PConnectionManager,
    private val meshController: MeshController
    // In a real DI setup (like Hilt), TransferManager and PassportManager would also be injected here
) : LearningMesh {

    private val TAG = "LearningMeshImpl"

    // State flows for Group 1's UI to observe
    private val _meshState = MutableStateFlow(MeshState.IDLE)
    private val _transfers = MutableStateFlow<List<TransferTask>>(emptyList())

    override fun startMesh() {
        Log.d(TAG, "Starting Learning Mesh... Advertising & Discovering.")
        _meshState.value = MeshState.DISCOVERING
        connectionManager.startAdvertising("RuralEdTech-Node")
        connectionManager.startDiscovery()
    }

    override fun stopMesh() {
        Log.d(TAG, "Stopping Learning Mesh.")
        connectionManager.stopAdvertising()
        connectionManager.stopDiscovery()
        _meshState.value = MeshState.OFFLINE
    }

    override suspend fun requestContent(requirement: ContentRequirement): Boolean {
        Log.d(TAG, "Group 2 requested: ${requirement.packageId}. Searching mesh...")
        
        // MVP logic: We broadcast a REQUEST message to all currently connected peers.
        // The MeshController handles the actual network transmission.
        // Return true if we dispatched the request successfully.
        
        return if (_meshState.value == MeshState.CONNECTED || _meshState.value == MeshState.PEERS_AVAILABLE) {
            Log.d(TAG, "Mesh is active. Dispatching demand-driven request.")
            // (In a full implementation, you'd trigger meshController to send a REQUEST)
            true
        } else {
            Log.w(TAG, "Mesh is offline. Cannot fulfill ContentRequirement right now.")
            false
        }
    }

    override fun observeMeshState(): Flow<MeshState> = _meshState.asStateFlow()

    override fun observeTransfers(): Flow<List<TransferTask>> = _transfers.asStateFlow()

    // Internal helper to update states from the various managers
    fun updateMeshState(newState: MeshState) {
        _meshState.value = newState
    }

    fun updateTransfers(tasks: List<TransferTask>) {
        _transfers.value = tasks
    }
}
