package com.simulatedtez.gochat

import ChatEngine
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.util.newAppWideChatService
import listeners.ChatEngineEventListener
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

open class Session private constructor() {

    var username: String = ""
        private set
    var accessToken: String = ""
        private set
    var tokenExpiryMs: Long = 0L
        private set
    var lastActiveChat: ChatInfo? = null
        private set

    var appWideChatService: ChatEngine<Message>? = null
        private set

    var isReadReceiptEnabled: Boolean = false
        private set

    var canSharePresenceStatus: Boolean = false

    var isRevealAnimationEnabled: Boolean = true
        private set

    init {
        UserPreference.getUsername()?.let {
            saveUsername(it)
        }
        UserPreference.getAccessToken()?.let {
            saveAccessToken(it)
        }
        tokenExpiryMs = UserPreference.getTokenExpiry()
        isReadReceiptEnabled = UserPreference.isReadReceiptEnabled()
        canSharePresenceStatus = UserPreference.canSharePresenceStatus()
        isRevealAnimationEnabled = UserPreference.isRevealAnimationEnabled()
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

    fun toggleRevealAnimation(isEnabled: Boolean) {
        UserPreference.revealAnimationToggle(isEnabled)
        isRevealAnimationEnabled = isEnabled
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

    fun saveTokenExpiry(expiryMs: Long) {
        UserPreference.storeTokenExpiry(expiryMs)
        tokenExpiryMs = expiryMs
    }

    /** Atomically save both the access token and its expiry. Use this after any
     *  login or token-refresh so both fields are always in sync. */
    fun saveTokenDetails(accessToken: String, expiryTime: String) {
        saveAccessToken(accessToken)
        val expiryMs = parseExpiryToMillis(expiryTime)
        if (expiryMs > 0L) saveTokenExpiry(expiryMs)
    }

    private fun parseExpiryToMillis(expiryTime: String): Long {
        // ISO 8601 — "2026-05-13T13:21:13Z"
        return try {
            Instant.parse(expiryTime).toEpochMilli()
        } catch (_: Exception) {
            // Go time.Time.String() — "2026-05-13 13:21:13 +0000 UTC"
            try {
                ZonedDateTime.parse(
                    expiryTime,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z z")
                ).toInstant().toEpochMilli()
            } catch (_: Exception) {
                // Numeric epoch millis string
                try {
                    expiryTime.toLong()
                } catch (_: Exception) {
                    0L
                }
            }
        }
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