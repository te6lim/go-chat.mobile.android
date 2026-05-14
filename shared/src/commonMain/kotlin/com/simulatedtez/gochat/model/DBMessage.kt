package com.simulatedtez.gochat.model

import models.ComparableMessage

class DBMessage(
    override val id: String,
    override val message: String,
    override val sender: String,
    override val receiver: String,
    override var timestamp: String,
    val chatReference: String,
    val deliveredTimestamp: String? = null,
    val seenTimestamp: String? = null,
    var isSent: Boolean?,
    val isReadReceiptEnabled: Boolean? = null
) : ComparableMessage()

fun Message.toDBMessage(): DBMessage = DBMessage(
    id = id,
    message = message,
    sender = sender,
    receiver = receiver,
    timestamp = timestamp,
    chatReference = chatReference,
    deliveredTimestamp = deliveredTimestamp,
    seenTimestamp = seenTimestamp,
    isSent = null,
    isReadReceiptEnabled = isReadReceiptEnabled
)

fun List<DBMessage>.toMessages(): List<Message> = map {
    Message(
        id = it.id,
        message = it.message,
        sender = it.sender,
        receiver = it.receiver,
        timestamp = it.timestamp,
        chatReference = it.chatReference,
        deliveredTimestamp = it.deliveredTimestamp,
        seenTimestamp = it.seenTimestamp
    )
}
