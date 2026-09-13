package com.hackx.ruraledtech.domain.model

data class Lesson(
    val lessonId: String,
    val packageId: String,
    val subject: String,
    val grade: Int,
    val conceptId: String,
    val language: String,
    val title: String,
    val orderIndex: Int,
    val blocks: List<ContentBlock>,
)

sealed class ContentBlock {
    abstract val blockId: String

    data class Text(override val blockId: String, val body: String) : ContentBlock()

    data class Image(override val blockId: String, val assetPath: String, val altText: String) : ContentBlock()

    data class Audio(override val blockId: String, val assetPath: String, val transcript: String?) : ContentBlock()

    data class Video(override val blockId: String, val assetPath: String) : ContentBlock()

    /** A source document (PDF/PPT/etc.) attached to the lesson, opened via the device's own viewer app. */
    data class Document(override val blockId: String, val assetPath: String, val title: String) : ContentBlock()

    data class Example(override val blockId: String, val prompt: String, val explanation: String) : ContentBlock()

    data class Callout(override val blockId: String, val message: String, val tone: CalloutTone) : ContentBlock()

    data class Unsupported(override val blockId: String, val rawType: String) : ContentBlock()
}

enum class CalloutTone { INFO, WARNING, TIP }
