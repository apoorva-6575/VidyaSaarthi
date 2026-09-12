package com.hackx.ruraledtech.p2p.connection

import java.io.File

interface ConnectionListener {
    fun onPeerDiscovered(endpointId: String, endpointName: String)
    fun onPeerLost(endpointId: String)
    fun onConnectionInitiated(endpointId: String, endpointName: String, authToken: String)
    fun onConnectionAccepted(endpointId: String)
    fun onConnectionRejected(endpointId: String)
    fun onDisconnected(endpointId: String)
    
    // Payload Events
    fun onBytesReceived(endpointId: String, bytes: ByteArray)
    fun onFilePayloadReceived(endpointId: String, payloadId: Long) {}
    fun onFileTransferProgress(endpointId: String, payloadId: Long, progressPercent: Int)
    fun onFileTransferComplete(endpointId: String, payloadId: Long, file: File)
    fun onFileTransferFailed(endpointId: String, payloadId: Long)
}
