package com.hackx.ruraledtech.p2p.storage

import android.content.Context
import com.hackx.ruraledtech.p2p.transfer.PackageVerifier
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Manages local disk storage of content package ZIP archives for store-and-forward P2P distribution.
 * Packages are persisted under internal storage (`filesDir/packages/{packageId}.zip`).
 */
class PackageStorageManager(private val baseDir: File) {

    constructor(context: Context) : this(File(context.filesDir, "packages"))

    init {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
    }

    fun getStoreDir(): File {
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        return baseDir
    }

    /** Returns the local ZIP file for an installed package if present. */
    fun getPackageZipFile(packageId: String): File? {
        val file = File(getStoreDir(), "$packageId.zip")
        return if (file.exists() && file.isFile && file.length() > 0) file else null
    }

    /** Saves an incoming ZIP file into local store-and-forward storage. */
    fun savePackageZip(packageId: String, sourceFile: File): File {
        val destFile = File(getStoreDir(), "$packageId.zip")
        if (destFile.exists()) {
            destFile.delete()
        }
        sourceFile.copyTo(destFile, overwrite = true)
        return destFile
    }

    /** Saves an input stream into local store-and-forward storage. */
    fun savePackageZipStream(packageId: String, inputStream: InputStream): File {
        val destFile = File(getStoreDir(), "$packageId.zip")
        if (destFile.exists()) {
            destFile.delete()
        }
        FileOutputStream(destFile).use { fos ->
            inputStream.copyTo(fos)
        }
        return destFile
    }

    /** Safely extracts a package ZIP archive into target directory, preventing zip slip attacks. */
    fun extractZip(zipFile: File, targetDir: File): Boolean {
        if (!zipFile.exists() || !zipFile.isFile) return false
        if (!targetDir.exists()) targetDir.mkdirs()

        return try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                val canonicalTargetDirPath = targetDir.canonicalPath

                while (entry != null) {
                    val newFile = File(targetDir, entry.name)
                    val canonicalDestinationPath = newFile.canonicalPath

                    // Zip Slip vulnerability check
                    if (!canonicalDestinationPath.startsWith(canonicalTargetDirPath + File.separator) &&
                        canonicalDestinationPath != canonicalTargetDirPath
                    ) {
                        throw SecurityException("Zip entry attempted path traversal: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Computes SHA-256 for a given file. */
    suspend fun computeSha256(file: File): String? {
        if (!file.exists()) return null
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
