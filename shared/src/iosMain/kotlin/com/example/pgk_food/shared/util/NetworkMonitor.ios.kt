package com.example.pgk_food.shared.util

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

actual fun observeNetworkConnectivity(): Flow<Boolean> = callbackFlow {
    val monitor = nw_path_monitor_create()
    nw_path_monitor_set_update_handler(monitor) { path ->
        val isConnected = nw_path_get_status(path) == nw_path_status_satisfied
        trySend(isConnected)
    }
    nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
    nw_path_monitor_start(monitor)

    awaitClose {
        nw_path_monitor_cancel(monitor)
    }
}
