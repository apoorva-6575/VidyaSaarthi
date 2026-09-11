package com.hackx.ruraledtech.domain.usecase.sync

import com.hackx.ruraledtech.core.connectivity.ConnectivityObserver
import com.hackx.ruraledtech.core.connectivity.ConnectivityState
import com.hackx.ruraledtech.domain.repository.SyncOutcome
import com.hackx.ruraledtech.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConnectivityUseCase @Inject constructor(private val observer: ConnectivityObserver) {
    operator fun invoke(): Flow<ConnectivityState> = observer.observe()
}

class ObservePendingSyncCountUseCase @Inject constructor(private val repository: SyncRepository) {
    operator fun invoke(): Flow<Int> = repository.observePendingCount()
}

class TriggerSyncUseCase @Inject constructor(private val repository: SyncRepository) {
    suspend operator fun invoke(): SyncOutcome = repository.syncNow()
}
