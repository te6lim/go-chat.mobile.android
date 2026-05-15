package com.simulatedtez.gochat.storage

import com.simulatedtez.gochat.model.DBConversation
import com.simulatedtez.gochat.model.Message
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryConversationStore : IConversationsStore {

    private val conversations = CopyOnWriteArrayList<DBConversation>()

    override suspend fun getConversations(): List<DBConversation> =
        conversations.sortedByDescending { it.timestamp }

    override suspend fun insertConversation(convo: DBConversation) {
        val idx = conversations.indexOfFirst { it.chatReference == convo.chatReference }
        if (idx >= 0) conversations[idx] = convo else conversations.add(convo)
    }

    override suspend fun insertConversations(convos: List<DBConversation>) =
        convos.forEach { insertConversation(it) }

    override suspend fun updateConversationLastMessage(message: Message) {
        val idx = conversations.indexOfFirst { it.chatReference == message.chatReference }
        if (idx >= 0) conversations[idx] = conversations[idx].copy(
            lastMessage = message.message,
            timestamp = message.timestamp
        )
    }

    override suspend fun updateUnreadCountToZero(chatRef: String) {
        val idx = conversations.indexOfFirst { it.chatReference == chatRef }
        if (idx >= 0) conversations[idx] = conversations[idx].copy(unreadCount = 0)
    }

    override suspend fun deleteConversation(chatRef: String) {
        conversations.removeIf { it.chatReference == chatRef }
    }

    fun deleteAllConversations() = conversations.clear()
}
