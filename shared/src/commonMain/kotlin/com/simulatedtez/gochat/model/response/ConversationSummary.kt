package com.simulatedtez.gochat.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationSummary(
    @SerialName("chatReference") val chatReference: String,
    @SerialName("otherUsername") val otherUsername: String,
    @SerialName("chatType") val chatType: String
)
