package com.hackx.ruraledtech.p2p.mesh

enum class MeshState {
    OFFLINE, DISCOVERING, PEERS_AVAILABLE, CONNECTED, TRANSFERRING, IDLE, ERROR
}

enum class TransferState {
    QUEUED, CONNECTING, TRANSFERRING, VERIFYING, COMPLETED, FAILED
}

data class TransferTask(
    val transferId: String,
    val packageId: String,
    val state: TransferState,
    val progressPercent: Int
)
