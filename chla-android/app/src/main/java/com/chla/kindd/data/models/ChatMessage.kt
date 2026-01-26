package com.chla.kindd.data.models

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val feedback: Feedback? = null
) {
    enum class Role {
        USER,
        ASSISTANT,
        SYSTEM
    }

    enum class Feedback {
        LIKED,
        DISLIKED
    }

    val isUser: Boolean get() = role == Role.USER
    val isAssistant: Boolean get() = role == Role.ASSISTANT
}

data class UserContext(
    val zipCode: String? = null,
    val childAge: Int? = null,
    val diagnosis: String? = null,
    val insurance: String? = null,
    val regionalCenter: String? = null
)

data class LLMStreamChunk(
    val type: String,
    val content: String? = null,
    val message: String? = null,
    @com.google.gson.annotations.SerializedName("providers_referenced")
    val providersReferenced: List<String>? = null,
    @com.google.gson.annotations.SerializedName("regional_center")
    val regionalCenter: String? = null
)
