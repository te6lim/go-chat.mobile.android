package com.simulatedtez.gochat.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewChatResponse(
    @SerialName("user") val user: String,
    @SerialName("other") val other: String,
    @SerialName("chatReference") val chatReference: String
)
