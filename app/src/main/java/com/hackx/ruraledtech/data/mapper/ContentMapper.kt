package com.hackx.ruraledtech.data.mapper

import com.hackx.ruraledtech.data.local.entities.ContentPackageEntity
import com.hackx.ruraledtech.data.local.entities.LessonEntity
import com.hackx.ruraledtech.data.local.entities.QuestionEntity
import com.hackx.ruraledtech.domain.model.ContentPackage
import com.hackx.ruraledtech.domain.model.ContentPackageState
import com.hackx.ruraledtech.domain.model.Lesson
import com.hackx.ruraledtech.domain.model.Question
import com.hackx.ruraledtech.domain.model.QuestionOption
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun LessonEntity.toDomain(): Lesson = Lesson(
    lessonId = lessonId,
    packageId = packageId,
    subject = subject,
    grade = grade,
    conceptId = conceptId,
    language = language,
    title = title,
    orderIndex = orderIndex,
    blocks = json.decodeFromString(ListSerializer(ContentBlockDto.serializer()), blocksJson).map { it.toDomain() },
)

fun Lesson.toEntity(): LessonEntity = LessonEntity(
    lessonId = lessonId,
    packageId = packageId,
    subject = subject,
    grade = grade,
    conceptId = conceptId,
    language = language,
    title = title,
    orderIndex = orderIndex,
    blocksJson = json.encodeToString(ListSerializer(ContentBlockDto.serializer()), blocks.map { it.toDto() }),
)

@kotlinx.serialization.Serializable
private data class QuestionOptionDto(val optionId: String, val text: String)

fun QuestionEntity.toDomain(): Question = Question(
    questionId = questionId,
    lessonId = lessonId,
    conceptId = conceptId,
    questionType = com.hackx.ruraledtech.domain.model.QuestionType.valueOf(questionType),
    language = language,
    prompt = prompt,
    options = json.decodeFromString(ListSerializer(QuestionOptionDto.serializer()), optionsJson)
        .map { QuestionOption(it.optionId, it.text) },
    correctOptionId = correctOptionId,
    difficulty = difficulty,
    explanation = explanation,
)

fun Question.toEntity(): QuestionEntity = QuestionEntity(
    questionId = questionId,
    lessonId = lessonId,
    conceptId = conceptId,
    questionType = questionType.name,
    language = language,
    prompt = prompt,
    optionsJson = json.encodeToString(
        ListSerializer(QuestionOptionDto.serializer()),
        options.map { QuestionOptionDto(it.optionId, it.text) },
    ),
    correctOptionId = correctOptionId,
    difficulty = difficulty,
    explanation = explanation,
)

fun ContentPackageEntity.toDomain(): ContentPackage = ContentPackage(
    packageId = packageId,
    version = version,
    subject = subject,
    grade = grade,
    languages = languages,
    sizeBytes = sizeBytes,
    checksum = checksum,
    installedAt = installedAt,
    state = ContentPackageState.valueOf(state),
    priority = priority,
)

fun ContentPackage.toEntity(): ContentPackageEntity = ContentPackageEntity(
    packageId = packageId,
    version = version,
    subject = subject,
    grade = grade,
    languages = languages,
    sizeBytes = sizeBytes,
    checksum = checksum,
    installedAt = installedAt,
    state = state.name,
    priority = priority,
)
