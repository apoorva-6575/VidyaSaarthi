package com.hackx.ruraledtech.domain.usecase.learner

import com.hackx.ruraledtech.domain.voice.TextToSpeechEngine
import javax.inject.Inject

/**
 * A learner picking a language (onboarding, a new profile, or Profile > Change language)
 * only gets working "Listen"/"Read aloud" buttons if that language's voice is actually
 * installed on the device. Android has no fully silent way to fetch missing voice data in
 * the background, so this checks availability and, if missing, opens the system's voice
 * data installer immediately instead of leaving the student to discover the silence later.
 */
class EnsureTtsVoiceInstalledUseCase @Inject constructor(
    private val textToSpeechEngine: TextToSpeechEngine,
) {
    suspend operator fun invoke(languageTag: String): Boolean {
        if (textToSpeechEngine.isLanguageAvailable(languageTag)) return true
        textToSpeechEngine.requestLanguageInstall(languageTag)
        return false
    }
}
