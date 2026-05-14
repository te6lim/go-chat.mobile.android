package com.simulatedtez.gochat.storage

import com.simulatedtez.gochat.model.DBMessage
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.toDBMessage
import com.simulatedtez.gochat.session.ISession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryChatStorage(private val session: ISession) : IChatStorage {

    private val store = ConcurrentHashMap<String, CopyOnWriteArrayList<DBMessage>>()

    private fun listFor(chatRef: String): CopyOnWriteArrayList<DBMessage> =
        store.getOrPut(chatRef) { CopyOnWriteArrayList() }

    private fun upsert(message: DBMessage) {
        val list = listFor(message.chatReference)
        val idx = list.indexOfFirst { it.id == message.id }
        if (idx >= 0) list[idx] = message else list.add(message)
    }

    override suspend fun store(message: Message) {
        val db = message.toDBMessage().apply {
            if (sender == session.username) isSent = false
        }
        upsert(db)
    }

    override suspend fun store(messages: List<Message>) = messages.forEach { store(it) }

    override suspend fun getPendingMessages(chatRef: String): List<DBMessage> =
        listFor(chatRef).filter { it.sender == session.username && it.isSent == false }

    override suspend fun getUndeliveredMessages(username: String, chatRef: String): List<DBMessage> =
        listFor(chatRef).filter { it.sender == username && it.deliveredTimestamp == null }

    override suspend fun setAsSent(vararg messageRefToChatRef: Pair<String, String>) {
        for ((id, chatRef) in messageRefToChatRef) {
            val list = listFor(chatRef)
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) list[idx] = list[idx].copy(isSent = true)
        }
    }

    override suspend fun setAsSeen(vararg messageRefToChatRef: Pair<String, String>) {
        for ((id, chatRef) in messageRefToChatRef) {
            val list = listFor(chatRef)
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) list[idx] = list[idx].copy(seenTimestamp = "seen")
        }
    }

    override suspend fun getMessage(messageRef: String): DBMessage? =
        store.values.flatten().firstOrNull { it.id == messageRef }

    override suspend fun isEmpty(chatRef: String): Boolean = store[chatRef].isNullOrEmpty()

    override suspend fun loadNextPage(chatRef: String): List<DBMessage> =
        listFor(chatRef).sortedBy { it.timestamp }

    override suspend fun deleteAllMessages() = store.clear()

    fun getAllMessages(chatRef: String): List<DBMessage> =
        listFor(chatRef).sortedBy { it.timestamp }
}
