package com.hackx.ruraledtech.p2p.transfer

import android.net.Uri
import android.util.Log
import com.hackx.ruraledtech.p2p.connection.P2PConnectionManager
import com.hackx.ruraledtech.p2p.integration.ContentInstaller
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
    
    // Maps a payload ID to its expected SHA-256 hash
    private val expectedHashes = mutableMapOf<Long, String>()

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
    fun expectIncomingFile(payloadId: Long, expectedHash: String) {
        expectedHashes[payloadId] = expectedHash
        Log.d(TAG, "Expecting incoming payload $payloadId with hash$expectedHash")
    }

    /**
     * Called by MeshController when Nearby Connections finishes a file download.
     */
    fun onFileTransferComplete(endpointId: String, payloadId: Long, tempFile: File) {
        val expectedHash = expectedHashes.remove(payloadId)
        
        if (expectedHash == null) {
            Log.e(TAG, "Received unrequested file payload $payloadId. Deleting.")
            tempFile.delete()
            return
        }

        // Launch into background to verify and install
        coroutineScope.launch {
            Log.d(TAG, "Verifying downloaded package $payloadId...")
            val isValid = PackageVerifier.verifyFile(tempFile, expectedHash)

            if (isValid) {
                Log.d(TAG, "Verification SUCCESS. Handing off to Group 1 ContentInstaller.")
                val installSuccess = contentInstaller.install(tempFile.absolutePath)
                if (installSuccess) {
                    Log.d(TAG, "Package installed successfully!")
                    // TODO: Update our local ContentManifest to include this new package
                    // so we can Store-and-Forward it to the next peer!
                } else {
                    Log.e(TAG, "Group 1 ContentInstaller failed to install package.")
                }
            } else {
                Log.e(TAG, "Verification FAILED. File is corrupted or tampered with. Deleting.")
                tempFile.delete()
            }
        }
    }
}
