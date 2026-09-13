package com.hackx.ruraledtech.data.mapper

import com.hackx.ruraledtech.domain.model.CalloutTone
import com.hackx.ruraledtech.domain.model.ContentBlock
import kotlinx.serialization.Serializable

/**
 * Wire/storage shape for [ContentBlock]. Kept separate from the domain sealed class so the
 * on-disk/manifest JSON format (owned jointly with Group 2's content package spec, PS
 * section 11) can evolve independently of in-app rendering logic.
 */
@Serializable
data class ContentBlockDto(
    val blockId: String,
    val type: String,
    val body: String? = null,
    val assetPath: String? = null,
    val altText: String? = null,
    val transcript: String? = null,
    val prompt: String? = null,
    val explanation: String? = null,
    val message: String? = null,
    val tone: String? = null,
)

fun ContentBlock.toDto(): ContentBlockDto = when (this) {
    is ContentBlock.Text -> ContentBlockDto(blockId, "TEXT", body = body)
    is ContentBlock.Image -> ContentBlockDto(blockId, "IMAGE", assetPath = assetPath, altText = altText)
    is ContentBlock.Audio -> ContentBlockDto(blockId, "AUDIO", assetPath = assetPath, transcript = transcript)
    is ContentBlock.Video -> ContentBlockDto(blockId, "VIDEO", assetPath = assetPath)
    is ContentBlock.Document -> ContentBlockDto(blockId, "DOCUMENT", assetPath = assetPath, altText = title)
    is ContentBlock.Example -> ContentBlockDto(blockId, "EXAMPLE", prompt = prompt, explanation = explanation)
    is ContentBlock.Callout -> ContentBlockDto(blockId, "CALLOUT", message = message, tone = tone.name)
    is ContentBlock.Unsupported -> ContentBlockDto(blockId, rawType)
}

fun ContentBlockDto.toDomain(): ContentBlock = when (type) {
    "TEXT" -> ContentBlock.Text(blockId, body.orEmpty())
    "IMAGE" -> ContentBlock.Image(blockId, assetPath.orEmpty(), altText.orEmpty())
    "AUDIO" -> ContentBlock.Audio(blockId, assetPath.orEmpty(), transcript)
    "VIDEO" -> ContentBlock.Video(blockId, assetPath.orEmpty())
    "DOCUMENT" -> ContentBlock.Document(blockId, assetPath.orEmpty(), altText ?: "Document")
    "EXAMPLE" -> ContentBlock.Example(blockId, prompt.orEmpty(), explanation.orEmpty())
    "CALLOUT" -> ContentBlock.Callout(blockId, message.orEmpty(), runCatching { CalloutTone.valueOf(tone.orEmpty()) }.getOrDefault(CalloutTone.INFO))
    else -> ContentBlock.Unsupported(blockId, type)
}
