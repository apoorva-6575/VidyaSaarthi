package com.hackx.ruraledtech.feature.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.datastore.PreferencesManager
import com.hackx.ruraledtech.core.connectivity.ConnectivityObserver
import com.hackx.ruraledtech.core.connectivity.ConnectivityState
import com.hackx.ruraledtech.data.local.dao.ClassGroupDao
import com.hackx.ruraledtech.data.local.dao.LearnerDao
import com.hackx.ruraledtech.data.local.entities.ClassGroupEntity
import com.hackx.ruraledtech.data.local.entities.ClassGroupLearnerEntity
import com.hackx.ruraledtech.data.local.entities.LearnerEntity
import com.hackx.ruraledtech.data.remote.RuralEdTechApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeacherHomeUiState(
    val isLoading: Boolean = false,
    val classes: List<ClassGroupEntity> = emptyList(),
    val isOffline: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val classGroupDao: ClassGroupDao,
    private val learnerDao: LearnerDao,
    private val api: RuralEdTechApi,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherHomeUiState())
    val uiState: StateFlow<TeacherHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.observe().collectLatest { state ->
                _uiState.value = _uiState.value.copy(isOffline = state == ConnectivityState.OFFLINE)
            }
        }
        
        viewModelScope.launch {
            val teacherId = "teacher-123" // In real app, this comes from auth
            classGroupDao.observeForTeacher(teacherId).collectLatest { classes ->
                _uiState.value = _uiState.value.copy(classes = classes)
            }
        }
    }
    
    fun refreshData() {
        if (_uiState.value.isOffline) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = api.getClasses()
                if (response.isSuccessful && response.body() != null) {
                    val classes = response.body()!!
                    val teacherId = "teacher-123"
                    
                    val entities = classes.map { dto ->
                        ClassGroupEntity(
                            classId = dto.id,
                            name = dto.name,
                            grade = dto.grade,
                            subject = dto.subject,
                            teacherId = teacherId,
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

    fun switchToStudentRole(onDone: () -> Unit) {
        viewModelScope.launch {
            preferencesManager.setUserRole(null)
            onDone()
        }
    }
}
