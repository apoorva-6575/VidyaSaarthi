package com.hackx.ruraledtech.feature.passport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.core.device.DeviceIdProvider
import com.hackx.ruraledtech.core.session.CurrentLearnerManager
import com.hackx.ruraledtech.p2p.mesh.LearningMesh
import com.hackx.ruraledtech.p2p.mesh.MeshState
import com.hackx.ruraledtech.p2p.passport.model.LearningPassport
import com.hackx.ruraledtech.p2p.passport.transport.PassportImportResult
import com.hackx.ruraledtech.p2p.passport.transport.PassportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PassportUiState(
    val learnerId: String? = null,
    val meshState: MeshState = MeshState.IDLE,
    val connectedEndpoints: List<String> = emptyList(),
    val pendingPassport: LearningPassport? = null,
    val exporting: Boolean = false,
    val exportSent: Boolean = false,
    val importing: Boolean = false,
    val importResult: PassportImportResult? = null,
)

/**
 * Learning Passport (P2P offline export/import of a learner's progress so they can move
 * between devices) had crypto/transport fully built by Group 3 but no screen anywhere ever
 * called into it — nothing let a learner actually create or import one.
 */
@HiltViewModel
class PassportViewModel @Inject constructor(
    private val currentLearnerManager: CurrentLearnerManager,
    private val passportManager: PassportManager,
    private val learningMesh: LearningMesh,
    private val deviceIdProvider: DeviceIdProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PassportUiState())
    val uiState: StateFlow<PassportUiState> = _uiState

    init {
        _uiState.value = _uiState.value.copy(learnerId = currentLearnerManager.currentLearnerId.value)

        viewModelScope.launch {
            combine(learningMesh.observeMeshState(), learningMesh.observeConnectedEndpoints()) { mesh, endpoints -> mesh to endpoints }
                .collectLatest { (mesh, endpoints) ->
                    _uiState.value = _uiState.value.copy(meshState = mesh, connectedEndpoints = endpoints)
                }
        }

        viewModelScope.launch {
            passportManager.pendingPassport.collectLatest { passport ->
                _uiState.value = _uiState.value.copy(pendingPassport = passport, importResult = null)
            }
        }
    }

    fun startMesh() = learningMesh.startMesh()

    fun exportPassport(pin: String) {
        val learnerId = _uiState.value.learnerId ?: return
        val endpointId = _uiState.value.connectedEndpoints.firstOrNull() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(exporting = true, exportSent = false)
            val deviceId = deviceIdProvider.get()
            passportManager.exportAndSendPassport(learnerId, pin, endpointId, deviceId)
            _uiState.value = _uiState.value.copy(exporting = false, exportSent = true)
        }
    }

    fun importPendingPassport(pin: String) {
        val passport = _uiState.value.pendingPassport ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(importing = true, importResult = null)
            val result = passportManager.importPassport(passport, pin)
            _uiState.value = _uiState.value.copy(importing = false, importResult = result)
        }
    }

    fun dismissPendingPassport() {
        passportManager.clearPendingPassport()
        _uiState.value = _uiState.value.copy(pendingPassport = null, importResult = null)
    }

    fun clearExportSent() {
        _uiState.value = _uiState.value.copy(exportSent = false)
    }
}
