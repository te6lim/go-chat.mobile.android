package com.simulatedtez.gochat.model

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
    @SerialName("shouldDelete") open var shouldDelete: Boolean? = null,
    @SerialName("isReadReceiptEnabled") open val isReadReceiptEnabled: Boolean? = false
) : ComparableMessage()
