package com.hackx.ruraledtech.domain.voice

/**
 * Offline Text-To-Speech abstraction for educational voice output in regional languages.
 */
interface TextToSpeechEngine {
    suspend fun speak(text: String, languageTag: String = "en"): Boolean
    suspend fun stop()
}
