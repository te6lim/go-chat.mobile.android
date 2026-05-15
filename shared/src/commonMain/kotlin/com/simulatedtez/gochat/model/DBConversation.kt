package com.simulatedtez.gochat.model

data class DBConversation(
    val chatReference: String,
    val otherUser: String,
    val lastMessage: String = "",
    var timestamp: String = "",
    var unreadCount: Int = 0,
    val contactAvi: String = "",
    val isPendingSentInvite: Boolean = false,
    val chatType: String = "private",
    val chatName: String = ""
)
