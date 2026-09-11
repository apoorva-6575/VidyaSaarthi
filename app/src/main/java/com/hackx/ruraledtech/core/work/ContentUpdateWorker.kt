package com.hackx.ruraledtech.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Placeholder hook for Group 4's backend content-update checks (PS section 17/19: GET
 * /content/manifest, GET /content/{package_id}). Content installation itself always goes
 * through ContentInstaller so this worker, P2P, and manual downloads share one code path.
 */
@HiltWorker
class ContentUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: com.hackx.ruraledtech.data.remote.RuralEdTechApi,
    private val contentDao: com.hackx.ruraledtech.data.local.dao.ContentPackageDao,
    private val contentInstaller: com.hackx.ruraledtech.p2p.integration.ContentInstaller
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val response = api.getAvailableContent()
            if (!response.isSuccessful || response.body() == null) {
                return Result.retry()
            }

            val availablePackages = response.body()!!
            
            for (pkg in availablePackages) {
                val latestLocal = contentDao.getLatest(pkg.id)
                // If we don't have it, or ours is older
                if (latestLocal == null || latestLocal.version < pkg.version) {
                    val success = downloadAndInstall(pkg)
                    if (!success) {
                        // Log but continue trying others
                    }
                }
            }
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private suspend fun downloadAndInstall(pkg: com.hackx.ruraledtech.data.remote.dto.ContentPackageDto): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var tempFile: java.io.File? = null
            var extractDir: java.io.File? = null
            try {
                val downloadResp = api.downloadContent(pkg.id, pkg.version)
                if (!downloadResp.isSuccessful || downloadResp.body() == null) {
                    return@withContext false
                }

                // Save to temp file
                tempFile = java.io.File(applicationContext.cacheDir, "pkg_${pkg.id}_${pkg.version}.zip")
                downloadResp.body()!!.byteStream().use { input ->
                    java.io.FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Verify Checksum
                val isHashValid = com.hackx.ruraledtech.p2p.transfer.PackageVerifier.verifyFile(tempFile, pkg.checksum)
                if (!isHashValid) {
                    tempFile.delete()
                    return@withContext false
                }
                
                // Safely extract ZIP
                extractDir = java.io.File(applicationContext.cacheDir, "pkg_extract_${pkg.id}_${pkg.version}")
                if (extractDir.exists()) {
                    extractDir.deleteRecursively()
                }
                extractDir.mkdirs()
                
                java.util.zip.ZipFile(tempFile).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        val entryFile = java.io.File(extractDir, entry.name)
                        // Path traversal check
                        if (!entryFile.canonicalPath.startsWith(extractDir.canonicalPath + java.io.File.separator)) {
                            throw SecurityException("Zip Path Traversal Vulnerability")
                        }
                        if (entry.isDirectory) {
                            entryFile.mkdirs()
                        } else {
                            entryFile.parentFile?.mkdirs()
                            zip.getInputStream(entry).use { input ->
                                java.io.FileOutputStream(entryFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }

                // Install from extracted directory
                val installed = contentInstaller.install(extractDir.absolutePath)
                return@withContext installed
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            } finally {
                tempFile?.delete()
                extractDir?.deleteRecursively()
            }
        }
    }
}

