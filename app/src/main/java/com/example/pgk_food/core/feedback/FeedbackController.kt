package com.example.pgk_food.core.feedback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

object FeedbackController {
    private val _messages = MutableSharedFlow<FeedbackMessage>(extraBufferCapacity = 32)
    val messages = _messages.asSharedFlow()

    private val dedupWindowMs = 2_000L
    private val messageTimestamps = ConcurrentHashMap<String, Long>()

    fun emit(message: FeedbackMessage) {
        val key = "${message.level}:${message.text}:${message.actionLabel ?: ""}"
        val now = System.currentTimeMillis()
        val previous = messageTimestamps[key]

        if (previous != null && now - previous < dedupWindowMs) return

        messageTimestamps[key] = now
        _messages.tryEmit(message)
    }

    fun success(text: String, actionLabel: String? = null, actionId: String? = null) {
        emit(FeedbackMessage(text = text, level = FeedbackLevel.Success, actionLabel = actionLabel, actionId = actionId))
    }

    fun error(text: String, actionLabel: String? = null, actionId: String? = null) {
        emit(FeedbackMessage(text = text, level = FeedbackLevel.Error, actionLabel = actionLabel, actionId = actionId))
    }

    fun info(text: String, actionLabel: String? = null, actionId: String? = null) {
        emit(FeedbackMessage(text = text, level = FeedbackLevel.Info, actionLabel = actionLabel, actionId = actionId))
    }
}

data class FeedbackMessage(
    val text: String,
    val level: FeedbackLevel = FeedbackLevel.Info,
    val actionLabel: String? = null,
    val actionId: String? = null
)

enum class FeedbackLevel {
    Success,
    Error,
    Info
}
