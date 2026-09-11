package com.hackx.ruraledtech.p2p.mesh

import android.net.Uri
import android.util.Log
import com.hackx.ruraledtech.p2p.connection.ConnectionListener
import com.hackx.ruraledtech.p2p.connection.P2PConnectionManager
import com.hackx.ruraledtech.p2p.manifest.ContentManifest
import com.hackx.ruraledtech.p2p.manifest.ManifestReconciler
import com.hackx.ruraledtech.p2p.manifest.PackageDescriptor
import com.hackx.ruraledtech.p2p.protocol.P2PMessage
import com.hackx.ruraledtech.p2p.protocol.ProtocolSerializer
import com.hackx.ruraledtech.p2p.storage.PackageStorageManager
import com.hackx.ruraledtech.p2p.transfer.TransferManager
import com.hackx.ruraledtech.p2p.passport.transport.PassportManager
import java.io.File
import java.util.UUID

class MeshController(
    private val connectionManager: P2PConnectionManager,
    private val reconciler: ManifestReconciler,
    private val transferManager: TransferManager? = null,
    private val packageStorageManager: PackageStorageManager? = null,
    private val passportManager: PassportManager? = null,
    private val deviceId: String = UUID.randomUUID().toString()
) : ConnectionListener {

    private val TAG = "MeshController"

    private var localManifest = ContentManifest(deviceId, 1, emptyList())

    fun updateLocalManifest(manifest: ContentManifest) {
        localManifest = manifest
    }

    fun getLocalManifest(): ContentManifest = localManifest

    // --- TRANSPORT LIFECYCLE ---

    override fun onPeerDiscovered(endpointId: String, endpointName: String) {
        Log.d(TAG, "Peer discovered: $endpointId ($endpointName)")
    }

    override fun onPeerLost(endpointId: String) {
        Log.d(TAG, "Peer lost: $endpointId")
    }

    override fun onConnectionInitiated(endpointId: String, endpointName: String, authToken: String) {
        Log.d(TAG, "Connection initiated with $endpointId. Auto-accepting.")
        connectionManager.acceptConnection(endpointId)
    }

    override fun onConnectionAccepted(endpointId: String) {
        Log.d(TAG, "Connection accepted with $endpointId. Sending local manifest.")
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
            Log.e(TAG, "Failed to deserialize incoming P2PMessage from $endpointId")
            return
        }

        when (message) {
            is P2PMessage.Hello -> Log.d(TAG, "Received Hello from ${message.senderDeviceId}")
            is P2PMessage.Manifest -> handleRemoteManifest(endpointId, message.manifest)
            is P2PMessage.Request -> handleIncomingRequest(endpointId, message)
            is P2PMessage.Offer -> handleIncomingOffer(endpointId, message)
            is P2PMessage.Accept -> handleIncomingAccept(endpointId, message)
            is P2PMessage.PassportTransfer -> {
                Log.d(TAG, "Received PassportTransfer from ${message.senderDeviceId}")
                passportManager?.onPassportReceived(message.passport)
            }
        }
    }

    private fun handleRemoteManifest(endpointId: String, remoteManifest: ContentManifest) {
        Log.d(TAG, "Reconciling remote manifest from ${remoteManifest.deviceId}")
        val result = reconciler.reconcile(localManifest, remoteManifest)

        // Request content packages we lack or have older versions of
        result.toRequest.forEach { pkg ->
            Log.d(TAG, "Sending REQUEST for ${pkg.packageId} v${pkg.version} to $endpointId")
            val req = P2PMessage.Request(
                messageId = UUID.randomUUID().toString(),
                senderDeviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                packageId = pkg.packageId,
                version = pkg.version
            )
            connectionManager.sendBytes(endpointId, ProtocolSerializer.serialize(req))
        }

        // Offer content packages peer lacks
        result.toOffer.forEach { pkg ->
            val zipFile = packageStorageManager?.getPackageZipFile(pkg.packageId)
            if (zipFile != null && zipFile.exists()) {
                val transferId = UUID.randomUUID().toString()
                transferManager?.registerOutboundTransfer(
                    transferId = transferId,
                    packageId = pkg.packageId,
                    version = pkg.version,
                    expectedHash = pkg.checksum,
                    sizeBytes = pkg.sizeBytes,
                    fileUri = Uri.fromFile(zipFile),
                    endpointId = endpointId
                )

                Log.d(TAG, "Sending OFFER for ${pkg.packageId} v${pkg.version} (transferId: $transferId) to $endpointId")
                val offer = P2PMessage.Offer(
                    messageId = UUID.randomUUID().toString(),
                    senderDeviceId = deviceId,
                    timestamp = System.currentTimeMillis(),
                    packageId = pkg.packageId,
                    version = pkg.version,
                    expectedHash = pkg.checksum,
                    transferId = transferId,
                    sizeBytes = pkg.sizeBytes
                )
                connectionManager.sendBytes(endpointId, ProtocolSerializer.serialize(offer))
            }
        }
    }

    private fun handleIncomingRequest(endpointId: String, request: P2PMessage.Request) {
        Log.d(TAG, "Peer $endpointId requested ${request.packageId} v${request.version}")

        val pkgDescriptor = localManifest.packages.firstOrNull { it.packageId == request.packageId }
        val zipFile = packageStorageManager?.getPackageZipFile(request.packageId)

        if (zipFile != null && zipFile.exists()) {
            val checksum = pkgDescriptor?.checksum ?: ""
            val sizeBytes = pkgDescriptor?.sizeBytes ?: zipFile.length()
            val transferId = UUID.randomUUID().toString()

            transferManager?.registerOutboundTransfer(
                transferId = transferId,
                packageId = request.packageId,
                version = request.version,
                expectedHash = checksum,
                sizeBytes = sizeBytes,
                fileUri = Uri.fromFile(zipFile),
                endpointId = endpointId
            )

            Log.d(TAG, "Sending OFFER for requested ${request.packageId} (transferId: $transferId) to $endpointId")
            val offer = P2PMessage.Offer(
                messageId = UUID.randomUUID().toString(),
                senderDeviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                packageId = request.packageId,
                version = request.version,
                expectedHash = checksum,
                transferId = transferId,
                sizeBytes = sizeBytes
            )
            connectionManager.sendBytes(endpointId, ProtocolSerializer.serialize(offer))
        } else {
            Log.w(TAG, "Cannot fulfill REQUEST for ${request.packageId}: package ZIP not found locally")
        }
    }

    private fun handleIncomingOffer(endpointId: String, offer: P2PMessage.Offer) {
        Log.d(TAG, "Peer $endpointId offered ${offer.packageId} v${offer.version} (transferId: ${offer.transferId})")

        val localPkg = localManifest.packages.firstOrNull { it.packageId == offer.packageId }
        val stillNeeds = localPkg == null || offer.version > localPkg.version

        if (stillNeeds) {
            transferManager?.registerInboundTransfer(
                transferId = offer.transferId,
                packageId = offer.packageId,
                version = offer.version,
                expectedHash = offer.expectedHash,
                sizeBytes = offer.sizeBytes,
                endpointId = endpointId
            )

            Log.d(TAG, "Accepting OFFER for ${offer.packageId} (transferId: ${offer.transferId}). Sending ACCEPT.")
            val acceptMsg = P2PMessage.Accept(
                messageId = UUID.randomUUID().toString(),
                senderDeviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                transferId = offer.transferId,
                packageId = offer.packageId
            )
            connectionManager.sendBytes(endpointId, ProtocolSerializer.serialize(acceptMsg))
        }
    }

    private fun handleIncomingAccept(endpointId: String, accept: P2PMessage.Accept) {
        Log.d(TAG, "Peer $endpointId ACCEPTED offer for transfer ${accept.transferId} (${accept.packageId})")
        val payloadId = transferManager?.startOutboundTransfer(accept.transferId)
        if (payloadId != null) {
            Log.d(TAG, "Started outbound file payload $payloadId for transfer ${accept.transferId}")
        } else {
            Log.e(TAG, "Failed to start outbound transfer for ${accept.transferId}")
        }
    }

    // --- FILE TRANSFER CALLBACKS ---

    override fun onFileTransferProgress(endpointId: String, payloadId: Long, progressPercent: Int) {
        Log.d(TAG, "File transfer progress from $endpointId (payload $payloadId): $progressPercent%")
    }

    override fun onFileTransferComplete(endpointId: String, payloadId: Long, file: File) {
        Log.d(TAG, "File transfer COMPLETE from $endpointId (payload $payloadId). Handing off to TransferManager.")
        transferManager?.onFileTransferComplete(endpointId, payloadId, file) { packageId, version, checksum ->
            // Store-and-Forward: Update local manifest to include newly installed package!
            val updatedPackages = localManifest.packages.filterNot { it.packageId == packageId } +
                PackageDescriptor(packageId = packageId, version = version, checksum = checksum, sizeBytes = file.length())
            localManifest = localManifest.copy(packages = updatedPackages)
            Log.d(TAG, "Local ContentManifest updated with $packageId v$version for future Store-and-Forward.")
        }
    }

    override fun onFileTransferFailed(endpointId: String, payloadId: Long) {
        Log.e(TAG, "File transfer FAILED from $endpointId (payload $payloadId)")
    }
}
