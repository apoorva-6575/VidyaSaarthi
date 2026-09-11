package com.hackx.ruraledtech.data.voice

import com.hackx.ruraledtech.domain.integration.VoiceIntent
import com.hackx.ruraledtech.domain.voice.VoiceIntentParser
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [VoiceIntentParser].
 * Supports deterministic matching across regional language command keywords (English, Hindi, Marathi, Tamil, Telugu).
 */
@Singleton
class DefaultVoiceIntentParser @Inject constructor() : VoiceIntentParser {

    override fun parse(text: String, language: String): VoiceIntent {
        val cleanText = text.lowercase().trim()
            .replace(Regex("[^\\p{L}\\p{Nd}\\s]"), "")

        if (cleanText.isEmpty()) return VoiceIntent.UNKNOWN

        return when {
            containsAny(cleanText, "start", "shuru", "shuru karo", "chalu", "aarambhi", "begin") -> VoiceIntent.START_LESSON
            containsAny(cleanText, "next", "aage", "pudhe", "adutha", "thadupari", "ahead", "agla") -> VoiceIntent.NEXT
            containsAny(cleanText, "back", "piche", "maage", "pinadi", "venukaku", "previous", "pichla") -> VoiceIntent.BACK
            containsAny(cleanText, "repeat", "dohrao", "phir se", "purnavartana", "again") -> VoiceIntent.REPEAT
            containsAny(cleanText, "explain", "samjhao", "spasht", "vivarikkavum", "meaning") -> VoiceIntent.EXPLAIN
            containsAny(cleanText, "help", "maddad", "sahayata", "udavi", "saayam") -> VoiceIntent.HELP
            containsAny(cleanText, "pause", "roko", "thambo", "niru", "stop") -> VoiceIntent.PAUSE
            containsAny(cleanText, "submit", "bhejo", "jama karo", "samarpikavum", "finish", "done") -> VoiceIntent.SUBMIT
            else -> VoiceIntent.UNKNOWN
        }
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        val words = text.split(Regex("\\s+"))
        return keywords.any { keyword ->
            if (keyword.contains(" ")) {
                text.contains(keyword)
            } else {
                words.contains(keyword)
            }
        }
    }
}
