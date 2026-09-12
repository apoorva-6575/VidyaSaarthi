package com.hackx.ruraledtech.feature.teacher

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.connectivity.ConnectivityObserver
import com.hackx.ruraledtech.core.connectivity.ConnectivityState
import com.hackx.ruraledtech.data.local.dao.ClassGroupDao
import com.hackx.ruraledtech.data.local.dao.TeacherCacheDao
import com.hackx.ruraledtech.data.local.entities.ClassAnalyticsCacheEntity
import com.hackx.ruraledtech.data.local.entities.ClassGroupEntity
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

import com.hackx.ruraledtech.data.local.dao.AttemptDao
import com.hackx.ruraledtech.data.local.dao.MasteryDao
import com.hackx.ruraledtech.data.local.dao.ProgressDao

import com.hackx.ruraledtech.domain.model.ContentPackage
import com.hackx.ruraledtech.domain.usecase.content.ObserveInstalledPackagesUseCase
import com.hackx.ruraledtech.p2p.mesh.LearningMesh
import com.hackx.ruraledtech.p2p.mesh.MeshState

data class ClassAnalyticsUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val error: String? = null,
    val classGroup: ClassGroupEntity? = null,
    val classAverages: Map<String, Float> = emptyMap(),
    val learnerRows: List<LearnerAnalyticsRow> = emptyList(),
    val attendance: Map<String, Boolean> = emptyMap(),
    val cachedAt: Long? = null,
    val exportedReport: String? = null,
)

data class LearnerAnalyticsRow(
    val learnerId: String,
    val name: String,
    val metrics: LearnerMetricsDto?,
    val isPresent: Boolean = true,
)

/** Per-class breakdown (PS section 6.6) supporting offline facilitator group tracking and attendance. */
@HiltViewModel
class ClassAnalyticsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: RuralEdTechApi,
    private val classGroupDao: ClassGroupDao,
    private val teacherCacheDao: TeacherCacheDao,
    private val attemptDao: AttemptDao,
    private val masteryDao: MasteryDao,
    private val progressDao: ProgressDao,
    private val connectivityObserver: ConnectivityObserver,
    observeInstalledPackagesUseCase: ObserveInstalledPackagesUseCase,
    private val learningMesh: LearningMesh,
) : ViewModel() {

    val classId: String = checkNotNull(savedStateHandle[Routes.ARG_CLASS_ID])

    val packages: StateFlow<List<ContentPackage>> = observeInstalledPackagesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val meshState: StateFlow<MeshState> = learningMesh.observeMeshState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MeshState.OFFLINE)

    val connectedEndpoints: StateFlow<List<String>> = learningMesh.observeConnectedEndpoints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(ClassAnalyticsUiState())
    val uiState: StateFlow<ClassAnalyticsUiState> = _uiState

    private var cachedLearners: List<LearnerEntity> = emptyList()
    private val attendanceMap = mutableMapOf<String, Boolean>()

    fun sharePackageToClass(packageId: String) {
        viewModelScope.launch {
            learningMesh.broadcastPackage(packageId)
        }
    }

    fun startMesh() {
        learningMesh.startMesh()
    }

    fun stopMesh() {
        learningMesh.stopMesh()
    }

    init {
        viewModelScope.launch {
            val cg = classGroupDao.getById(classId)
            _uiState.value = _uiState.value.copy(classGroup = cg)
        }
        viewModelScope.launch {
            classGroupDao.observeLearnersForClass(classId).collectLatest { learners ->
                cachedLearners = learners
                learners.forEach { l ->
                    if (!attendanceMap.containsKey(l.learnerId)) {
                        attendanceMap[l.learnerId] = true
                    }
                }
                rebuildRows()
            }
        }
        loadAnalytics()
    }

    fun toggleAttendance(learnerId: String) {
        val current = attendanceMap[learnerId] ?: true
        attendanceMap[learnerId] = !current
        _uiState.value = _uiState.value.copy(attendance = attendanceMap.toMap())
        rebuildRows()
    }

    fun exportClassReport(): String {
        val state = _uiState.value
        val className = state.classGroup?.name ?: "Classroom"
        val totalStudents = state.learnerRows.size
        val presentCount = state.learnerRows.count { it.isPresent }
        val dateStr = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())

        val sb = StringBuilder()
        sb.append("📊 VIDYASARATHI FACILITATOR REPORT\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🏫 Class: $className (${state.classGroup?.grade ?: ""} ${state.classGroup?.subject ?: ""})\n")
        sb.append("📅 Date: $dateStr\n")
        sb.append("👥 Attendance: $presentCount / $totalStudents Present\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

        if (state.classAverages.isNotEmpty()) {
            sb.append("📈 Concept Mastery Averages:\n")
            state.classAverages.forEach { (concept, score) ->
                sb.append(" • $concept: ${(score * 100).toInt()}%\n")
            }
            sb.append("\n")
        }

        sb.append("👤 Student Roster & Performance:\n")
        state.learnerRows.forEachIndexed { i, row ->
            val status = if (row.isPresent) "✅ Present" else "❌ Absent"
            val m = row.metrics
            val scoreStr = if (m != null) "${(m.average_score * 100).toInt()}% avg (${m.completed_lessons} lessons)" else "Enrolled"
            sb.append("${i + 1}. ${row.name} — $status | $scoreStr\n")
        }
        sb.append("\nGenerated offline via VidyaSarathi Field Facilitator Tool.")
        val report = sb.toString()
        _uiState.value = _uiState.value.copy(exportedReport = report)
        return report
    }

    private fun rebuildRows(metrics: Map<String, LearnerMetricsDto> = _uiState.value.let { current ->
        current.learnerRows.mapNotNull { row -> row.metrics?.let { row.learnerId to it } }.toMap()
    }) {
        val allIds = (cachedLearners.map { it.learnerId } + metrics.keys).distinct()
        val rows = allIds.map { lid ->
            val cached = cachedLearners.find { it.learnerId == lid }
            val metric = metrics[lid]
            val name = cached?.name ?: metric?.name?.ifBlank { null } ?: "Student ${lid.take(4)}"
            val isPresent = attendanceMap[lid] ?: true
            LearnerAnalyticsRow(learnerId = lid, name = name, metrics = metric, isPresent = isPresent)
        }
        _uiState.value = _uiState.value.copy(
            learnerRows = rows,
            attendance = attendanceMap.toMap(),
        )
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
        val cached = teacherCacheDao.getClassAnalytics(classId)
        if (cached != null) {
            val classAverages = Json.decodeFromString<Map<String, Float>>(cached.classAveragesJson)
            val learnerMetrics = Json.decodeFromString<Map<String, LearnerMetricsDto>>(cached.learnerMetricsJson)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                classAverages = classAverages,
                cachedAt = cached.cachedAt,
                error = null,
            )
            rebuildRows(learnerMetrics)
        } else {
            // Offline fallback: aggregate from local learners
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                classAverages = emptyMap(),
                cachedAt = System.currentTimeMillis(),
                error = null,
            )
            rebuildRows()
        }
    }
}
