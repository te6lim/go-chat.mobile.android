package com.simulatedtez.gochat.util

import ChatEngine
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.session.ISession
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
open class UserPresenceHelper(
    val chatEngine: ChatEngine<Message>?,
    statusToBeSent: PresenceStatus,
    private val chatInfo: ChatInfo?,
    private val session: ISession
) {
    var presenceStatus = statusToBeSent
        private set

    var presenceIdPair: Pair<String?, String?> = null to null
    var presenceId: String = Uuid.random().toString()

    fun handlePresenceMessage(
        receivedPresence: PresenceStatus,
        messageId: String,
        chatRef: String,
        onResolve: (status: PresenceStatus) -> Unit
    ) {
        if (presenceIdPair.second != messageId) postPresence(presenceStatus, chatRef)
        presenceIdPair = (presenceIdPair.first to messageId)
        onResolve(receivedPresence)
    }

    fun postNewUserPresence(presenceStatus: PresenceStatus) {
        if (!session.canSharePresenceStatus) return
        this.presenceStatus = presenceStatus
        presenceId = Uuid.random().toString()
        postPresence(presenceStatus)
    }

    fun postPresence(presenceStatus: PresenceStatus, chatRef: String) {
        if (!session.canSharePresenceStatus) return
        this.presenceStatus = presenceStatus
        chatEngine?.sendMessage(Message(
            id = presenceId,
            message = "",
            sender = session.username,
            receiver = chatInfo?.recipientsUsernames?.getOrElse(0) { "" } ?: "",
            timestamp = nowAsISOString(),
            chatReference = chatRef,
            presenceStatus = presenceStatus.name
        ))
    }

    private fun postPresence(presenceStatus: PresenceStatus) {
        if (!session.canSharePresenceStatus) return
        this.presenceStatus = presenceStatus
        chatEngine?.sendMessage(Message(
            id = presenceId,
            message = "",
            sender = chatInfo?.username ?: session.username,
            receiver = chatInfo?.recipientsUsernames?.getOrElse(0) { "" } ?: "",
            timestamp = nowAsISOString(),
            chatReference = chatInfo?.chatReference ?: "",
            presenceStatus = presenceStatus.name
        ))
    }

    fun onPresenceSent(id: String) {
        presenceIdPair = (id to presenceIdPair.second)
    }
}
