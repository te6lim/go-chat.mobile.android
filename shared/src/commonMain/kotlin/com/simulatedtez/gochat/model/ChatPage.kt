package com.simulatedtez.gochat.model

import com.simulatedtez.gochat.model.ui.UIMessage

data class ChatPage(
    val messages: List<UIMessage>,
    val paginationCount: Int,
    val size: Int
)
