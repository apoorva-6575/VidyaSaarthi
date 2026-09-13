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

    fun getPendingOutboundTransfer(
        packageId: String,
        endpointId: String
    ): PendingTransfer? {
        return pendingTransfersByTransferId.values.firstOrNull {
            it.isOutbound &&
                it.packageId == packageId &&
                it.endpointId == endpointId
        }
    }

    fun getPendingInboundTransfer(
        packageId: String,
        endpointId: String
    ): PendingTransfer? {
        return pendingTransfersByTransferId.values.firstOrNull {
            !it.isOutbound &&
                it.packageId == packageId &&
                it.endpointId == endpointId
        }
    }

    fun clearTransfer(transferId: String) {
        pendingTransfersByTransferId.remove(transferId)
        transferIdByPayloadId.entries.removeIf {
            it.value == transferId
        }

        _transfersMap.value =
            _transfersMap.value - transferId
    }

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
            progressPercent = 0,
            endpointId = endpointId,
            expectedHash = expectedHash,
            sizeBytes = sizeBytes
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
            progressPercent = 0,
            endpointId = endpointId,
            expectedHash = expectedHash,
            sizeBytes = sizeBytes
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
        val transfer = pendingTransfersByTransferId[transferId] ?: run {
            Log.e(TAG, "[P2P][FILE_SEND_ERROR] No pending transfer found for transferId=$transferId")
            return null
        }

        if (transfer.nearbyPayloadId != null) {
            Log.d(
                TAG,
                "[P2P][FILE_SEND_DUPLICATE_IGNORED] " +
                    "transferId=$transferId " +
                    "payloadId=${transfer.nearbyPayloadId}"
            )
            return transfer.nearbyPayloadId
        }

        val zipFile = packageStorageManager?.getPackageZipFile(transfer.packageId)

        val payloadId = if (zipFile != null && zipFile.exists() && zipFile.length() > 0) {
            connectionManager.sendFile(transfer.endpointId, zipFile)
        } else if (transfer.fileUri != null) {
            connectionManager.sendFile(transfer.endpointId, transfer.fileUri)
        } else {
            Log.e(TAG, "[P2P][FILE_SEND_ERROR] Cannot start outbound transfer $transferId: no valid file or URI found for package ${transfer.packageId}")
            return null
        }

        associatePayloadId(transfer.endpointId, payloadId, transferId)
        Log.d(
            TAG,
            "[P2P][FILE_SEND_START] transferId=$transferId payloadId=$payloadId file=${zipFile?.absolutePath ?: transfer.fileUri} size=${zipFile?.length() ?: 0}"
        )

        _transfersMap.value = _transfersMap.value + (transferId to com.hackx.ruraledtech.p2p.mesh.TransferTask(
            transferId = transferId,
            packageId = transfer.packageId,
            state = com.hackx.ruraledtech.p2p.mesh.TransferState.TRANSFERRING,
            progressPercent = 0,
            endpointId = transfer.endpointId,
            expectedHash = transfer.expectedHash,
            sizeBytes = transfer.sizeBytes
        ))
        return payloadId
    }

    fun onOutgoingFileTransferComplete(
        payloadId: Long
    ) {
        val transferId =
            transferIdByPayloadId.remove(payloadId)

        if (transferId == null) {
            Log.w(
                TAG,
                "[P2P][FILE_SEND_SUCCESS] " +
                    "No transfer metadata for payload=$payloadId"
            )
            return
        }

        val transfer =
            pendingTransfersByTransferId.remove(transferId)

        if (transfer == null) {
            Log.w(
                TAG,
                "[P2P][FILE_SEND_SUCCESS] " +
                    "Transfer metadata already removed " +
                    "for transfer=$transferId"
            )
            return
        }

        _transfersMap.value =
            _transfersMap.value +
                (
                    transferId to
                        com.hackx.ruraledtech.p2p.mesh.TransferTask(
                            transferId = transferId,
                            packageId = transfer.packageId,
                            state = com.hackx.ruraledtech.p2p.mesh.TransferState.COMPLETED,
                            progressPercent = 100,
                            endpointId = transfer.endpointId,
                            expectedHash = transfer.expectedHash,
                            sizeBytes = transfer.sizeBytes
                        )
                )

        Log.d(
            TAG,
            "[P2P][FILE_SEND_SUCCESS] " +
                "transferId=$transferId " +
                "package=${transfer.packageId} " +
                "payload=$payloadId"
        )
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
                    progressPercent = progressPercent,
                    endpointId = pending.endpointId,
                    expectedHash = pending.expectedHash,
                    sizeBytes = pending.sizeBytes
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
            progressPercent = 100,
            endpointId = finalTransfer.endpointId,
            expectedHash = finalTransfer.expectedHash,
            sizeBytes = finalTransfer.sizeBytes
        ))

        coroutineScope.launch {
            val calculatedSha256 = packageStorageManager?.computeSha256(tempFile) ?: ""
            Log.d(
                TAG,
                "[P2P][FILE_RECEIVED] " +
                    "payloadId=$payloadId " +
                    "package=${finalTransfer.packageId} " +
                    "size=${tempFile.length()}"
            )
            Log.d(
                TAG,
                "[P2P][SHA256_EXPECTED] " +
                    finalTransfer.expectedHash
            )
            Log.d(
                TAG,
                "[P2P][SHA256_ACTUAL] " +
                    calculatedSha256
            )

            val isValid = PackageVerifier.verifyFile(tempFile, finalTransfer.expectedHash, ioDispatcher)

            if (!isValid) {
                Log.e(TAG, "[P2P][SHA256_VERIFY_FAILED] Expected ${finalTransfer.expectedHash} but computed $calculatedSha256 for package ${finalTransfer.packageId}. Deleting temp file.")
                tempFile.delete()
                _transfersMap.value = _transfersMap.value + (finalTransferId to com.hackx.ruraledtech.p2p.mesh.TransferTask(
                    transferId = finalTransferId,
                    packageId = finalTransfer.packageId,
                    state = com.hackx.ruraledtech.p2p.mesh.TransferState.FAILED,
                    progressPercent = 0,
                    endpointId = finalTransfer.endpointId,
                    expectedHash = finalTransfer.expectedHash,
                    actualHash = calculatedSha256,
                    installResult = "Verification Failed: Checksum Mismatch",
                    sizeBytes = finalTransfer.sizeBytes
                ))
                return@launch
            }

            Log.d(
                TAG,
                "[P2P][SHA256_VERIFIED] " +
                    finalTransfer.packageId
            )

            // Persist ZIP for store-and-forward
            val savedZip = packageStorageManager?.savePackageZip(finalTransfer.packageId, tempFile)
            Log.d(
                TAG,
                "[P2P][PACKAGE_STORE] " +
                    savedZip?.absolutePath
            )

            val installDir = File(
                tempFile.parentFile,
                "extracted_${finalTransfer.packageId}_${System.currentTimeMillis()}"
            )

            val extracted =
                packageStorageManager?.extractZip(
                    tempFile,
                    installDir
                ) ?: false

            if (!extracted) {
                Log.e(
                    TAG,
                    "[P2P][PACKAGE_EXTRACT_FAILED] " +
                        "Could not extract ZIP for " +
                        finalTransfer.packageId
                )

                tempFile.delete()

                _transfersMap.value =
                    _transfersMap.value +
                        (
                            finalTransferId to
                                com.hackx.ruraledtech.p2p.mesh.TransferTask(
                                    transferId = finalTransferId,
                                    packageId = finalTransfer.packageId,
                                    state = com.hackx.ruraledtech.p2p.mesh.TransferState.FAILED,
                                    progressPercent = 0
                                )
                        )

                return@launch
            }

            Log.d(
                TAG,
                "[P2P][PACKAGE_EXTRACTED] " +
                    installDir.absolutePath
            )

            val manifestFile = File(installDir, "manifest.json")

            if (!manifestFile.exists()) {
                Log.e(
                    TAG,
                    "[P2P][PACKAGE_INVALID] " +
                        "manifest.json missing from extracted package " +
                        finalTransfer.packageId
                )

                tempFile.delete()

                _transfersMap.value =
                    _transfersMap.value +
                        (
                            finalTransferId to
                                com.hackx.ruraledtech.p2p.mesh.TransferTask(
                                    transferId = finalTransferId,
                                    packageId = finalTransfer.packageId,
                                    state = com.hackx.ruraledtech.p2p.mesh.TransferState.FAILED,
                                    progressPercent = 0
                                )
                        )

                return@launch
            }

            Log.d(
                TAG,
                "[P2P][PACKAGE_INSTALL_START] " +
                    finalTransfer.packageId
            )

            val installResult = performInstall(installDir.absolutePath)

            if (installResult is InstallResult.Success) {
                Log.d(
                    TAG,
                    "[P2P][PACKAGE_INSTALL_SUCCESS] " +
                        finalTransfer.packageId
                )
                _transfersMap.value = _transfersMap.value + (finalTransferId to com.hackx.ruraledtech.p2p.mesh.TransferTask(
                    transferId = finalTransferId,
                    packageId = finalTransfer.packageId,
                    state = com.hackx.ruraledtech.p2p.mesh.TransferState.COMPLETED,
                    progressPercent = 100,
                    endpointId = finalTransfer.endpointId,
                    expectedHash = finalTransfer.expectedHash,
                    actualHash = calculatedSha256,
                    installResult = "Success",
                    sizeBytes = finalTransfer.sizeBytes
                ))
                onSuccess?.invoke(finalTransfer.packageId, finalTransfer.version, finalTransfer.expectedHash)
                onPackageInstalled?.invoke(finalTransfer.packageId, finalTransfer.version)
            } else {
                Log.e(
                    TAG,
                    "[P2P][PACKAGE_INSTALL_FAILED] " +
                        finalTransfer.packageId + " Reason: $installResult"
                )
                _transfersMap.value = _transfersMap.value + (finalTransferId to com.hackx.ruraledtech.p2p.mesh.TransferTask(
                    transferId = finalTransferId,
                    packageId = finalTransfer.packageId,
                    state = com.hackx.ruraledtech.p2p.mesh.TransferState.FAILED,
                    progressPercent = 0,
                    endpointId = finalTransfer.endpointId,
                    expectedHash = finalTransfer.expectedHash,
                    actualHash = calculatedSha256,
                    installResult = installResult.toString(),
                    sizeBytes = finalTransfer.sizeBytes
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

    private suspend fun performInstall(packagePath: String): InstallResult {
        if (domainContentInstaller != null) {
            return domainContentInstaller.install(packagePath)
        }
        if (legacyContentInstaller != null) {
            val success = legacyContentInstaller.install(packagePath)
            if (success) return InstallResult.Success("", 1)
        }
        return InstallResult.Failed("No ContentInstaller found or legacy failed")
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
