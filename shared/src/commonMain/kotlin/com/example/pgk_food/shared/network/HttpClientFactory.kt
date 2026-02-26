package com.example.pgk_food.shared.network

import io.ktor.client.engine.HttpClientEngineFactory

expect fun platformHttpClientEngineFactory(): HttpClientEngineFactory<*>
