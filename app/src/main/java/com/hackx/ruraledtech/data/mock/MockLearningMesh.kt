package com.hackx.ruraledtech.data.mock

import com.hackx.ruraledtech.p2p.integration.ContentRequirement
import com.hackx.ruraledtech.p2p.mesh.LearningMesh
import com.hackx.ruraledtech.p2p.mesh.MeshState
import com.hackx.ruraledtech.p2p.mesh.TransferState
import com.hackx.ruraledtech.p2p.mesh.TransferTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder for Group 3's Nearby Connections-backed [LearningMesh] (PS section 7). Bound
 * in [com.hackx.ruraledtech.core.di.P2PModule] so Group 1's UI (content library, "content
 * unavailable" states) can be built and demoed against realistic mesh/transfer states before
 * the real transport exists. Simulates a peer appearing and a package transferring in once
 * [startMesh] is called — replace this class's binding with the real implementation when
 * Group 3's mesh is ready; no UI call site should need to change.
 */
@Singleton
class MockLearningMesh @Inject constructor(
    private val applicationScope: CoroutineScope,
) : LearningMesh {

    private val meshState = MutableStateFlow(MeshState.OFFLINE)
    private val transfers = MutableStateFlow<List<TransferTask>>(emptyList())
    private var simulationJob: Job? = null

    override fun startMesh() {
        if (simulationJob?.isActive == true) return
        meshState.value = MeshState.DISCOVERING
        simulationJob = applicationScope.launch {
            delay(2_000)
            meshState.value = MeshState.PEERS_AVAILABLE
            delay(1_500)
            meshState.value = MeshState.CONNECTED
            delay(1_000)
            meshState.value = MeshState.IDLE
        }
    }

    override fun stopMesh() {
        simulationJob?.cancel()
        simulationJob = null
        meshState.value = MeshState.OFFLINE
        transfers.value = emptyList()
    }

    override suspend fun requestContent(requirement: ContentRequirement): Boolean {
        val transferId = "T-${requirement.packageId}"
        transfers.update { it + TransferTask(transferId, requirement.packageId, TransferState.QUEUED, 0) }
        meshState.value = MeshState.TRANSFERRING

        for (progress in listOf(20, 45, 70, 90, 100)) {
            delay(400)
            val state = if (progress == 100) TransferState.COMPLETED else TransferState.TRANSFERRING
            transfers.update { list ->
                list.map { if (it.transferId == transferId) it.copy(state = state, progressPercent = progress) else it }
            }
        }

        meshState.value = MeshState.IDLE
        return true
    }

    override fun observeMeshState(): Flow<MeshState> = meshState

    override fun observeTransfers(): Flow<List<TransferTask>> = transfers
}
