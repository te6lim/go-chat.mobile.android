package com.simulatedtez.gochat.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("expiryTime") val expiryTime: String
)
