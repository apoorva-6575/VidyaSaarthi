package com.hackx.ruraledtech.core.connectivity

import kotlinx.coroutines.flow.Flow

enum class ConnectivityState { OFFLINE, LIMITED, ONLINE }

interface ConnectivityObserver {
    fun observe(): Flow<ConnectivityState>
    fun current(): ConnectivityState
}
