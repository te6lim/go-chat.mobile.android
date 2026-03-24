package com.simulatedtez.gochat

import ChatEngine
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.util.newAppWideChatService
import listeners.ChatEngineEventListener

open class Session private constructor() {

    var username: String = ""
        private set
    var accessToken: String = ""
        private set
    var lastActiveChat: ChatInfo? = null
        private set

    var appWideChatService: ChatEngine<Message>? = null
        private set

    var isReadReceiptEnabled: Boolean = false
    private set

    var canSharePresenceStatus: Boolean = false

    init {
        UserPreference.getUsername()?.let {
            saveUsername(it)
        }
        UserPreference.getAccessToken()?.let {
            saveAccessToken(it)
        }
        isReadReceiptEnabled = UserPreference.isReadReceiptEnabled()
        canSharePresenceStatus = UserPreference.canSharePresenceStatus()
    }

    companion object {
        var session = object: Session() {}
        private set

        fun clear() {
            session = object: Session() {}
        }
    }

    fun setPresenceSharing(isEnabled: Boolean) {
        UserPreference.presenceSharingToggle(isEnabled)
        canSharePresenceStatus = isEnabled
    }

    fun toggleReadReceipt(isEnabled: Boolean) {
        UserPreference.readReceiptToggle(isEnabled)
        isReadReceiptEnabled = isEnabled
    }

    fun setupAppWideChatService(eventListener: ChatEngineEventListener<Message>) {
        if (appWideChatService == null) {
            appWideChatService = newAppWideChatService(session.username, eventListener)
        }
    }

    fun setActiveChat(chatInfo: ChatInfo) {
        lastActiveChat = chatInfo
    }

    fun saveAccessToken(token: String) {
        UserPreference.storeAccessToken(token)
        accessToken = token
    }

    fun saveUsername(username: String) {
        UserPreference.storeUsername(username)
        this.username = username
    }

    fun savePassword(password: String) {
        UserPreference.storePassword(password)
    }

    fun getPassword(): String? {
        return UserPreference.getPassword()
    }
}