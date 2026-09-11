package com.hackx.ruraledtech.domain.voice

/**
 * Offline Text-To-Speech abstraction for educational voice output in regional languages.
 */
interface TextToSpeechEngine {
    suspend fun speak(text: String, languageTag: String = "en"): Boolean
    suspend fun stop()

    /** True if a voice for [languageTag] is already installed on-device and usable offline. */
    suspend fun isLanguageAvailable(languageTag: String): Boolean

    /**
     * Opens the system's "install voice data" screen for [languageTag]. Android does not allow
     * a silent, fully automatic background download of TTS voice data — this is the closest
     * equivalent: it takes the user straight to the download prompt instead of making them find
     * it themselves under Settings > Accessibility > Text-to-speech. Requires connectivity to
     * actually complete the download.
     */
    fun requestLanguageInstall(languageTag: String)
}
