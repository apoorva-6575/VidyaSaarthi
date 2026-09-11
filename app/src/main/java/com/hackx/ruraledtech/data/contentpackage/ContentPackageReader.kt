package com.hackx.ruraledtech.data.contentpackage

import com.hackx.ruraledtech.data.contentpackage.dto.ChecksumsFileDto
import com.hackx.ruraledtech.data.contentpackage.dto.LessonFileDto
import com.hackx.ruraledtech.data.contentpackage.dto.ManifestDto
import com.hackx.ruraledtech.data.contentpackage.dto.QuestionFileDto
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

private val json = Json { ignoreUnknownKeys = true }

/**
 * Reads the standardised content-package directory format (PS section 11). This is the one
 * place that understands the on-disk layout; everything else in Group 1 only ever sees
 * domain models. Group 3 hands this class a directory path once P2P transfer completes.
 */
class ContentPackageReader @Inject constructor() {

    fun readManifest(packageDir: File): ManifestDto =
        json.decodeFromString(ManifestDto.serializer(), File(packageDir, "manifest.json").readText())

    fun readChecksums(packageDir: File): ChecksumsFileDto {
        val file = File(packageDir, "checksums.json")
        return if (file.exists()) json.decodeFromString(ChecksumsFileDto.serializer(), file.readText()) else ChecksumsFileDto(emptyMap())
    }

    fun readLessons(packageDir: File): List<LessonFileDto> {
        val dir = File(packageDir, "lessons")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "json" }.orEmpty()
            .sortedBy { it.name }
            .map { json.decodeFromString(LessonFileDto.serializer(), it.readText()) }
    }

    fun readQuestions(packageDir: File): List<QuestionFileDto> {
        val dir = File(packageDir, "questions")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "json" }.orEmpty()
            .sortedBy { it.name }
            .map { json.decodeFromString(QuestionFileDto.serializer(), it.readText()) }
    }

    /** SHA-256 over every lesson/question file's bytes, concatenated in stable (sorted) order. */
    fun computeContentChecksum(packageDir: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        listOf("lessons", "questions").forEach { sub ->
            File(packageDir, sub).listFiles { f -> f.extension == "json" }.orEmpty()
                .sortedBy { it.name }
                .forEach { digest.update(it.readBytes()) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
