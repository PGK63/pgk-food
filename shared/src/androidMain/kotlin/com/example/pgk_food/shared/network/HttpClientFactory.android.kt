package com.example.pgk_food.shared.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android

actual fun platformHttpClientEngineFactory(): HttpClientEngineFactory<*> = Android
