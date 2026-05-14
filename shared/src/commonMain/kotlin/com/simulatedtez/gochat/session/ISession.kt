package com.simulatedtez.gochat.session

import ChatEngine
import com.simulatedtez.gochat.model.Message

interface ISession {
    val username: String
    val accessToken: String
    val tokenExpiryMs: Long
    val isReadReceiptEnabled: Boolean
    val canSharePresenceStatus: Boolean
    val appWideChatService: ChatEngine<Message>?

    fun saveTokenDetails(accessToken: String, expiryTime: String)
    fun saveUsername(username: String)
    fun savePassword(password: String)
    fun getPassword(): String?

    fun isNewChatHistory(chatRef: String): Boolean
    fun storeChatHistoryStatus(chatRef: String, isNew: Boolean)
    fun getCutOffDateForMarkingMessagesAsSeen(): String?
}
