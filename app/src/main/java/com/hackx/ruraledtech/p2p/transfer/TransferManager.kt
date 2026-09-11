package com.hackx.ruraledtech.p2p.transfer

import android.net.Uri
import android.util.Log
import com.hackx.ruraledtech.p2p.connection.P2PConnectionManager
import com.hackx.ruraledtech.domain.integration.ContentInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class TransferManager(
    private val connectionManager: P2PConnectionManager,
    private val contentInstaller: ContentInstaller,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val TAG = "TransferManager"
    
    data class ExpectedFile(val packageId: String, val version: Int, val expectedHash: String)
    
    // Maps a payload ID to its expected file info
    private val expectedFiles = mutableMapOf<Long, ExpectedFile>()
    
    // Callback when a package is successfully verified and installed
    var onPackageInstalled: ((String, Int) -> Unit)? = null

    /**
     * Called by MeshController when we agree to send a file.
     */
    fun sendContentPackage(endpointId: String, fileUri: Uri) {
        Log.d(TAG, "Starting file transfer to $endpointId")
        connectionManager.sendFile(endpointId, fileUri)
    }

    /**
     * Called by MeshController when it expects an incoming file.
     */
    fun expectIncomingFile(payloadId: Long, packageId: String, version: Int, expectedHash: String) {
        expectedFiles[payloadId] = ExpectedFile(packageId, version, expectedHash)
        Log.d(TAG, "Expecting incoming payload $payloadId with hash $expectedHash")
    }

    /**
     * Called by MeshController when Nearby Connections finishes a file download.
     */
    fun onFileTransferComplete(endpointId: String, payloadId: Long, tempFile: File) {
        val expectedFile = expectedFiles.remove(payloadId)
        
        if (expectedFile == null) {
            Log.e(TAG, "Received unrequested file payload $payloadId. Deleting.")
            tempFile.delete()
            return
        }

        // Launch into background to verify and install
        coroutineScope.launch {
            Log.d(TAG, "Verifying downloaded package $payloadId...")
            val isValid = PackageVerifier.verifyFile(tempFile, expectedFile.expectedHash)

            if (isValid) {
                Log.d(TAG, "Verification SUCCESS. Extracting ZIP to handoff to Group 1.")
                val extractDir = File(tempFile.parentFile, "extracted_${payloadId}")
                var extractionSuccess = false
                try {
                    extractDir.mkdirs()
                    java.util.zip.ZipInputStream(java.io.FileInputStream(tempFile)).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val canonicalDestPath = File(extractDir, entry.name).canonicalPath
                            if (!canonicalDestPath.startsWith(extractDir.canonicalPath + File.separator)) {
                                throw SecurityException("Path traversal vulnerability detected in ZIP!")
                            }
                            if (entry.isDirectory) {
                                File(canonicalDestPath).mkdirs()
                            } else {
                                File(canonicalDestPath).parentFile?.mkdirs()
                                java.io.FileOutputStream(canonicalDestPath).use { fos ->
                                    zis.copyTo(fos)
                                }
                            }
                            entry = zis.nextEntry
                        }
                    }
                    extractionSuccess = true
                } catch (e: Exception) {
                    Log.e(TAG, "Extraction failed", e)
                } finally {
                    tempFile.delete() // Always delete the original zip after extraction attempt
                }

                if (extractionSuccess) {
                    Log.d(TAG, "Extraction SUCCESS. Handoff to ContentInstaller.")
                    val installResult = contentInstaller.install(extractDir.absolutePath)
                    when (installResult) {
                        is com.hackx.ruraledtech.domain.integration.InstallResult.Success -> {
                            Log.d(TAG, "Package installed successfully!")
                            // Update our local ContentManifest to include this new package
                            onPackageInstalled?.invoke(expectedFile.packageId, expectedFile.version)
                        } else -> {
                            Log.e(TAG, "Group 1 ContentInstaller failed to install package: $installResult")
                        }
                    }
                    // Clean up extracted directory
                    extractDir.deleteRecursively()
                } else {
                    extractDir.deleteRecursively()
                }
            } else {
                Log.e(TAG, "Verification FAILED. File is corrupted or tampered with. Deleting.")
                tempFile.delete()
            }
        }
    }

    /**
     * Called by MeshController when a file transfer fails or is canceled.
     */
    fun onFileTransferFailed(payloadId: Long) {
        expectedFiles.remove(payloadId)
        Log.d(TAG, "Transfer failed for payload $payloadId. Cleaned up expected file tracking.")
    }
}
