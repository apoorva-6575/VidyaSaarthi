package com.hackx.ruraledtech.domain.integration

import kotlinx.coroutines.flow.Flow

/** Owned by Group 2. Group 1 ships a mock so voice-triggered navigation can be wired early. */
interface OfflineSpeechRecognizer {
    fun listen(languageTag: String): Flow<VoiceIntent>
    fun stop()
}

enum class VoiceIntent {
    START_LESSON, NEXT, BACK, REPEAT, EXPLAIN, HELP, PAUSE, SUBMIT, UNKNOWN
}
