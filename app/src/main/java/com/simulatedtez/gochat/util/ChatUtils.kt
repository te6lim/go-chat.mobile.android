package com.simulatedtez.gochat.util

import ChatEngine
import MessageReturner
import com.simulatedtez.gochat.BuildConfig
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.Message
import listeners.ChatEngineEventListener
import java.time.LocalDateTime

fun newPrivateChat(
    chatInfo: ChatInfo, eventListener: ChatEngineEventListener<Message>
): ChatEngine<Message> {
    return ChatEngine.Builder<Message>()
        .setSocketURL(
            "${BuildConfig.WEBSOCKET_BASE_URL}/room/${chatInfo.chatReference}?me=${chatInfo.username}"
        )
        .setUsername(chatInfo.username)
        .setExpectedReceivers(chatInfo.recipientsUsernames)
        .setChatServiceListener(eventListener)
        .setMessageReturner(socketMessageLabeler())
        .build(Message.serializer())
}

fun newAppWideChatService(username: String, eventListener: ChatEngineEventListener<Message>): ChatEngine<Message> {
    return ChatEngine.Builder<Message>()
            .setSocketURL(
                "${BuildConfig.WEBSOCKET_BASE_URL}/conversations/${username}"
            )
            .setUsername(username)
            .setExpectedReceivers(listOf())
            .setChatServiceListener(eventListener)
            .setMessageReturner(socketMessageLabeler())
            .build(Message.serializer())
}

private fun socketMessageLabeler(): MessageReturner<Message> {
    return object : MessageReturner<Message> {
        override fun returnMessage(
            message: Message
        ): Message {
            return Message(
                id = message.id,
                message = message.message,
                sender = message.sender,
                receiver = message.receiver,
                timestamp = message.timestamp,
                chatReference = message.chatReference,
                deliveredTimestamp = LocalDateTime.now().toISOString(),
                seenTimestamp = message.seenTimestamp,
                isReadReceiptEnabled = message.isReadReceiptEnabled
            )
        }

        override fun isMessageReturnable(message: Message): Boolean {
            return message.sender != session.username
                    && message.deliveredTimestamp == null
                    && message.presenceStatus.isNullOrEmpty()
                    && message.messageStatus.isNullOrEmpty()
        }
    }
}