package com.hackx.ruraledtech.p2p.transfer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object PackageVerifier {
    
    /**
     * Hashes a file using an 8KB buffer on the IO Dispatcher.
     * Returns true if the calculated hash matches the expected hash.
     */
    suspend fun verifyFile(file: File, expectedHash: String): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false

        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192) // 8KB buffer
            
            FileInputStream(file).use { fis ->
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            
            val calculatedHash = digest.digest().joinToString("") { "%02x".format(it) }
            return@withContext calculatedHash.equals(expectedHash, ignoreCase = true)
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
