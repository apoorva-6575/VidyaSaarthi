package com.hackx.ruraledtech.feature.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.connectivity.ConnectivityObserver
import com.hackx.ruraledtech.core.connectivity.ConnectivityState
import com.hackx.ruraledtech.core.datastore.PreferencesManager
import com.hackx.ruraledtech.core.datastore.TeacherAuthStore
import com.hackx.ruraledtech.data.local.dao.ClassGroupDao
import com.hackx.ruraledtech.data.local.dao.LearnerDao
import com.hackx.ruraledtech.data.local.entities.ClassGroupEntity
import com.hackx.ruraledtech.data.local.entities.ClassGroupLearnerEntity
import com.hackx.ruraledtech.data.local.entities.LearnerEntity
import com.hackx.ruraledtech.data.remote.RuralEdTechApi
import com.hackx.ruraledtech.data.remote.dto.ClassGroupCreateDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherHomeUiState(
    val teacherName: String = "",
    val isLoading: Boolean = false,
    val classes: List<ClassGroupEntity> = emptyList(),
    val isOffline: Boolean = false,
    val error: String? = null,
    val creatingClass: Boolean = false,
)

/**
 * [teacherId] used to be hardcoded to "teacher-123" before real login existed — now it
 * comes from [TeacherAuthStore], populated by [TeacherLoginViewModel] after a successful
 * login/register round trip.
 */
@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val authStore: TeacherAuthStore,
    private val classGroupDao: ClassGroupDao,
    private val learnerDao: LearnerDao,
    private val api: RuralEdTechApi,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherHomeUiState())
    val uiState: StateFlow<TeacherHomeUiState> = _uiState.asStateFlow()

    val teacherId: StateFlow<String?> = authStore.teacherId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            connectivityObserver.observe().collectLatest { state ->
                _uiState.value = _uiState.value.copy(isOffline = state == ConnectivityState.OFFLINE)
            }
        }

        viewModelScope.launch {
            authStore.teacherName.collectLatest { name ->
                _uiState.value = _uiState.value.copy(teacherName = name ?: "")
            }
        }

        viewModelScope.launch {
            authStore.teacherId.filterNotNull().flatMapLatest { classGroupDao.observeForTeacher(it) }.collectLatest { classes ->
                _uiState.value = _uiState.value.copy(classes = classes)
            }
        }
    }

    fun refreshData() {
        if (_uiState.value.isOffline) return

        viewModelScope.launch {
            val currentTeacherId = authStore.currentTeacherId() ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = api.getClasses()
                if (response.isSuccessful && response.body() != null) {
                    val classes = response.body()!!

                    val entities = classes.map { dto ->
                        ClassGroupEntity(
                            classId = dto.id,
                            name = dto.name,
                            grade = dto.grade,
                            subject = dto.subject,
                            teacherId = currentTeacherId,
                            lastSynced = System.currentTimeMillis()
                        )
                    }
                    classGroupDao.insertAll(entities)

                    classes.forEach { classDto ->
                        classDto.learners.forEach { learnerDto ->
                            val learnerEntity = LearnerEntity(
                                learnerId = learnerDto.id,
                                name = learnerDto.name,
                                grade = learnerDto.grade,
                                preferredLanguage = learnerDto.preferred_language,
                                avatarKey = learnerDto.avatar_key,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis(),
                                lastActiveAt = System.currentTimeMillis()
                            )
                            learnerDao.upsert(learnerEntity)
                            classGroupDao.insertLearnerMapping(
                                ClassGroupLearnerEntity(classDto.id, learnerDto.id)
                            )
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(error = "Failed to fetch classes")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun createClass(name: String, grade: String?, subject: String?, onDone: (success: Boolean) -> Unit) {
        if (_uiState.value.isOffline) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(creatingClass = true, error = null)
            try {
                val response = api.createClass(ClassGroupCreateDto(name = name, grade = grade, subject = subject))
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    val teacherId = authStore.currentTeacherId() ?: return@launch
                    classGroupDao.insert(
                        ClassGroupEntity(
                            classId = dto.id,
                            name = dto.name,
                            grade = dto.grade,
                            subject = dto.subject,
                            teacherId = teacherId,
                            lastSynced = System.currentTimeMillis(),
                        ),
                    )
                    _uiState.value = _uiState.value.copy(creatingClass = false)
                    onDone(true)
                } else {
                    _uiState.value = _uiState.value.copy(creatingClass = false, error = "Failed to create class")
                    onDone(false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(creatingClass = false, error = e.message)
                onDone(false)
            }
        }
    }

    fun switchToStudentRole(onDone: () -> Unit) {
        viewModelScope.launch {
            preferencesManager.setUserRole(null)
            onDone()
        }
    }

    fun logOut(onDone: () -> Unit) {
        viewModelScope.launch {
            authStore.clearSession()
            onDone()
        }
    }
}
