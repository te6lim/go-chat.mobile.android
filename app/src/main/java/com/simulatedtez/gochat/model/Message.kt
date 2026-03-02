package com.simulatedtez.gochat.model

import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.ui.UIMessage
import com.simulatedtez.gochat.database.DBMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import models.ComparableMessage

@Serializable
open class Message(
    @SerialName("messageReference") override val id: String,
    @SerialName("textMessage") override val message: String,
    @SerialName("senderUsername") override val sender: String,
    @SerialName("receiverUsername") override val receiver: String,
    @SerialName("sentTimestamp") override var timestamp: String,
    @SerialName("chatReference") open val chatReference: String,
    @SerialName("deliveredTimestamp") open var deliveredTimestamp: String? = null,
    @SerialName("seenTimestamp") open var seenTimestamp: String? = null,
    @SerialName("messageStatus") open val messageStatus: String? = null,
    @SerialName("presenceStatus") open val presenceStatus: String? = null,
    @SerialName("isBackedUp") open val isBackedUp: Boolean? = null,
    @SerialName("shouldDelete") open var shouldDelete: Boolean? = null,
    @SerialName("isReadReceiptEnabled") open val isReadReceiptEnabled: Boolean? = false
): ComparableMessage()

fun Message.toUIMessage(isSent: Boolean): UIMessage {
    return UIMessage(
        message = this,
        status = when {
            !seenTimestamp.isNullOrEmpty() -> MessageStatus.SEEN
            !deliveredTimestamp.isNullOrEmpty() -> MessageStatus.DELIVERED
            else -> {
                if (isSent) MessageStatus.SENT
                else MessageStatus.SENDING
            }
        }
    )
}

fun Message.toDBMessage(): DBMessage {
    return DBMessage(
        id = id,
        sender = sender,
        receiver = receiver,
        message = message,
        chatReference = chatReference,
        timestamp = timestamp,
        deliveredTimestamp = deliveredTimestamp,
        seenTimestamp = seenTimestamp,
        isSent = null,
        isReadReceiptEnabled = isReadReceiptEnabled
    )
}

fun List<Message>.toDBMessages(): List<DBMessage> {
    return map {
        it.toDBMessage()
    }
}