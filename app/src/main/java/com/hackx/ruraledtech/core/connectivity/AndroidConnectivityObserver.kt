package com.hackx.ruraledtech.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConnectivityObserver {

    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun observe(): Flow<ConnectivityState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(resolveState())
            }

            override fun onLost(network: Network) {
                trySend(resolveState())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(resolveState())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        manager.registerNetworkCallback(request, callback)
        trySend(resolveState())

        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    override fun current(): ConnectivityState = resolveState()

    private fun resolveState(): ConnectivityState {
        val network = manager.activeNetwork ?: return ConnectivityState.OFFLINE
        val capabilities = manager.getNetworkCapabilities(network) ?: return ConnectivityState.OFFLINE

        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return ConnectivityState.OFFLINE
        }

        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val isWeakCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) && isMetered &&
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED)

        return if (isWeakCellular) ConnectivityState.LIMITED else ConnectivityState.ONLINE
    }
}
