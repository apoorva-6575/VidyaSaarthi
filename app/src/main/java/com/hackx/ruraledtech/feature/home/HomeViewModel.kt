package com.hackx.ruraledtech.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.connectivity.ConnectivityState
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.data.local.dao.ClassGroupDao
import com.hackx.ruraledtech.data.local.dao.LessonDao
import com.hackx.ruraledtech.data.local.entities.ClassGroupEntity
import com.hackx.ruraledtech.data.local.entities.LessonEntity
import com.hackx.ruraledtech.domain.model.Learner
import com.hackx.ruraledtech.domain.model.Recommendation
import com.hackx.ruraledtech.domain.model.SubjectProgress
import com.hackx.ruraledtech.domain.repository.LearnerRepository
import com.hackx.ruraledtech.domain.usecase.progress.ObserveSubjectProgressUseCase
import com.hackx.ruraledtech.domain.usecase.recommendation.ObserveRecommendationUseCase
import com.hackx.ruraledtech.domain.usecase.sync.ObserveConnectivityUseCase
import com.hackx.ruraledtech.domain.usecase.sync.ObservePendingSyncCountUseCase
import com.hackx.ruraledtech.domain.voice.TextToSpeechEngine
import com.hackx.ruraledtech.feature.common.UiStrings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A class group plus the lessons that belong to it. */
data class ClassWithLessons(
    val classGroup: ClassGroupEntity,
    val lessons: List<LessonEntity>,
)

data class HomeUiState(
    val learner: Learner? = null,
    val allLearners: List<Learner> = emptyList(),
    val connectivity: ConnectivityState = ConnectivityState.OFFLINE,
    val subjectProgress: List<SubjectProgress> = emptyList(),
    val recommendation: Recommendation? = null,
    val pendingSyncCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val currentLearnerManager: CurrentLearnerManager,
    private val learnerRepository: LearnerRepository,
    private val textToSpeechEngine: TextToSpeechEngine,
    private val lessonDao: LessonDao,
    private val classGroupDao: ClassGroupDao,
    observeSubjectProgressUseCase: ObserveSubjectProgressUseCase,
    observeRecommendationUseCase: ObserveRecommendationUseCase,
    observeConnectivityUseCase: ObserveConnectivityUseCase,
    observePendingSyncCountUseCase: ObservePendingSyncCountUseCase,
) : ViewModel() {

    private val learnerId = currentLearnerManager.currentLearnerId.filterNotNull()

    /**
     * Lessons grouped by the learner's enrolled classes.
     * Each item = one class + all lessons tagged with that classId.
     */
    val classLessons: StateFlow<List<ClassWithLessons>> = learnerId
        .flatMapLatest { id ->
            classGroupDao.observeClassesForLearner(id).flatMapLatest { classes ->
                if (classes.isEmpty()) {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    // Combine a lesson-flow for every enrolled class
                    val lessonFlows = classes.map { cls ->
                        lessonDao.observeByClassId(cls.classId).map { lessons ->
                            ClassWithLessons(cls, lessons)
                        }
                    }
                    combine(lessonFlows) { it.toList() }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** General lessons not tied to any class (available to all learners). */
    val generalLessons: StateFlow<List<LessonEntity>> = lessonDao.observeUnassigned()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allLearnersFlow = learnerRepository.observeLearners()
    private val learnerFlow = learnerId.flatMapLatest { id ->
        allLearnersFlow.map { list -> list.firstOrNull { it.learnerId == id } }
    }
    private val subjectProgressFlow = learnerId.flatMapLatest { observeSubjectProgressUseCase(it) }
    private val recommendationFlow = learnerId.flatMapLatest { observeRecommendationUseCase(it) }

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val coreInfoFlow = combine(learnerFlow, allLearnersFlow, observeConnectivityUseCase()) { learner, allLearners, connectivity ->
        Triple(learner, allLearners, connectivity)
    }

    val uiState = combine(
        coreInfoFlow,
        subjectProgressFlow,
        recommendationFlow,
        observePendingSyncCountUseCase(),
    ) { (learner, allLearners, connectivity), subjects, recommendation, pending ->
        HomeUiState(learner, allLearners, connectivity, subjects, recommendation, pending)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun switchLearner(newLearnerId: String) {
        viewModelScope.launch {
            stopSpeech()
            currentLearnerManager.selectLearner(newLearnerId)
        }
    }

    fun playAudioGuide() {
        val learner = uiState.value.learner
        val lang = learner?.preferredLanguage ?: "en"
        val name = learner?.name ?: "Student"
        val text = UiStrings.homeAudioGuide(name, lang)
        viewModelScope.launch {
            _isSpeaking.value = true
            textToSpeechEngine.speak(text, lang)
        }
    }

    fun stopSpeech() {
        _isSpeaking.value = false
        viewModelScope.launch {
            textToSpeechEngine.stop()
        }
    }
}

