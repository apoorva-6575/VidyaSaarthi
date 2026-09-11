package com.hackx.ruraledtech.data.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import com.hackx.ruraledtech.core.common.SecureLogger
import com.hackx.ruraledtech.domain.voice.TextToSpeechEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [TextToSpeechEngine] wrapping Android local TextToSpeech API.
 * Fails gracefully if TTS hardware or language voices are unavailable on low-end devices.
 */
@Singleton
class AndroidTextToSpeechEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : TextToSpeechEngine, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            SecureLogger.e("AndroidTTS", "Failed to instantiate Android TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
        } else {
            SecureLogger.e("AndroidTTS", "TTS Initialization failed with status $status")
        }
    }

    override suspend fun speak(text: String, languageTag: String): Boolean {
        if (!isInitialized || tts == null) {
            SecureLogger.d("AndroidTTS", "TTS not initialized, fallback silent: $text")
            return false
        }
        val locale = try {
            Locale.forLanguageTag(languageTag)
        } catch (e: Exception) {
            Locale.ENGLISH
        }
        tts?.language = locale
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_id_${System.currentTimeMillis()}")
        return result == TextToSpeech.SUCCESS
    }

    override suspend fun stop() {
        tts?.stop()
    }
}
