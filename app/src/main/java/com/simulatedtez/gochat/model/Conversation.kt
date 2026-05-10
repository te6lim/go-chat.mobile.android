package com.simulatedtez.gochat.model

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val other: String,
    val chatReference: String,
    var lastMessage: String = "",
    var timestamp: String = "",
    var unreadCount: Int = 0,
    val contactAvi: String = "",
    val isPendingSentInvite: Boolean = false,
    val chatType: String = "private",
    val chatName: String = "",
)