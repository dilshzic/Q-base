package com.algorithmx.q_base.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Single shared StateFlow ensures only one system-level NetworkCallback is registered,
    // regardless of how many consumers collect this flow.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val isOnline: StateFlow<Boolean> = callbackFlow {
        fun currentConnectivity(): Boolean {
            val network = connectivityManager.activeNetwork
            if (network == null) {
                android.util.Log.d("QbaseNetwork", "currentConnectivity: activeNetwork is null")
                return false
            }
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities == null) {
                android.util.Log.d("QbaseNetwork", "currentConnectivity: capabilities is null for network=$network")
                return false
            }
            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            android.util.Log.d("QbaseNetwork", "currentConnectivity: network=$network hasInternet=$hasInternet")
            return hasInternet
        }

        val initialConnectivity = currentConnectivity()
        android.util.Log.d("QbaseNetwork", "NetworkMonitor: initialConnectivity=$initialConnectivity")
        trySend(initialConnectivity)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val conn = currentConnectivity()
                android.util.Log.d("QbaseNetwork", "NetworkMonitor callback: onAvailable network=$network, currentConnectivity=$conn")
                trySend(conn)
            }

            override fun onLost(network: Network) {
                val conn = currentConnectivity()
                android.util.Log.d("QbaseNetwork", "NetworkMonitor callback: onLost network=$network, currentConnectivity=$conn")
                trySend(conn)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val conn = currentConnectivity()
                android.util.Log.d("QbaseNetwork", "NetworkMonitor callback: onCapabilitiesChanged network=$network, currentConnectivity=$conn")
                trySend(conn)
            }
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
            android.util.Log.d("QbaseNetwork", "NetworkMonitor: successfully registered default network callback")
        } catch (e: Exception) {
            android.util.Log.e("QbaseNetwork", "NetworkMonitor: failed to register default network callback", e)
        }
        awaitClose {
            android.util.Log.d("QbaseNetwork", "NetworkMonitor: unregistering callback")
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.conflate()
     .stateIn(scope, SharingStarted.Eagerly, false)
}
