package com.example.pgk_food.shared.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.pgk_food.shared.platform.requireAndroidAppContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual fun observeNetworkConnectivity(): Flow<Boolean> = callbackFlow {
    val appContext = requireAndroidAppContext()
    val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trySend(true)
        }

        override fun onLost(network: Network) {
            trySend(false)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val connected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            trySend(connected)
        }
    }

    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    connectivityManager.registerNetworkCallback(request, callback)

    val activeNetwork = connectivityManager.activeNetwork
    val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
    trySend(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)

    awaitClose {
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
    }
}
