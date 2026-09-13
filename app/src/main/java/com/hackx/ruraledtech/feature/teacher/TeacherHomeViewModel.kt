package com.hackx.ruraledtech.feature.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.connectivity.ConnectivityObserver
import com.hackx.ruraledtech.core.connectivity.ConnectivityState
import com.hackx.ruraledtech.core.datastore.PreferencesManager
import com.hackx.ruraledtech.core.datastore.TeacherAuthStore
import com.hackx.ruraledtech.data.local.dao.TeacherCacheDao
import com.hackx.ruraledtech.data.local.entities.LearnerEntity
import com.hackx.ruraledtech.data.local.entities.TeacherDashboardCacheEntity
import com.hackx.ruraledtech.data.remote.RuralEdTechApi
import com.hackx.ruraledtech.data.remote.dto.TeacherDashboardDto
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class TeacherDashboardSummary(
    val classesCount: Int,
    val learnersCount: Int,
    val conceptAverages: Map<String, Float>,
    val cachedAt: Long? = null,
) {
    val averageMastery: Float
        get() = if (conceptAverages.isEmpty()) 0f else conceptAverages.values.sum() / conceptAverages.size

    val weakConcepts: List<String>
        get() = conceptAverages.entries.sortedBy { it.value }.filter { it.value < 0.7f }.take(3).map { it.key }
}

data class TeacherHomeUiState(
    val teacherName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val dashboard: TeacherDashboardSummary? = null,
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
    private val teacherCacheDao: TeacherCacheDao,
    private val api: RuralEdTechApi,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherHomeUiState())
    val uiState: StateFlow<TeacherHomeUiState> = _uiState.asStateFlow()

    val teacherId: StateFlow<String?> = authStore.teacherId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {


        viewModelScope.launch {
            authStore.teacherName.collectLatest { name ->
                _uiState.value = _uiState.value.copy(teacherName = name ?: "")
            }
        }



        viewModelScope.launch {
            val currentTeacherId = authStore.currentTeacherId() ?: return@launch
            loadDashboardFromCache(currentTeacherId)
            refreshData()
        }
    }

    private suspend fun loadDashboardFromCache(teacherId: String) {
        val cached = teacherCacheDao.getDashboard(teacherId) ?: return
        _uiState.value = _uiState.value.copy(
            dashboard = TeacherDashboardSummary(
                classesCount = cached.classesCount,
                learnersCount = cached.learnersCount,
                conceptAverages = Json.decodeFromString(cached.conceptAveragesJson),
                cachedAt = cached.cachedAt,
            ),
        )
    }

    private suspend fun refreshDashboard(teacherId: String) {
        try {
            val response = api.getTeacherDashboard()
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                teacherCacheDao.upsertDashboard(
                    TeacherDashboardCacheEntity(
                        teacherId = teacherId,
                        classesCount = dto.classes_count,
                        learnersCount = dto.learners_count,
                        conceptAveragesJson = Json.encodeToString(dto.concept_averages),
                        cachedAt = System.currentTimeMillis(),
                    ),
                )
                _uiState.value = _uiState.value.copy(
                    dashboard = TeacherDashboardSummary(
                        classesCount = dto.classes_count,
                        learnersCount = dto.learners_count,
                        conceptAverages = dto.concept_averages,
                        cachedAt = null,
                    ),
                )
            }
        } catch (_: Exception) {
            // Dashboard refresh is best-effort; the class list refresh above already
            // surfaces a connectivity error, and cached dashboard data (if any) stays shown.
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            val currentTeacherId = authStore.currentTeacherId() ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                refreshDashboard(currentTeacherId)
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

    fun logOut(onDone: () -> Unit) {
        viewModelScope.launch {
            authStore.clearSession()
            onDone()
        }
    }
}
