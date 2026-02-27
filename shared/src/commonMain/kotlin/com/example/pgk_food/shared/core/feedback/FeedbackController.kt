package com.example.pgk_food.shared.core.feedback

import com.example.pgk_food.shared.platform.currentTimeMillis
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object FeedbackController {
    private val _messages = MutableSharedFlow<FeedbackMessage>(extraBufferCapacity = 32)
    val messages = _messages.asSharedFlow()

    private const val DedupWindowMs = 2_000L
    private val messageTimestamps = mutableMapOf<String, Long>()

    fun emit(message: FeedbackMessage) {
        val key = "${message.level}:${message.text}:${message.actionLabel.orEmpty()}"
        val now = currentTimeMillis()
        val previous = messageTimestamps[key]
        if (previous != null && now - previous < DedupWindowMs) return
        messageTimestamps[key] = now
        _messages.tryEmit(message)
    }

    fun success(text: String, actionLabel: String? = null, actionId: String? = null) {
        emit(FeedbackMessage(text, FeedbackLevel.Success, actionLabel, actionId))
    }

    fun error(text: String, actionLabel: String? = null, actionId: String? = null) {
        emit(FeedbackMessage(text, FeedbackLevel.Error, actionLabel, actionId))
    }

    fun info(text: String, actionLabel: String? = null, actionId: String? = null) {
        emit(FeedbackMessage(text, FeedbackLevel.Info, actionLabel, actionId))
    }
}

data class FeedbackMessage(
    val text: String,
    val level: FeedbackLevel = FeedbackLevel.Info,
    val actionLabel: String? = null,
    val actionId: String? = null,
)

enum class FeedbackLevel {
    Success,
    Error,
    Info,
}
