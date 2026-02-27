package com.example.pgk_food.shared.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

private val iosNetworkState = MutableStateFlow(true)

actual fun observeNetworkConnectivity(): Flow<Boolean> = iosNetworkState
