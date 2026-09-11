package com.hackx.ruraledtech.data.contentpackage

import com.google.common.truth.Truth.assertThat
import com.hackx.ruraledtech.data.local.dao.ContentPackageDao
import com.hackx.ruraledtech.data.local.dao.LessonDao
import com.hackx.ruraledtech.data.local.dao.QuestionDao
import com.hackx.ruraledtech.data.local.entities.LessonEntity
import com.hackx.ruraledtech.data.local.entities.QuestionEntity
import com.hackx.ruraledtech.domain.integration.InstallResult
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Proves the two real demo content ZIPs (repo root `demo-content/`, mirrored here under
 * test resources) are genuinely installable through the app's actual production path —
 * extraction -> ContentPackageReader -> ContentInstallerImpl.install() — not just
 * hand-written JSON that looks plausible. This is what a P2P transfer or backend download
 * hands off to once bytes have arrived; if this test passes, the packages are real.
 */
class DemoContentPackageTest {

    private fun extractDemoZip(resourceName: String): File {
        val tempDir = kotlin.io.path.createTempDirectory("demo-content-test").toFile()
        val stream = javaClass.classLoader!!.getResourceAsStream("demo-content/$resourceName")
            ?: error("Missing test resource demo-content/$resourceName")
        ZipInputStream(stream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(tempDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return tempDir
    }

    private fun installerWithMocks(): Triple<ContentInstallerImpl, LessonDao, QuestionDao> {
        val lessonDao: LessonDao = mockk(relaxed = true)
        val questionDao: QuestionDao = mockk(relaxed = true)
        val contentPackageDao: ContentPackageDao = mockk(relaxed = true)
        val installer = ContentInstallerImpl(ContentPackageReader(), lessonDao, questionDao, contentPackageDao)
        return Triple(installer, lessonDao, questionDao)
    }

    @Test
    fun `math-grade5-decimals zip installs successfully with matching checksum and both languages`() = runTest {
        val dir = extractDemoZip("math-grade5-decimals.zip")
        val (installer, lessonDao, questionDao) = installerWithMocks()

        val lessonsSlot = slot<List<LessonEntity>>()
        val questionsSlot = slot<List<QuestionEntity>>()
        coEvery { lessonDao.insertAll(capture(lessonsSlot)) } returns Unit
        coEvery { questionDao.insertAll(capture(questionsSlot)) } returns Unit

        val result = installer.install(dir.absolutePath)

        assertThat(result).isInstanceOf(InstallResult.Success::class.java)
        result as InstallResult.Success
        assertThat(result.packageId).isEqualTo("math-grade5-decimals")

        val languages = lessonsSlot.captured.map { it.language }.toSet()
        assertThat(languages).containsExactly("en", "hi")
        assertThat(questionsSlot.captured).hasSize(5) // 3 English + 2 Hindi
    }

    @Test
    fun `science-grade5-water-cycle zip installs successfully with matching checksum and both languages`() = runTest {
        val dir = extractDemoZip("science-grade5-water-cycle.zip")
        val (installer, lessonDao, questionDao) = installerWithMocks()

        val lessonsSlot = slot<List<LessonEntity>>()
        val questionsSlot = slot<List<QuestionEntity>>()
        coEvery { lessonDao.insertAll(capture(lessonsSlot)) } returns Unit
        coEvery { questionDao.insertAll(capture(questionsSlot)) } returns Unit

        val result = installer.install(dir.absolutePath)

        assertThat(result).isInstanceOf(InstallResult.Success::class.java)
        result as InstallResult.Success
        assertThat(result.packageId).isEqualTo("science-grade5-water-cycle")

        val languages = lessonsSlot.captured.map { it.language }.toSet()
        assertThat(languages).containsExactly("en", "hi")
        assertThat(questionsSlot.captured).hasSize(5) // 3 English + 2 Hindi
    }

    @Test
    fun `a tampered demo package is rejected by checksum verification`() = runTest {
        val dir = extractDemoZip("math-grade5-decimals.zip")
        File(dir, "lessons/lesson_decimals_en.json").appendText(" ") // corrupt after extraction
        val (installer, _, _) = installerWithMocks()

        val result = installer.install(dir.absolutePath)

        assertThat(result).isInstanceOf(InstallResult.ChecksumMismatch::class.java)
    }
}
