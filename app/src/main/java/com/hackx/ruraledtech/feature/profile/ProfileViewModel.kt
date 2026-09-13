package com.hackx.ruraledtech.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.data.local.dao.ClassGroupDao
import com.hackx.ruraledtech.data.local.entities.ClassGroupEntity
import com.hackx.ruraledtech.data.local.entities.ClassGroupLearnerEntity
import com.hackx.ruraledtech.data.remote.RuralEdTechApi
import com.hackx.ruraledtech.data.remote.dto.JoinClassRequestDto
import com.hackx.ruraledtech.domain.model.Learner
import com.hackx.ruraledtech.domain.repository.LearnerRepository
import com.hackx.ruraledtech.domain.usecase.learner.EnsureTtsVoiceInstalledUseCase
import com.hackx.ruraledtech.domain.usecase.learner.UpdateLearnerLanguageUseCase
import com.hackx.ruraledtech.feature.common.SupportedLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.hackx.ruraledtech.domain.voice.TextToSpeechEngine

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val currentLearnerManager: CurrentLearnerManager,
    private val updateLearnerLanguageUseCase: UpdateLearnerLanguageUseCase,
    private val ensureTtsVoiceInstalledUseCase: EnsureTtsVoiceInstalledUseCase,
    private val classGroupDao: ClassGroupDao,
    private val api: RuralEdTechApi,
    private val learnerRepository: LearnerRepository,
    private val textToSpeechEngine: TextToSpeechEngine,
    private val meshController: com.hackx.ruraledtech.p2p.mesh.MeshController,
) : ViewModel() {

    val learner: StateFlow<Learner?> = currentLearnerManager.currentLearnerId
        .filterNotNull()
        .flatMapLatest { id -> learnerRepository.observeLearners().map { list -> list.firstOrNull { it.learnerId == id } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val enrolledClasses: StateFlow<List<ClassGroupEntity>> = currentLearnerManager.currentLearnerId
        .filterNotNull()
        .flatMapLatest { id -> classGroupDao.observeClassesForLearner(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun switchProfile() {
        currentLearnerManager.clearSelection()
    }

    fun changeLanguage(language: SupportedLanguage) {
        viewModelScope.launch {
            val learnerId = currentLearnerManager.currentLearnerId.value
                ?: currentLearnerManager.currentLearnerId.filterNotNull().first()
            updateLearnerLanguageUseCase(learnerId, language.tag)
            ensureTtsVoiceInstalledUseCase(language.tag)
        }
    }

    fun testSpeak(language: SupportedLanguage) {
        viewModelScope.launch {
            textToSpeechEngine.stop()
            val sampleText = when (language.tag.lowercase()) {
                "hi" -> "नमस्ते! विद्यासारथी में आपका स्वागत है।"
                "mr" -> "नमस्कार! विद्यासारथी मध्ये आपले स्वागत आहे."
                "bn" -> "নমস্কার! বিদ্যাসারথিতে আপনাকে স্বাগতম।"
                "te" -> "నమస్కారం! విద್ಯಾసారథికి స్వాగతం."
                "ta" -> "வணக்கம்! வித்யாசாரதிக்கு வருக."
                "gu" -> "નમસ્તે! વિદ્યાસારથીમાં આપનું સ્વાગત છે."
                "kn" -> "ನಮಸ್ಕಾರ! ವಿದ್ಯಾಸಾರಥಿಗೆ ಸುಸ್ವಾಗತ."
                "ml" -> "നമസ്കാരം! വിദ്യാസാരഥിയിലേക്ക് സ്വാഗതം."
                "pa" -> "ਸਤਿ ਸ੍ਰੀ ਅਕਾਲ! ਵਿਦਿਆਸਾਰਥੀ ਵਿੱਚ ਤੁਹਾਡਾ ਸੁਆਗਤ ਹੈ।"
                "or" -> "ନମସ୍କାର! ବିଦ୍ୟାସାରଥିକୁ ଆପଣଙ୍କୁ ସ୍ୱାଗତ।"
                "as" -> "নমস্কাৰ! বিদ্যাসাৰথীলৈ আপোনাক স্বাগতম।"
                "ur" -> "آداب! ودیاسارتھی میں خوش آمدید۔"
                else -> "Hello! Welcome to VidyaSaarthi offline learning."
            }
            textToSpeechEngine.speak(sampleText, language.tag)
        }
    }

    fun openTtsSettings() {
        textToSpeechEngine.openTtsSettings()
    }

    fun joinClass(code: String, onResult: (success: Boolean, message: String) -> Unit) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) {
            onResult(false, "Please enter a class code")
            return
        }

        viewModelScope.launch {
            val currentLearner = learner.value
                ?: currentLearnerManager.currentLearnerId.value?.let { id -> learnerRepository.getLearner(id) }

            if (currentLearner == null) {
                onResult(false, "No active learner profile")
                return@launch
            }

            try {
                val response = api.joinClass(
                    JoinClassRequestDto(
                        code = cleanCode,
                        learner_id = currentLearner.learnerId,
                        name = currentLearner.name,
                        grade = currentLearner.grade.toString(),
                        preferred_language = currentLearner.preferredLanguage
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    val entity = ClassGroupEntity(
                        classId = dto.id,
                        joinCode = dto.join_code,
                        name = dto.name,
                        grade = dto.grade,
                        subject = dto.subject,
                        teacherId = dto.teacher_id ?: "",
                        lastSynced = System.currentTimeMillis()
                    )
                    classGroupDao.insert(entity)
                    classGroupDao.insertLearnerMapping(
                        ClassGroupLearnerEntity(dto.id, currentLearner.learnerId)
                    )
                    onResult(true, "Successfully joined ${dto.name}!")
                    return@launch
                } else if (response.code() == 404) {
                    onResult(false, "No class found matching code '$cleanCode'")
                    return@launch
                }
            } catch (_: Exception) {
                // Network error, try offline fallback
            }

            // If we're offline, check if we already have this class locally.
            // If yes, just map the learner to it. If not, fail.
            val localClass = classGroupDao.getByJoinCode(cleanCode)

            if (localClass != null) {
                classGroupDao.insertLearnerMapping(
                    ClassGroupLearnerEntity(localClass.classId, currentLearner.learnerId)
                )
                meshController.broadcastLearnerSync()
                onResult(true, "Joined ${localClass.name} (offline mode)")
            } else {
                onResult(false, "You are offline and this class is not known locally. Please connect to the internet to join for the first time.")
            }
        }
    }
}
