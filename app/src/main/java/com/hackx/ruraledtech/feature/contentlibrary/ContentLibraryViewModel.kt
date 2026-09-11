package com.hackx.ruraledtech.feature.contentlibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.domain.model.ContentPackage
import com.hackx.ruraledtech.domain.usecase.content.ObserveInstalledPackagesUseCase
import com.hackx.ruraledtech.domain.usecase.content.RemoveContentPackageUseCase
import com.hackx.ruraledtech.p2p.integration.ContentRequirement
import com.hackx.ruraledtech.p2p.mesh.LearningMesh
import com.hackx.ruraledtech.p2p.mesh.MeshState
import com.hackx.ruraledtech.p2p.mesh.TransferTask
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContentLibraryViewModel @Inject constructor(
    observeInstalledPackagesUseCase: ObserveInstalledPackagesUseCase,
    private val removeContentPackageUseCase: RemoveContentPackageUseCase,
    private val learningMesh: LearningMesh,
) : ViewModel() {

    val packages: StateFlow<List<ContentPackage>> = observeInstalledPackagesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val meshState: StateFlow<MeshState> = learningMesh.observeMeshState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MeshState.OFFLINE)

    val transfers: StateFlow<List<TransferTask>> = learningMesh.observeTransfers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun remove(packageId: String) {
        viewModelScope.launch { removeContentPackageUseCase(packageId) }
    }

    fun findNearbyDevice() {
        learningMesh.startMesh()
    }

    /** Demo hook for PS section 36's "Find nearby learning device" empty-state action. */
    fun requestMissingContent(packageId: String, conceptId: String) {
        viewModelScope.launch {
            learningMesh.requestContent(
                ContentRequirement(packageId = packageId, version = null, conceptId = conceptId, priority = 1f, reason = "learner_requested"),
            )
        }
    }
}
