package com.hackx.ruraledtech.p2p.storage

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PackageStorageManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storageDir: File
    private lateinit var storageManager: PackageStorageManager

    @Before
    fun setUp() {
        storageDir = tempFolder.newFolder("packages_test")
        storageManager = PackageStorageManager(storageDir)
    }

    @Test
    fun `savePackageZip and getPackageZipFile persists package zip for store-and-forward`() {
        val sampleFile = tempFolder.newFile("dummy.zip")
        sampleFile.writeText("test package content")

        val savedFile = storageManager.savePackageZip("pkg_math_1", sampleFile)

        assertThat(savedFile.exists()).isTrue()
        assertThat(savedFile.name).isEqualTo("pkg_math_1.zip")

        val retrievedFile = storageManager.getPackageZipFile("pkg_math_1")
        assertThat(retrievedFile).isNotNull()
        assertThat(retrievedFile?.readText()).isEqualTo("test package content")
    }

    @Test
    fun `computeSha256 produces deterministic SHA-256 hash for package`() = runTest {
        val sampleFile = tempFolder.newFile("sample.zip")
        sampleFile.writeText("Hello World!")

        val hash = storageManager.computeSha256(sampleFile)
        assertThat(hash).isNotNull()
        // SHA-256 of "Hello World!" is "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069"
        assertThat(hash?.lowercase()).isEqualTo("7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069")
    }

    @Test
    fun `extractZip extracts valid zip entries safely into target directory`() {
        val zipFile = tempFolder.newFile("valid.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write("""{"package_id": "pkg_test", "version": 1}""".toByteArray())
            zos.closeEntry()
        }

        val extractTarget = tempFolder.newFolder("extracted")
        val result = storageManager.extractZip(zipFile, extractTarget)

        assertThat(result).isTrue()
        val extractedManifest = File(extractTarget, "manifest.json")
        assertThat(extractedManifest.exists()).isTrue()
        assertThat(extractedManifest.readText()).contains("pkg_test")
    }
}
