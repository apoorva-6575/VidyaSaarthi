package com.hackx.ruraledtech.p2p.mesh

import android.util.Log
import com.hackx.ruraledtech.p2p.connection.ConnectionListener
import com.hackx.ruraledtech.p2p.connection.P2PConnectionManager
import com.hackx.ruraledtech.p2p.manifest.ContentManifest
import com.hackx.ruraledtech.p2p.manifest.ManifestReconciler
import com.hackx.ruraledtech.p2p.protocol.P2PMessage
import com.hackx.ruraledtech.p2p.protocol.ProtocolSerializer
import java.io.File
import java.util.UUID

class MeshController(
    private val connectionManager: P2PConnectionManager,
    private val reconciler: ManifestReconciler,
    private val deviceId: String = UUID.randomUUID().toString()
) : ConnectionListener {

    private val TAG = "MeshController"

    // For MVP, we hold a local reference to our inventory manifest
    private var localManifest = ContentManifest(deviceId, 1, emptyList())

    fun updateLocalManifest(manifest: ContentManifest) {
        localManifest = manifest
    }

    // --- TRANSPORT LIFECYCLE ---

    override fun onPeerDiscovered(endpointId: String, endpointName: String) {
        Log.d(TAG, "Peer discovered: $endpointId. Awaiting manual/UI connection request.")
        // In a fully autonomous mesh, you could call connectionManager.requestConnection() here.
    }

    override fun onPeerLost(endpointId: String) {
        Log.d(TAG, "Peer lost: $endpointId")
    }

    override fun onConnectionInitiated(endpointId: String, endpointName: String, authToken: String) {
        Log.d(TAG, "Connection initiated with $endpointId. Auto-accepting for MVP.")
        // MVP Shortcut: Auto-accept. In production, verify the PIN/AuthToken via UI.
        connectionManager.acceptConnection(endpointId)
    }

    override fun onConnectionAccepted(endpointId: String) {
        Log.d(TAG, "Connection accepted with $endpointId. Sending local manifest.")
        
        // As soon as we connect, we broadcast what we have.
        val manifestMsg = P2PMessage.Manifest(
            messageId = UUID.randomUUID().toString(),
            senderDeviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            manifest = localManifest
        )
        connectionManager.sendBytes(endpointId, ProtocolSerializer.serialize(manifestMsg))
    }

    override fun onConnectionRejected(endpointId: String) {
        Log.w(TAG, "Connection rejected by $endpointId")
    }

    override fun onDisconnected(endpointId: String) {
        Log.d(TAG, "Disconnected from $endpointId")
    }

    // --- PROTOCOL ROUTING ---

    override fun onBytesReceived(endpointId: String, bytes: ByteArray) {
        val message = ProtocolSerializer.deserialize(bytes)
        if (message == null) {
            Log.e(TAG, "Failed to deserialize incoming P2PMessage")
            return
        }

        when (message) {
            is P2PMessage.Hello -> Log.d(TAG, "Received Hello from ${message.senderDeviceId}")
            is P2PMessage.Manifest -> handleRemoteManifest(endpointId, message.manifest)
            is P2PMessage.Request -> handleIncomingRequest(endpointId, message)
            is P2PMessage.Offer -> handleIncomingOffer(endpointId, message)
            is P2PMessage.Accept -> Log.d(TAG, "Peer accepted our offer for ${message.packageId}. Ready to trigger TransferManager.")
            is P2PMessage.PassportTransfer -> Log.d(TAG, "Received PassportTransfer from ${message.senderDeviceId}. Handling not yet implemented.")
        }
    }

    private fun handleRemoteManifest(endpointId: String, remoteManifest: ContentManifest) {
        Log.d(TAG, "Reconciling remote manifest from ${remoteManifest.deviceId}")
        val result = reconciler.reconcile(localManifest, remoteManifest)

        // Generate REQUEST messages for things we need
        result.toRequest.forEach { pkg ->
            Log.d(TAG, "I need ${pkg.packageId} v${pkg.version}. Sending REQUEST.")
            val req = P2PMessage.Request(
                messageId = UUID.randomUUID().toString(),
                senderDeviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                packageId = pkg.packageId,
                version = pkg.version
            )
            connectionManager.sendBytes(endpointId, ProtocolSerializer.serialize(req))
        }

        // Generate OFFER messages for things they need
        result.toOffer.forEach { pkg ->
            Log.d(TAG, "I can offer ${pkg.packageId} v${pkg.version}. Sending OFFER.")
            val offer = P2PMessage.Offer(
                messageId = UUID.randomUUID().toString(),
                senderDeviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                packageId = pkg.packageId,
                version = pkg.version
            )
            connectionManager.sendBytes(endpointId, ProtocolSerializer.serialize(offer))
        }
    }

    private fun handleIncomingRequest(endpointId: String, request: P2PMessage.Request) {
        Log.d(TAG, "Peer $endpointId requested ${request.packageId} v${request.version}")
        // In the next epic, this will trigger the TransferManager to send the actual .pkg file
    }

    private fun handleIncomingOffer(endpointId: String, offer: P2PMessage.Offer) {
        Log.d(TAG, "Peer $endpointId offered ${offer.packageId} v${offer.version}")
        // Next epic: If we still need it, send an ACCEPT message.
    }

    // --- FILE TRANSFER STUBS ---
    override fun onFileTransferProgress(endpointId: String, payloadId: Long, progressPercent: Int) {}
    override fun onFileTransferComplete(endpointId: String, payloadId: Long, file: File) {}
    override fun onFileTransferFailed(endpointId: String, payloadId: Long) {}
}
