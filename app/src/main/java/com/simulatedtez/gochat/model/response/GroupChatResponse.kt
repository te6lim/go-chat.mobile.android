package com.simulatedtez.gochat.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupChatResponse(
    @SerialName("chatReference") val chatReference: String,
    @SerialName("name") val name: String,
    @SerialName("participants") val participants: List<String>
)
