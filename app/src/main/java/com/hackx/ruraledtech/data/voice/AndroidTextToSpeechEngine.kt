package com.hackx.ruraledtech.data.voice

import android.content.Context
import android.content.Intent
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
    @Volatile private var isInitialized = false

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            SecureLogger.e("AndroidTTS", "Failed to instantiate Android TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setSpeechRate(0.88f)
            tts?.setPitch(1.0f)
        } else {
            SecureLogger.e("AndroidTTS", "TTS Initialization failed with status $status")
        }
    }

    private fun getLocaleForLanguage(lang: String): Locale = when (lang.lowercase().trim()) {
        "hi" -> Locale("hi", "IN")
        "mr" -> Locale("mr", "IN")
        "bn" -> Locale("bn", "IN")
        "te" -> Locale("te", "IN")
        "ta" -> Locale("ta", "IN")
        "gu" -> Locale("gu", "IN")
        "kn" -> Locale("kn", "IN")
        "ml" -> Locale("ml", "IN")
        "pa" -> Locale("pa", "IN")
        "or" -> Locale("or", "IN")
        "as" -> Locale("as", "IN")
        "ur" -> Locale("ur", "IN")
        "en" -> Locale.ENGLISH
        else -> try { Locale.forLanguageTag(lang) } catch (_: Exception) { Locale.ENGLISH }
    }

    override suspend fun speak(text: String, languageTag: String): Boolean {
        if (tts == null) {
            initTts()
        }
        if (!isInitialized) {
            // Give TTS engine up to 600ms to finish initializing if just started
            var waited = 0
            while (!isInitialized && waited < 600) {
                kotlinx.coroutines.delay(100)
                waited += 100
            }
        }
        if (!isInitialized || tts == null) {
            SecureLogger.d("AndroidTTS", "TTS not initialized, fallback silent: $text")
            return false
        }

        val primaryLocale = getLocaleForLanguage(languageTag)
        val resultLang = tts?.setLanguage(primaryLocale)
        if (resultLang == TextToSpeech.LANG_MISSING_DATA || resultLang == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback: Hindi (India) or English (US) so learner always hears audio feedback
            val fallbackLocale = if (languageTag != "en") Locale("hi", "IN") else Locale.US
            val fallbackResult = tts?.setLanguage(fallbackLocale)
            if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
        }

        tts?.setSpeechRate(0.88f)
        tts?.setPitch(1.0f)

        val params = android.os.Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "tts_id_${System.currentTimeMillis()}")
        return result == TextToSpeech.SUCCESS
    }

    override suspend fun stop() {
        tts?.stop()
    }

    override suspend fun isLanguageAvailable(languageTag: String): Boolean {
        val engine = tts ?: return false
        val locale = getLocaleForLanguage(languageTag)
        return engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
    }

    override fun requestLanguageInstall(languageTag: String) {
        try {
            val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openTtsSettings()
        }
    }

    override fun openTtsSettings() {
        val intents = listOf(
            // 1. Direct TTS settings
            Intent("com.android.settings.TTS_SETTINGS"),
            // 2. Google TTS Voice Install Action directed at Google TTS package
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
                setPackage("com.google.android.tts")
            },
            // 3. Generic Action Install TTS Data
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            // 4. Google Speech Services settings activity
            Intent().apply {
                setClassName("com.google.android.tts", "com.google.android.tts.settings.GoogleTtsSettingsActivity")
            },
            // 5. System Locale Settings
            Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS),
            // 6. Play Store page for Google Speech Services
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=com.google.android.tts")),
            // 7. Play Store web fallback
            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts")),
            // 8. General Android Settings
            Intent(android.provider.Settings.ACTION_SETTINGS),
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Try next intent
            }
        }
        SecureLogger.e("AndroidTTS", "Could not open any TTS or system settings activity")
    }
}
