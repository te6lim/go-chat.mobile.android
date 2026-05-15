package com.simulatedtez.gochat.util

import ChatEngine
import MessageReturner
import com.simulatedtez.gochat.config.IConfig
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.Message
import listeners.ChatEngineEventListener

fun newPrivateChat(
    chatInfo: ChatInfo,
    eventListener: ChatEngineEventListener<Message>,
    config: IConfig,
    username: String,
): ChatEngine<Message> {
    return ChatEngine.Builder<Message>()
        .setSocketURL("${config.wsBaseUrl}/room/${chatInfo.chatReference}?me=${chatInfo.username}")
        .setUsername(chatInfo.username)
        .setExpectedReceivers(chatInfo.recipientsUsernames)
        .setChatServiceListener(eventListener)
        .setMessageReturner(socketMessageLabeler(username))
        .build(Message.serializer())
}

fun newAppWideChatService(
    username: String,
    eventListener: ChatEngineEventListener<Message>,
    config: IConfig
): ChatEngine<Message> {
    return ChatEngine.Builder<Message>()
        .setSocketURL("${config.wsBaseUrl}/conversations/$username")
        .setUsername(username)
        .setExpectedReceivers(listOf())
        .setChatServiceListener(eventListener)
        .setMessageReturner(socketMessageLabeler(username))
        .build(Message.serializer())
}

private fun socketMessageLabeler(username: String): MessageReturner<Message> {
    return object : MessageReturner<Message> {
        override fun returnMessage(message: Message): Message = Message(
            id = message.id,
            message = message.message,
            sender = message.sender,
            receiver = message.receiver,
            timestamp = message.timestamp,
            chatReference = message.chatReference,
            deliveredTimestamp = nowAsISOString(),
            seenTimestamp = message.seenTimestamp,
            isReadReceiptEnabled = message.isReadReceiptEnabled
        )

        override fun isMessageReturnable(message: Message): Boolean =
            message.sender != username
                && message.deliveredTimestamp == null
                && message.presenceStatus.isNullOrEmpty()
                && message.messageStatus.isNullOrEmpty()
    }
}
