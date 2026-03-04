package com.example.pgk_food.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class AccountStatus {
    ACTIVE,
    FROZEN_EXPELLED
}

fun AccountStatus.titleRu(): String = when (this) {
    AccountStatus.ACTIVE -> "Активен"
    AccountStatus.FROZEN_EXPELLED -> "Отчислен (заморожен)"
}
