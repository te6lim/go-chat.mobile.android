package com.simulatedtez.gochat.storage

import com.simulatedtez.gochat.model.DBMessage
import com.simulatedtez.gochat.model.Message

interface IChatStorage {
    suspend fun store(message: Message)
    suspend fun store(messages: List<Message>)
    suspend fun deleteAllMessages()
    suspend fun getUndeliveredMessages(username: String, chatRef: String): List<DBMessage>
    suspend fun setAsSeen(vararg messageRefToChatRef: Pair<String, String>)
    suspend fun setAsSent(vararg messageRefToChatRef: Pair<String, String>)
    suspend fun getMessage(messageRef: String): DBMessage?
    suspend fun getPendingMessages(chatRef: String): List<DBMessage>
    suspend fun isEmpty(chatRef: String): Boolean
    suspend fun loadNextPage(chatRef: String): List<DBMessage>
}
