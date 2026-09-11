package com.hackx.ruraledtech.feature.contentlibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hackx.ruraledtech.domain.model.ContentPackage
import com.hackx.ruraledtech.domain.usecase.content.ObserveInstalledPackagesUseCase
import com.hackx.ruraledtech.domain.usecase.content.RemoveContentPackageUseCase
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
) : ViewModel() {

    val packages: StateFlow<List<ContentPackage>> = observeInstalledPackagesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun remove(packageId: String) {
        viewModelScope.launch { removeContentPackageUseCase(packageId) }
    }
}
