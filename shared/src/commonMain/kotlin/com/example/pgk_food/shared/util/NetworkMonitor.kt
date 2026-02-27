package com.example.pgk_food.shared.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkMonitor {
    val isConnected: Flow<Boolean> = observeNetworkConnectivity().distinctUntilChanged()
}

expect fun observeNetworkConnectivity(): Flow<Boolean>
