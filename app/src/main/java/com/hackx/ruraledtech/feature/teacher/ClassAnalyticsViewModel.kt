package com.hackx.ruraledtech.feature.teacher

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.connectivity.ConnectivityObserver
import com.hackx.ruraledtech.core.connectivity.ConnectivityState
import com.hackx.ruraledtech.data.local.dao.ClassGroupDao
import com.hackx.ruraledtech.data.local.dao.TeacherCacheDao
import com.hackx.ruraledtech.data.local.entities.ClassAnalyticsCacheEntity
import com.hackx.ruraledtech.data.local.entities.LearnerEntity
import com.hackx.ruraledtech.data.remote.RuralEdTechApi
import com.hackx.ruraledtech.data.remote.dto.ClassAnalyticsDto
import com.hackx.ruraledtech.data.remote.dto.LearnerMetricsDto
import com.hackx.ruraledtech.feature.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ClassAnalyticsUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val error: String? = null,
    val classAverages: Map<String, Float> = emptyMap(),
    val learnerRows: List<LearnerAnalyticsRow> = emptyList(),
    val cachedAt: Long? = null,
)

data class LearnerAnalyticsRow(
    val learnerId: String,
    val name: String,
    val metrics: LearnerMetricsDto?,
)

/** Per-class breakdown (PS section 6.6's "how is *this* class doing", not the whole-account lump from /teacher/dashboard). */
@HiltViewModel
class ClassAnalyticsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: RuralEdTechApi,
    private val classGroupDao: ClassGroupDao,
    private val teacherCacheDao: TeacherCacheDao,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val classId: String = checkNotNull(savedStateHandle[Routes.ARG_CLASS_ID])

    private val _uiState = MutableStateFlow(ClassAnalyticsUiState())
    val uiState: StateFlow<ClassAnalyticsUiState> = _uiState

    private var cachedLearners: List<LearnerEntity> = emptyList()

    init {
        viewModelScope.launch {
            classGroupDao.observeLearnersForClass(classId).collectLatest { learners ->
                cachedLearners = learners
                rebuildRows()
            }
        }
        loadAnalytics()
    }

    private fun rebuildRows(metrics: Map<String, LearnerMetricsDto> = _uiState.value.let { current ->
        current.learnerRows.mapNotNull { row -> row.metrics?.let { row.learnerId to it } }.toMap()
    }) {
        val rows = cachedLearners.map { learner ->
            LearnerAnalyticsRow(learnerId = learner.learnerId, name = learner.name, metrics = metrics[learner.learnerId])
        }
        _uiState.value = _uiState.value.copy(learnerRows = rows)
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            val offline = connectivityObserver.current() == ConnectivityState.OFFLINE
            _uiState.value = _uiState.value.copy(isLoading = !offline, isOffline = offline, error = null)
            if (offline) {
                loadFromCache()
                return@launch
            }

            try {
                val response = api.getClassAnalytics(classId)
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    _uiState.value = _uiState.value.copy(isLoading = false, classAverages = dto.class_averages, cachedAt = null)
                    rebuildRows(dto.learner_metrics)
                    cacheAnalytics(dto)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load analytics (${response.code()})")
                    loadFromCache()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Network error")
                loadFromCache()
            }
        }
    }

    private suspend fun cacheAnalytics(dto: ClassAnalyticsDto) {
        teacherCacheDao.upsertClassAnalytics(
            ClassAnalyticsCacheEntity(
                classId = classId,
                generatedAt = dto.generated_at,
                classAveragesJson = Json.encodeToString(dto.class_averages),
                learnerMetricsJson = Json.encodeToString(dto.learner_metrics),
                cachedAt = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun loadFromCache() {
        val cached = teacherCacheDao.getClassAnalytics(classId) ?: return
        val classAverages = Json.decodeFromString<Map<String, Float>>(cached.classAveragesJson)
        val learnerMetrics = Json.decodeFromString<Map<String, LearnerMetricsDto>>(cached.learnerMetricsJson)
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            classAverages = classAverages,
            cachedAt = cached.cachedAt,
            error = null,
        )
        rebuildRows(learnerMetrics)
    }
}
