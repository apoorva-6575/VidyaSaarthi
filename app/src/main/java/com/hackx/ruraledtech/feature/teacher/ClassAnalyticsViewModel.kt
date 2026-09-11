package com.hackx.ruraledtech.feature.teacher

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.connectivity.ConnectivityObserver
import com.hackx.ruraledtech.core.connectivity.ConnectivityState
import com.hackx.ruraledtech.data.local.dao.ClassGroupDao
import com.hackx.ruraledtech.data.local.entities.LearnerEntity
import com.hackx.ruraledtech.data.remote.RuralEdTechApi
import com.hackx.ruraledtech.data.remote.dto.LearnerMetricsDto
import com.hackx.ruraledtech.feature.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ClassAnalyticsUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val error: String? = null,
    val classAverages: Map<String, Float> = emptyMap(),
    val learnerRows: List<LearnerAnalyticsRow> = emptyList(),
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
            if (offline) return@launch

            try {
                val response = api.getClassAnalytics(classId)
                if (response.isSuccessful && response.body() != null) {
                    val dto = response.body()!!
                    _uiState.value = _uiState.value.copy(isLoading = false, classAverages = dto.class_averages)
                    rebuildRows(dto.learner_metrics)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Failed to load analytics (${response.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Network error")
            }
        }
    }
}
