package com.simulatedtez.gochat.util

import android.content.Context
import com.simulatedtez.gochat.Session
import com.simulatedtez.gochat.UserPreference
import com.simulatedtez.gochat.database.ChatDatabase
import com.simulatedtez.gochat.database.ConversationDatabase

class CleanupManager(
    private val chatDatabase: ChatDatabase,
    private val conversationDatabase: ConversationDatabase
) {

    companion object {
        private var INSTANCE: CleanupManager? = null
        fun get(context: Context): CleanupManager {
            return INSTANCE ?: synchronized(this) {
                val instance = CleanupManager(
                    ChatDatabase.get(context),
                    ConversationDatabase.get(context)
                )
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun clearUserData() {
        chatDatabase.deleteAllMessages()
        conversationDatabase.deleteAllConversations()
        UserPreference.deleteUsername()
        UserPreference.deleteAccessToken()
        UserPreference.deletePassword()
        Session.clear()
    }
}