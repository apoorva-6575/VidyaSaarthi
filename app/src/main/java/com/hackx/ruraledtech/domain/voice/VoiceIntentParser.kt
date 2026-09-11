package com.hackx.ruraledtech.domain.voice

import com.hackx.ruraledtech.domain.integration.VoiceIntent

/**
 * Parses spoken or recognized text input deterministically into structured [VoiceIntent]s.
 */
interface VoiceIntentParser {
    fun parse(text: String, language: String = "en"): VoiceIntent
}
