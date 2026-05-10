package com.simulatedtez.gochat.model

data class ChatInfo(
    val username: String,
    val recipientsUsernames: List<String>,
    val chatReference: String,
    val isGroup: Boolean = false,
    val chatName: String = "",
)