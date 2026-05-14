package com.simulatedtez.gochat.listener

import ChatServiceErrorResponse
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.enums.PresenceStatus
import okhttp3.Response

interface SocketConnection {
    fun onClose(code: Int, reason: String)
    fun onConnect()
    fun onDisconnect(t: Throwable, response: Response?)
    fun onError(error: ChatServiceErrorResponse<Message>)
}

interface MessageSender {
    fun onSend(message: Message) {}
}

interface MessageReceiver {
    fun onReceiveRecipientMessageStatus(chatRef: String, messageStatus: MessageStatus)
    fun onReceive(message: Message)
}

interface ChatEventListener : SocketConnection, MessageSender, MessageReceiver {
    fun onReceiveRecipientActivityStatusMessage(presenceStatus: PresenceStatus)
    fun onMessageSent(message: Message)
}
