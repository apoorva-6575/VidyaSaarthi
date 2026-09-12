package com.hackx.ruraledtech.p2p.transfer

import android.net.Uri
import android.util.Log
import com.hackx.ruraledtech.domain.integration.InstallResult
import com.hackx.ruraledtech.p2p.connection.P2PConnectionManager
import com.hackx.ruraledtech.p2p.storage.PackageStorageManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class PendingTransfer(
    val transferId: String,
    val packageId: String,
    val version: Int,
    val expectedHash: String,
    val sizeBytes: Long,
    val endpointId: String,
    val fileUri: Uri? = null,
    var nearbyPayloadId: Long? = null,
    val isOutbound: Boolean,
)

class TransferManager(
    private val connectionManager: P2PConnectionManager,
    private val domainContentInstaller: com.hackx.ruraledtech.domain.integration.ContentInstaller? = null,
    private val legacyContentInstaller: com.hackx.ruraledtech.p2p.integration.ContentInstaller? = null,
    private val packageStorageManager: PackageStorageManager? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val coroutineScope: CoroutineScope = CoroutineScope(ioDispatcher)
) {
    private val TAG = "TransferManager"

    var onPackageInstalled: ((String, Int) -> Unit)? = null

    private val pendingTransfersByTransferId = ConcurrentHashMap<String, PendingTransfer>()
    private val transferIdByPayloadId = ConcurrentHashMap<Long, String>()

    private val _transfersMap = MutableStateFlow<Map<String, com.hackx.ruraledtech.p2p.mesh.TransferTask>>(emptyMap())
    val transfersFlow: StateFlow<List<com.hackx.ruraledtech.p2p.mesh.TransferTask>> = _transfersMap
        .map { it.values.toList() }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    /** Called when preparing to send an offered package. */
    fun registerOutboundTransfer(
        transferId: String,
        packageId: String,
        version: Int,
        expectedHash: String,
        sizeBytes: Long,
        fileUri: Uri,
        endpointId: String
    ) {
        val transfer = PendingTransfer(
            transferId = transferId,
            packageId = packageId,
            version = version,
            expectedHash = expectedHash,
            sizeBytes = sizeBytes,
            endpointId = endpointId,
            fileUri = fileUri,
            isOutbound = true
        )
        pendingTransfersByTransferId[transferId] = transfer
        _transfersMap.value = _transfersMap.value + (transferId to com.hackx.ruraledtech.p2p.mesh.TransferTask(
            transferId = transferId,
            packageId = packageId,
            state = com.hackx.ruraledtech.p2p.mesh.TransferState.QUEUED,
            progressPercent = 0
        ))
        Log.d(TAG, "Registered outbound transfer $transferId for package $packageId")
    }

    /** Called when accepting an offer for an incoming package. */
    fun registerInboundTransfer(
        transferId: String,
        packageId: String,
        version: Int,
        expectedHash: String,
        sizeBytes: Long,
        endpointId: String
    ) {
        val transfer = PendingTransfer(
            transferId = transferId,
            packageId = packageId,
            version = version,
            expectedHash = expectedHash,
            sizeBytes = sizeBytes,
            endpointId = endpointId,
            isOutbound = false
        )
        pendingTransfersByTransferId[transferId] = transfer
        _transfersMap.value = _transfersMap.value + (transferId to com.hackx.ruraledtech.p2p.mesh.TransferTask(
            transferId = transferId,
            packageId = packageId,
            state = com.hackx.ruraledtech.p2p.mesh.TransferState.QUEUED,
            progressPercent = 0
        ))
        Log.d(TAG, "Registered inbound transfer $transferId for package $packageId with expected hash $expectedHash")
    }

    /** Correlates a Nearby Connections generated payloadId with a transferId. */
    fun associatePayloadId(endpointId: String, payloadId: Long, transferId: String? = null) {
        if (transferId != null) {
            val transfer = pendingTransfersByTransferId[transferId]
            if (transfer != null) {
                transfer.nearbyPayloadId = payloadId
                transferIdByPayloadId[payloadId] = transferId
                Log.d(TAG, "Explicitly associated payload $payloadId with transfer $transferId")
                return
            }
        }

        val candidate = pendingTransfersByTransferId.values.firstOrNull {
            it.endpointId == endpointId && it.nearbyPayloadId == null
        }
        if (candidate != null) {
            candidate.nearbyPayloadId = payloadId
            transferIdByPayloadId[payloadId] = candidate.transferId
            Log.d(TAG, "Implicitly associated payload $payloadId with transfer ${candidate.transferId}")
        } else {
            Log.w(TAG, "Could not find pending transfer for endpoint $endpointId to map payload $payloadId")
        }
    }

    /** Triggers sending the registered outbound transfer file. */
    fun startOutboundTransfer(transferId: String): Long? {
        val transfer = pendingTransfersByTransferId[transferId] ?: return null
        if (transfer.fileUri == null) return null

        Log.d(TAG, "Starting outbound file transfer $transferId to ${transfer.endpointId}")
        val payloadId = connectionManager.sendFile(transfer.endpointId, transfer.fileUri)
        associatePayloadId(transfer.endpointId, payloadId, transferId)
        return payloadId
    }

    fun onFileTransferProgress(endpointId: String, payloadId: Long, progressPercent: Int) {
        val transferId = transferIdByPayloadId[payloadId]
            ?: pendingTransfersByTransferId.values.firstOrNull { it.endpointId == endpointId }?.transferId
        if (transferId != null) {
            val pending = pendingTransfersByTransferId[transferId]
            if (pending != null) {
                _transfersMap.value = _transfersMap.value + (transferId to com.hackx.ruraledtech.p2p.mesh.TransferTask(
                    transferId = transferId,
                    packageId = pending.packageId,
                    state = com.hackx.ruraledtech.p2p.mesh.TransferState.TRANSFERRING,
                    progressPercent = progressPercent
                ))
            }
        }
    }

    fun getTransferByPayloadId(payloadId: Long): PendingTransfer? {
        val transferId = transferIdByPayloadId[payloadId] ?: return null
        return pendingTransfersByTransferId[transferId]
    }

    fun getPendingTransfer(transferId: String): PendingTransfer? {
        return pendingTransfersByTransferId[transferId]
    }

    /** Called when Nearby Connections finishes receiving a file payload. */
    fun onFileTransferComplete(
        endpointId: String,
        payloadId: Long,
        tempFile: File,
        onSuccess: ((packageId: String, version: Int, checksum: String) -> Unit)? = null
    ) {
        var transferId = transferIdByPayloadId.remove(payloadId)
        var transfer = if (transferId != null) pendingTransfersByTransferId.remove(transferId) else null

        if (transfer == null) {
            val candidate = pendingTransfersByTransferId.values.firstOrNull { it.endpointId == endpointId && !it.isOutbound }
            if (candidate != null) {
                transfer = pendingTransfersByTransferId.remove(candidate.transferId)
                transferId = candidate.transferId
            }
        }

        if (transfer == null) {
            Log.e(TAG, "Received file payload $payloadId from $endpointId without matching transfer metadata. Deleting.")
            tempFile.delete()
            return
        }

        val finalTransfer = transfer
        val finalTransferId = transferId ?: finalTransfer.transferId

        _transfersMap.value = _transfersMap.value + (finalTransferId to com.hackx.ruraledtech.p2p.mesh.TransferTask(
            transferId = finalTransferId,
            packageId = finalTransfer.packageId,
            state = com.hackx.ruraledtech.p2p.mesh.TransferState.VERIFYING,
            progressPercent = 100
        ))

        coroutineScope.launch {
            Log.d(TAG, "Verifying downloaded package ${finalTransfer.packageId} (transfer $finalTransferId)...")
            val isValid = PackageVerifier.verifyFile(tempFile, finalTransfer.expectedHash, ioDispatcher)

            if (!isValid) {
                Log.e(TAG, "Verification FAILED for ${finalTransfer.packageId}. Tampered or corrupted file. Deleting.")
                tempFile.delete()
                _transfersMap.value = _transfersMap.value + (finalTransferId to com.hackx.ruraledtech.p2p.mesh.TransferTask(
                    transferId = finalTransferId,
                    packageId = finalTransfer.packageId,
                    state = com.hackx.ruraledtech.p2p.mesh.TransferState.FAILED,
                    progressPercent = 0
                ))
                return@launch
            }

            Log.d(TAG, "Verification SUCCESS for ${finalTransfer.packageId}. Storing for store-and-forward and installing.")

            // Persist ZIP for store-and-forward
            packageStorageManager?.savePackageZip(finalTransfer.packageId, tempFile)

            val installDir = if (tempFile.name.endsWith(".zip", ignoreCase = true) || isZipArchive(tempFile)) {
                val targetDir = File(tempFile.parentFile, "extracted_${finalTransfer.packageId}_${System.currentTimeMillis()}")
                val extracted = packageStorageManager?.extractZip(tempFile, targetDir) ?: extractZipDirect(tempFile, targetDir)
                if (extracted) targetDir else tempFile
            } else {
                tempFile
            }

            val installedSuccessfully = performInstall(installDir.absolutePath)

            if (installedSuccessfully) {
                Log.d(TAG, "Package ${finalTransfer.packageId} installed successfully into Room database!")
                _transfersMap.value = _transfersMap.value + (finalTransferId to com.hackx.ruraledtech.p2p.mesh.TransferTask(
                    transferId = finalTransferId,
                    packageId = finalTransfer.packageId,
                    state = com.hackx.ruraledtech.p2p.mesh.TransferState.COMPLETED,
                    progressPercent = 100
                ))
                onSuccess?.invoke(finalTransfer.packageId, finalTransfer.version, finalTransfer.expectedHash)
                onPackageInstalled?.invoke(finalTransfer.packageId, finalTransfer.version)
            } else {
                Log.e(TAG, "ContentInstaller failed to install package ${finalTransfer.packageId}")
                _transfersMap.value = _transfersMap.value + (finalTransferId to com.hackx.ruraledtech.p2p.mesh.TransferTask(
                    transferId = finalTransferId,
                    packageId = finalTransfer.packageId,
                    state = com.hackx.ruraledtech.p2p.mesh.TransferState.FAILED,
                    progressPercent = 0
                ))
            }
        }
    }

    fun onFileTransferFailed(payloadId: Long) {
        val transferId = transferIdByPayloadId.remove(payloadId)
        if (transferId != null) {
            pendingTransfersByTransferId.remove(transferId)
        }
        Log.d(TAG, "Transfer failed for payload $payloadId. Cleaned up expected file tracking.")
    }

    private suspend fun performInstall(packagePath: String): Boolean {
        if (domainContentInstaller != null) {
            return when (val result = domainContentInstaller.install(packagePath)) {
                is InstallResult.Success -> true
                else -> {
                    Log.w(TAG, "Domain ContentInstaller returned: $result")
                    false
                }
            }
        }
        if (legacyContentInstaller != null) {
            return legacyContentInstaller.install(packagePath)
        }
        return false
    }

    private fun isZipArchive(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        return try {
            val bytes = ByteArray(4)
            java.io.FileInputStream(file).use { it.read(bytes) }
            bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
        } catch (e: Exception) {
            false
        }
    }

    private fun extractZipDirect(zipFile: File, targetDir: File): Boolean {
        return packageStorageManager?.extractZip(zipFile, targetDir) ?: false
    }
}
