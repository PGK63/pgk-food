package com.example.pgk_food.data.remote

import com.example.pgk_food.shared.network.SharedNetworkModule

object NetworkModule {
    val client = SharedNetworkModule.client

    fun getUrl(path: String) = SharedNetworkModule.getUrl(path)
}
