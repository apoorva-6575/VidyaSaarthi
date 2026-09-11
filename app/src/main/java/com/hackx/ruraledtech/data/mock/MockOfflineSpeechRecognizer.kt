package com.hackx.ruraledtech.data.mock

import com.hackx.ruraledtech.domain.integration.OfflineSpeechRecognizer
import com.hackx.ruraledtech.domain.integration.VoiceIntent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder for Group 2's Vosk/ONNX-backed recognizer (PS section 9/27). Group 1's
 * VoiceController and UI wire against [OfflineSpeechRecognizer] only, so swapping this for
 * the real implementation is a one-line DI change. [emitIntentForTesting] lets the UI/demo
 * simulate a voice command without a working microphone pipeline.
 */
@Singleton
class MockOfflineSpeechRecognizer @Inject constructor() : OfflineSpeechRecognizer {

    private val intents = MutableSharedFlow<VoiceIntent>(extraBufferCapacity = 1)

    override fun listen(languageTag: String): Flow<VoiceIntent> = intents.asSharedFlow()

    override fun stop() = Unit

    fun emitIntentForTesting(intent: VoiceIntent) {
        intents.tryEmit(intent)
    }
}
