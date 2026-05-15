package com.simulatedtez.gochat.database

import com.simulatedtez.gochat.model.DBConversation as ModelDBConversation
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.storage.IConversationsStore

/**
 * Adapts the Android Room-backed [ConversationDatabase] to the shared [IConversationsStore]
 * interface.
 *
 * The two types use different "DBConversation" classes ([database.DBConversation] vs
 * [model.DBConversation]).  This adapter converts between them so the shared [ChatRepository]
 * can manage conversation metadata through the Room database.
 */
class ConversationStoreAdapter(private val conversationDB: ConversationDatabase) : IConversationsStore {

    override suspend fun getConversations(): List<ModelDBConversation> =
        conversationDB.getConversations().map { it.toModel() }

    override suspend fun insertConversation(convo: ModelDBConversation) =
        conversationDB.insertConversation(convo.toDatabase())

    override suspend fun insertConversations(convos: List<ModelDBConversation>) =
        conversationDB.insertConversations(convos.map { it.toDatabase() })

    override suspend fun deleteConversation(chatRef: String) =
        conversationDB.deleteConversation(chatRef)

    override suspend fun updateConversationLastMessage(message: Message) =
        conversationDB.updateConversationLastMessage(message)

    override suspend fun updateUnreadCountToZero(chatRef: String) =
        conversationDB.updateUnreadCountToZero(chatRef)

    private fun DBConversation.toModel(): ModelDBConversation = ModelDBConversation(
        chatReference = chatReference,
        otherUser = otherUser,
        lastMessage = lastMessage,
        timestamp = timestamp,
        unreadCount = unreadCount,
        contactAvi = contactAvi,
        isPendingSentInvite = isPendingSentInvite,
        chatType = chatType,
        chatName = chatName
    )

    private fun ModelDBConversation.toDatabase(): DBConversation = DBConversation(
        chatReference = chatReference,
        otherUser = otherUser,
        lastMessage = lastMessage,
        timestamp = timestamp,
        unreadCount = unreadCount,
        contactAvi = contactAvi,
        isPendingSentInvite = isPendingSentInvite,
        chatType = chatType,
        chatName = chatName
    )
}
