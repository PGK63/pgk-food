package com.example.pgk_food.shared.util

import kotlinx.coroutines.flow.Flow

class NetworkMonitor {
    val isConnected: Flow<Boolean> = observeNetworkConnectivity()
}

expect fun observeNetworkConnectivity(): Flow<Boolean>
