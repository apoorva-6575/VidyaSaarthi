package com.hackx.ruraledtech.data.contentpackage

import com.hackx.ruraledtech.core.common.SecureLogger
import com.hackx.ruraledtech.data.contentpackage.dto.ContentBlockFileDto
import com.hackx.ruraledtech.data.local.dao.ContentPackageDao
import com.hackx.ruraledtech.data.local.dao.LessonDao
import com.hackx.ruraledtech.data.local.dao.QuestionDao
import com.hackx.ruraledtech.data.local.entities.LessonEntity
import com.hackx.ruraledtech.data.local.entities.QuestionEntity
import com.hackx.ruraledtech.data.mapper.ContentBlockDto
import com.hackx.ruraledtech.data.mapper.toEntity
import com.hackx.ruraledtech.domain.integration.ContentInstaller
import com.hackx.ruraledtech.domain.integration.InstallResult
import com.hackx.ruraledtech.domain.integration.ValidationResult
import com.hackx.ruraledtech.domain.model.ContentPackage
import com.hackx.ruraledtech.domain.model.ContentPackageState
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

private val json = Json { ignoreUnknownKeys = true }

/**
 * The seam described in PS section 37/51: Group 3 (P2P) and Group 4 (backend downloads)
 * call [install] with a local directory path once they've acquired the bytes by whatever
 * transport they own. Neither group touches Room directly — this class is the only writer
 * of content_packages/lessons/questions tables.
 */
class ContentInstallerImpl @Inject constructor(
    private val reader: ContentPackageReader,
    private val lessonDao: LessonDao,
    private val questionDao: QuestionDao,
    private val contentPackageDao: ContentPackageDao,
) : ContentInstaller {

    override suspend fun validate(packagePath: String): ValidationResult {
        val dir = File(packagePath)
        if (!dir.exists() || !File(dir, "manifest.json").exists()) {
            return ValidationResult.Invalid("manifest.json missing at $packagePath")
        }
        return try {
            val manifest = reader.readManifest(dir)
            ValidationResult.Valid(manifest.packageId, manifest.version, manifest.checksum)
        } catch (e: Exception) {
            SecureLogger.e("ContentInstaller", "Manifest parse failed for package at $packagePath", e)
            ValidationResult.Invalid(e.message ?: "manifest parse error")
        }
    }

    override suspend fun install(packagePath: String): InstallResult {
        val dir = File(packagePath)
        val manifest = try {
            reader.readManifest(dir)
        } catch (e: Exception) {
            return InstallResult.Failed("Could not read manifest: ${e.message}")
        }

        val actualChecksum = reader.computeContentChecksum(dir)
        if (actualChecksum != manifest.checksum) {
            return InstallResult.ChecksumMismatch(expected = manifest.checksum, actual = actualChecksum)
        }

        val lessonFiles = try {
            reader.readLessons(dir)
        } catch (e: Exception) {
            return InstallResult.Corrupted("Lesson JSON malformed: ${e.message}")
        }
        val questionFiles = try {
            reader.readQuestions(dir)
        } catch (e: Exception) {
            return InstallResult.Corrupted("Question JSON malformed: ${e.message}")
        }

        val lessonEntities = lessonFiles.map { lesson ->
            LessonEntity(
                lessonId = lesson.lessonId,
                packageId = manifest.packageId,
                subject = manifest.subject,
                grade = manifest.grade,
                conceptId = lesson.conceptId,
                language = lesson.language,
                title = lesson.title,
                orderIndex = lesson.orderIndex,
                blocksJson = json.encodeToString(
                    ListSerializer(ContentBlockDto.serializer()),
                    lesson.blocks.map { it.toContentBlockDto() },
                ),
                classId = lesson.classId ?: manifest.classId,
            )
        }
        val questionEntities = questionFiles.map { q ->
            QuestionEntity(
                questionId = q.questionId,
                lessonId = q.lessonId,
                conceptId = q.conceptId,
                questionType = q.questionType,
                language = q.language,
                prompt = q.prompt,
                optionsJson = json.encodeToString(
                    ListSerializer(com.hackx.ruraledtech.data.contentpackage.dto.QuestionOptionFileDto.serializer()),
                    q.options,
                ),
                correctOptionId = q.correctOptionId,
                difficulty = q.difficulty,
                explanation = q.explanation,
            )
        }

        lessonDao.insertAll(lessonEntities)
        questionDao.insertAll(questionEntities)
        contentPackageDao.upsert(
            ContentPackage(
                packageId = manifest.packageId,
                version = manifest.version,
                subject = manifest.subject,
                grade = manifest.grade,
                languages = manifest.languages,
                sizeBytes = manifest.size,
                checksum = manifest.checksum,
                installedAt = System.currentTimeMillis(),
                state = ContentPackageState.INSTALLED,
                priority = manifest.priority,
            ).toEntity(),
        )

        return InstallResult.Success(manifest.packageId, manifest.version)
    }

    override suspend fun remove(packageId: String, version: Int) {
        lessonDao.deleteForPackage(packageId)
        contentPackageDao.deletePackage(packageId)
    }
}

private fun ContentBlockFileDto.toContentBlockDto() = ContentBlockDto(
    blockId = blockId,
    type = type,
    body = body,
    assetPath = assetPath,
    altText = altText,
    transcript = transcript,
    prompt = prompt,
    explanation = explanation,
    message = message,
    tone = tone,
)
