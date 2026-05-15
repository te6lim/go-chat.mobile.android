package com.simulatedtez.gochat.util

import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.ui.UIMessage

fun Message.toUIMessage(isSent: Boolean): UIMessage {
    return UIMessage(
        message = this,
        status = when {
            !seenTimestamp.isNullOrEmpty() -> MessageStatus.SEEN
            !deliveredTimestamp.isNullOrEmpty() -> MessageStatus.DELIVERED
            else -> if (isSent) MessageStatus.SENT else MessageStatus.SENDING
        }
    )
}
