package com.simulatedtez.gochat.database

import com.simulatedtez.gochat.model.DBMessage as ModelDBMessage
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.storage.IChatStorage as SharedIChatStorage

/**
 * Adapts the Android Room-backed [ChatDatabase] to the shared [SharedIChatStorage] interface.
 *
 * The two interfaces are structurally identical except that their "DBMessage" return types live
 * in different packages ([database.DBMessage] vs [model.DBMessage]).  This adapter bridges the
 * gap so the shared [ChatRepository] can accept the Room database at runtime.
 */
class ChatStorageAdapter(private val chatDatabase: ChatDatabase) : SharedIChatStorage {

    override suspend fun store(message: Message) = chatDatabase.store(message)

    override suspend fun store(messages: List<Message>) = chatDatabase.store(messages)

    override suspend fun deleteAllMessages() = chatDatabase.deleteAllMessages()

    override suspend fun getUndeliveredMessages(
        username: String, chatRef: String
    ): List<ModelDBMessage> =
        chatDatabase.getUndeliveredMessages(username, chatRef).toModelMessages()

    override suspend fun setAsSeen(vararg messageRefToChatRef: Pair<String, String>) =
        chatDatabase.setAsSeen(*messageRefToChatRef)

    override suspend fun setAsSent(vararg messageRefToChatRef: Pair<String, String>) =
        chatDatabase.setAsSent(*messageRefToChatRef)

    override suspend fun getMessage(messageRef: String): ModelDBMessage? =
        chatDatabase.getMessage(messageRef)?.toModelMessage()

    override suspend fun getPendingMessages(chatRef: String): List<ModelDBMessage> =
        chatDatabase.getPendingMessages(chatRef).toModelMessages()

    override suspend fun isEmpty(chatRef: String): Boolean = chatDatabase.isEmpty(chatRef)

    override suspend fun loadNextPage(chatRef: String): List<ModelDBMessage> =
        chatDatabase.loadNextPage(chatRef).toModelMessages()

    private fun DBMessage.toModelMessage(): ModelDBMessage = ModelDBMessage(
        id = id,
        message = message,
        sender = sender,
        receiver = receiver,
        timestamp = timestamp,
        chatReference = chatReference,
        deliveredTimestamp = deliveredTimestamp,
        seenTimestamp = seenTimestamp,
        isSent = isSent,
        isReadReceiptEnabled = isReadReceiptEnabled
    )

    private fun List<DBMessage>.toModelMessages(): List<ModelDBMessage> =
        map { it.toModelMessage() }
}
