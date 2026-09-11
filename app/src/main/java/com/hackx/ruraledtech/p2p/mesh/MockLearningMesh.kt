package com.hackx.ruraledtech.p2p.mesh

import android.util.Log
import com.hackx.ruraledtech.p2p.integration.ContentRequirement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MockLearningMesh : LearningMesh {
    
    private val TAG = "MockLearningMesh"
    
    private val _meshState = MutableStateFlow(MeshState.IDLE)
    private val _transfers = MutableStateFlow<List<TransferTask>>(emptyList())
    
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun startMesh() {
        Log.d(TAG, "[MOCK] Starting mesh...")
        _meshState.value = MeshState.DISCOVERING
        
        scope.launch {
            delay(2000)
            Log.d(TAG, "[MOCK] Found mock peer!")
            _meshState.value = MeshState.CONNECTED
        }
    }

    override fun stopMesh() {
        Log.d(TAG, "[MOCK] Stopping mesh.")
        _meshState.value = MeshState.OFFLINE
    }

    override suspend fun requestContent(requirement: ContentRequirement): Boolean {
        Log.d(TAG, "[MOCK] Requesting content: ${requirement.packageId}")
        
        val taskId = UUID.randomUUID().toString()
        val newTask = TransferTask(taskId, requirement.packageId, TransferState.QUEUED, 0)
        _transfers.value = _transfers.value + newTask

        // Simulate a file download over a slow connection
        scope.launch {
            delay(1000)
            updateTaskState(taskId, TransferState.TRANSFERRING, 0)
            
            for (i in 1..10) {
                delay(500) // 500ms per 10%
                updateTaskState(taskId, TransferState.TRANSFERRING, i * 10)
            }
            
            updateTaskState(taskId, TransferState.VERIFYING, 100)
            delay(1000) // Simulate SHA-256 hashing
            
            updateTaskState(taskId, TransferState.COMPLETED, 100)
            Log.d(TAG, "[MOCK] Package ${requirement.packageId} installed successfully.")
            
            // Notify Group 1's mock ContentInstaller here if needed
        }
        
        return true
    }

    override fun observeMeshState(): Flow<MeshState> = _meshState.asStateFlow()

    override fun observeTransfers(): Flow<List<TransferTask>> = _transfers.asStateFlow()

    private fun updateTaskState(taskId: String, state: TransferState, progress: Int) {
        _transfers.value = _transfers.value.map { 
            if (it.transferId == taskId) it.copy(state = state, progressPercent = progress) else it 
        }
    }
}
