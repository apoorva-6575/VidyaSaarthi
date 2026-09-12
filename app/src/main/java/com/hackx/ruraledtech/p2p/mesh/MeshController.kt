package com.hackx.ruraledtech.p2p.mesh

import android.net.Uri
import android.util.Log
import com.hackx.ruraledtech.p2p.connection.ConnectionListener
import com.hackx.ruraledtech.p2p.connection.P2PConnectionManager
import com.hackx.ruraledtech.p2p.manifest.ContentManifest
import com.hackx.ruraledtech.p2p.manifest.ManifestReconciler
import com.hackx.ruraledtech.p2p.manifest.PackageDescriptor
import com.hackx.ruraledtech.p2p.passport.transport.PassportManager
import com.hackx.ruraledtech.p2p.protocol.P2PMessage
import com.hackx.ruraledtech.p2p.protocol.ProtocolSerializer
import com.hackx.ruraledtech.p2p.storage.PackageStorageManager
import com.hackx.ruraledtech.p2p.transfer.TransferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class MeshController(
    private val connectionManager: P2PConnectionManager,
    private val reconciler: ManifestReconciler,
    private val transferManager: TransferManager? = null,
    private val packageStorageManager: PackageStorageManager? = null,
    private val contentPackageDao: com.hackx.ruraledtech.data.local.dao.ContentPackageDao? = null,
    private val learnerDao: com.hackx.ruraledtech.data.local.dao.LearnerDao? = null,
    private val classGroupDao: com.hackx.ruraledtech.data.local.dao.ClassGroupDao? = null,
    private val currentLearnerManager: com.hackx.ruraledtech.core.session.CurrentLearnerManager? = null,
    private var passportManager: PassportManager? = null,
    private val deviceId: String = UUID.randomUUID().toString(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : ConnectionListener {

    private val TAG = "MeshController"

    private var localManifest = ContentManifest(deviceId, 1, emptyList())
    private val connectedEndpoints = mutableSetOf<String>()
    private val pendingConnections = mutableSetOf<String>()

    /**
     * Connected peer endpoint IDs were tracked internally but never exposed anywhere the UI
     * could see them — meaning there was no way to build a "send my passport to this nearby
     * device" screen at all. Backs the same connectedEndpoints set the rest of this class
     * already uses.
     */
    private val _connectedEndpointsFlow = MutableStateFlow<Set<String>>(emptySet())
    val connectedEndpointsFlow: StateFlow<Set<String>> = _connectedEndpointsFlow.asStateFlow()

    private val _availablePeerPackagesFlow = MutableStateFlow<List<PackageDescriptor>>(emptyList())
    val availablePeerPackagesFlow: StateFlow<List<PackageDescriptor>> = _availablePeerPackagesFlow.asStateFlow()

    init {
        syncManifestFromDatabase()

        transferManager?.onPackageInstalled = { packageId, version ->
            // A package installed via P2P transfer already got a REAL checksum from
            // TransferManager.onFileTransferComplete's onSuccess callback (fired just before
            // this one, same install) — don't clobber it. This only needs to fill in a
            // descriptor for packages installed some other way (backend download, demo seed)
            // that never went through that verified-transfer path.
            val alreadyDescribed = localManifest.packages.any { it.packageId == packageId && it.version == version }
            if (alreadyDescribed) {
                Log.d(TAG, "Package $packageId v$version already has a manifest entry from its verified transfer; not overwriting.")
            } else {
                Log.d(TAG, "Package $packageId v$version installed outside P2P transfer. Computing real checksum for Store-and-Forward.")
                coroutineScope.launch {
                    val zipFile = packageStorageManager?.getPackageZipFile(packageId)
                    val checksum = zipFile?.let { packageStorageManager?.computeSha256(it) }
                    if (zipFile == null || checksum == null) {
                        Log.w(TAG, "No local ZIP/checksum available for $packageId — it cannot be advertised for Store-and-Forward yet.")
                        return@launch
                    }

                    val updatedPackages = localManifest.packages.filterNot { it.packageId == packageId } +
                        PackageDescriptor(packageId, version, checksum, zipFile.length())

                    localManifest = localManifest.copy(
                        protocolVersion = localManifest.protocolVersion + 1,
                        packages = updatedPackages
                    )
                    broadcastLocalManifest()
                }
            }
        }
    }

    fun syncManifestFromDatabase() {
        coroutineScope.launch {
            try {
                val installed = contentPackageDao?.getInstalled() ?: emptyList()
                val descriptors = installed.map { entity ->
                    val zipFile = packageStorageManager?.getPackageZipFile(entity.packageId)
                    val checksum = if (zipFile != null && zipFile.exists()) {
                        packageStorageManager?.computeSha256(zipFile) ?: entity.checksum
                    } else {
                        entity.checksum
                    }
                    val size = zipFile?.length() ?: entity.sizeBytes
                    PackageDescriptor(
                        packageId = entity.packageId,
                        version = entity.version,
                        checksum = checksum,
                        sizeBytes = size
                    )
                }
                localManifest = localManifest.copy(
                    packages = descriptors
                )
                Log.d(TAG, "Synced local manifest with ${descriptors.size} packages from Room DB for Mesh.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync local manifest from DB", e)
            }
        }
    }

    fun setPassportManager(pm: PassportManager) {
        this.passportManager = pm
    }

    fun updateLocalManifest(manifest: ContentManifest) {
        localManifest = manifest
    }

    fun getLocalManifest(): ContentManifest = localManifest

    // --- TRANSPORT LIFECYCLE ---

    override fun onPeerDiscovered(endpointId: String, endpointName: String) {
        Log.d(TAG, "[P2P][DISCOVERED] Peer discovered: endpointId=$endpointId name=$endpointName")
        if (connectedEndpoints.contains(endpointId)) {
            Log.d(TAG, "[P2P] Already connected to $endpointId; ignoring discovery.")
            return
        }
        if (pendingConnections.contains(endpointId)) {
            Log.d(TAG, "[P2P] Connection to $endpointId is already pending; ignoring duplicate discovery.")
            return
        }
        pendingConnections.add(endpointId)
        connectionManager.requestConnection(endpointId, endpointName)
    }

    override fun onPeerLost(endpointId: String) {
        Log.d(TAG, "[P2P][PEER_LOST] Peer lost: $endpointId")
        pendingConnections.remove(endpointId)
    }

    override fun onConnectionInitiated(endpointId: String, endpointName: String, authToken: String) {
        Log.d(TAG, "[P2P][INITIATED] Connection initiated with endpoint=$endpointId name=$endpointName auth=$authToken. Auto-accepting.")
        connectionManager.acceptConnection(endpointId)
    }

    override fun onConnectionAccepted(endpointId: String) {
        Log.d(TAG, "[P2P][CONNECTED] Connection accepted with endpoint=$endpointId. Sending Hello & Local Manifest.")
        pendingConnections.remove(endpointId)
        connectedEndpoints.add(endpointId)
        _connectedEndpointsFlow.value = connectedEndpoints.toSet()

        val helloMsg = P2PMessage.Hello(
            messageId = UUID.randomUUID().toString(),
            senderDeviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            protocolVersion = 1
        )
        connectionManager.sendBytes(endpointId, ProtocolSerializer.serialize(helloMsg))

        coroutineScope.launch {
            syncManifestFromDatabase()
            broadcastLocalManifest(endpointId)
            broadcastLearnerSync(endpointId)
        }
    }

    fun broadcastLearnerSync(endpointId: String? = null) {
        coroutineScope.launch {
            try {
                val learnerId = currentLearnerManager?.currentLearnerId?.firstOrNull() ?: return@launch
                val learner = learnerDao?.getById(learnerId) ?: return@launch
                val enrolledClasses = classGroupDao?.observeClassesForLearner(learnerId)?.firstOrNull() ?: emptyList()
                val classIds = enrolledClasses.map { it.classId }

                val syncMsg = P2PMessage.LearnerSync(
                    messageId = UUID.randomUUID().toString(),
                    senderDeviceId = deviceId,
                    timestamp = System.currentTimeMillis(),
                    learnerId = learner.learnerId,
                    name = learner.name,
                    grade = learner.grade,
                    preferredLanguage = learner.preferredLanguage,
                    enrolledClassIds = classIds
                )
                val payload = ProtocolSerializer.serialize(syncMsg)
                if (endpointId != null) {
                    connectionManager.sendBytes(endpointId, payload)
                } else {
                    connectedEndpoints.forEach { connectionManager.sendBytes(it, payload) }
                }
                Log.d(TAG, "Broadcasted LearnerSync for student ${learner.name} with ${classIds.size} class enrollments.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to broadcast LearnerSync", e)
            }
        }
    }

    private fun broadcastLocalManifest(endpointId: String? = null) {
        val manifestMsg = P2PMessage.Manifest(
            messageId = UUID.randomUUID().toString(),
            senderDeviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            manifest = localManifest
        )
        val payload = ProtocolSerializer.serialize(manifestMsg)

        if (endpointId != null) {
            connectionManager.sendBytes(endpointId, payload)
        } else {
            Log.d(TAG, "Manifest updated. Broadcasting to all ${connectedEndpoints.size} connected peers.")
            connectedEndpoints.forEach { connectionManager.sendBytes(it, payload) }
        }
    }

    override fun onConnectionRejected(endpointId: String) {
        Log.w(TAG, "[P2P][CONNECTION_REJECTED] Connection rejected by $endpointId")
        pendingConnections.remove(endpointId)
    }

    override fun onDisconnected(endpointId: String) {
        Log.d(TAG, "[P2P][DISCONNECTED] Disconnected from $endpointId")
        pendingConnections.remove(endpointId)
        connectedEndpoints.remove(endpointId)
        _connectedEndpointsFlow.value = connectedEndpoints.toSet()
    }

    override fun onFilePayloadReceived(endpointId: String, payloadId: Long) {
        Log.d(TAG, "File payload $payloadId received from $endpointId")
        transferManager?.associatePayloadId(endpointId, payloadId)
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
            is P2PMessage.LearnerSync -> {
                Log.d(TAG, "Received P2P LearnerSync from $endpointId: Student ${message.name} (${message.learnerId}) enrolled in: ${message.enrolledClassIds}")
                coroutineScope.launch {
                    try {
                        val now = System.currentTimeMillis()
                        learnerDao?.upsert(
                            com.hackx.ruraledtech.data.local.entities.LearnerEntity(
                                learnerId = message.learnerId,
                                name = message.name,
                                grade = message.grade,
                                preferredLanguage = message.preferredLanguage,
                                avatarKey = "avatar_1",
                                createdAt = now,
                                updatedAt = now,
                                lastActiveAt = now,
                            )
                        )
                        message.enrolledClassIds.forEach { cid ->
                            classGroupDao?.insertLearnerMapping(
                                com.hackx.ruraledtech.data.local.entities.ClassGroupLearnerEntity(
                                    classId = cid,
                                    learnerId = message.learnerId
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error handling LearnerSync over P2P", e)
                    }
                }
            }
        }
    }

    private fun handleRemoteManifest(endpointId: String, remoteManifest: ContentManifest) {
        Log.d(TAG, "Reconciling remote manifest from ${remoteManifest.deviceId}")
        val result = reconciler.reconcile(localManifest, remoteManifest)
        _availablePeerPackagesFlow.value = result.toRequest

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
                packageId = offer.packageId,
                version = offer.version
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

    fun broadcastPackage(packageId: String) {
        coroutineScope.launch {
            syncManifestFromDatabase()
            val zipFile = packageStorageManager?.getPackageZipFile(packageId)
            val checksum = if (zipFile != null && zipFile.exists()) {
                packageStorageManager?.computeSha256(zipFile) ?: ""
            } else ""
            val size = zipFile?.length() ?: 4096L

            val descriptor = localManifest.packages.firstOrNull { it.packageId == packageId }
                ?: PackageDescriptor(packageId, 1, checksum, size)

            val updated = (localManifest.packages.filterNot { it.packageId == packageId } + descriptor)
            localManifest = localManifest.copy(packages = updated)
            broadcastLocalManifest()

            if (zipFile != null && zipFile.exists()) {
                connectedEndpoints.forEach { endpointId ->
                    val transferId = UUID.randomUUID().toString()
                    transferManager?.registerOutboundTransfer(
                        transferId = transferId,
                        packageId = packageId,
                        version = descriptor.version,
                        expectedHash = descriptor.checksum,
                        sizeBytes = size,
                        fileUri = Uri.fromFile(zipFile),
                        endpointId = endpointId
                    )
                    val offer = P2PMessage.Offer(
                        messageId = UUID.randomUUID().toString(),
                        senderDeviceId = deviceId,
                        timestamp = System.currentTimeMillis(),
                        packageId = packageId,
                        version = descriptor.version,
                        expectedHash = descriptor.checksum,
                        transferId = transferId,
                        sizeBytes = size
                    )
                    connectionManager.sendBytes(endpointId, ProtocolSerializer.serialize(offer))
                }
            }
        }
    }

    fun broadcastRequest(packageId: String, version: Int) {
        val req = P2PMessage.Request(
            messageId = UUID.randomUUID().toString(),
            senderDeviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            packageId = packageId,
            version = version
        )
        val payload = ProtocolSerializer.serialize(req)
        Log.d(TAG, "Broadcasting REQUEST for $packageId v$version to ${connectedEndpoints.size} peers.")
        connectedEndpoints.forEach { connectionManager.sendBytes(it, payload) }
    }

    // --- FILE TRANSFER CALLBACKS ---

    override fun onFileTransferProgress(endpointId: String, payloadId: Long, progressPercent: Int) {
        Log.d(TAG, "File transfer progress from $endpointId (payload $payloadId): $progressPercent%")
    }

    override fun onFileTransferComplete(endpointId: String, payloadId: Long, file: File) {
        Log.d(TAG, "File transfer COMPLETE from $endpointId (payload $payloadId). Handing off to TransferManager.")
        transferManager?.onFileTransferComplete(endpointId, payloadId, file) { packageId, version, checksum ->
            val updatedPackages = localManifest.packages.filterNot { it.packageId == packageId } +
                PackageDescriptor(packageId = packageId, version = version, checksum = checksum, sizeBytes = file.length())
            localManifest = localManifest.copy(packages = updatedPackages)
            Log.d(TAG, "Local ContentManifest updated with $packageId v$version for future Store-and-Forward.")
        }
    }

    override fun onFileTransferFailed(endpointId: String, payloadId: Long) {
        Log.e(TAG, "File transfer FAILED from $endpointId (payload $payloadId)")
        transferManager?.onFileTransferFailed(payloadId)
    }
}
