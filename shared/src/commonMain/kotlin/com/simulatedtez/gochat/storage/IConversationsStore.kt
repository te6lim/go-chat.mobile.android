package com.simulatedtez.gochat.storage

import com.simulatedtez.gochat.model.DBConversation
import com.simulatedtez.gochat.model.Message

interface IConversationsStore {
    suspend fun getConversations(): List<DBConversation>
    suspend fun insertConversation(convo: DBConversation)
    suspend fun insertConversations(convos: List<DBConversation>)
    suspend fun deleteConversation(chatRef: String)
    suspend fun updateConversationLastMessage(message: Message) {}
    suspend fun updateUnreadCountToZero(chatRef: String) {}
}
