package com.simulatedtez.gochat.model.ui

import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.enums.MessageStatus

data class UIMessage(
    val message: Message,
    var status: MessageStatus
)
