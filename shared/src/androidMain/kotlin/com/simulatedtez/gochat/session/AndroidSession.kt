package com.simulatedtez.gochat.session

import ChatEngine
import com.simulatedtez.gochat.model.Message
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

open class AndroidSession private constructor() : ISession {

    override var username: String = ""
        protected set
    override var accessToken: String = ""
        protected set
    override var tokenExpiryMs: Long = 0L
        protected set
    override var isReadReceiptEnabled: Boolean = false
        protected set
    override var canSharePresenceStatus: Boolean = false
    override var appWideChatService: ChatEngine<Message>? = null
        protected set

    companion object {
        var session: AndroidSession = object : AndroidSession() {}
            private set

        fun clear() {
            session = object : AndroidSession() {}
        }
    }

    fun initFromPrefs(prefs: IAndroidSessionPrefs) {
        prefs.getUsername()?.let { username = it }
        prefs.getAccessToken()?.let { accessToken = it }
        tokenExpiryMs = prefs.getTokenExpiry()
        isReadReceiptEnabled = prefs.isReadReceiptEnabled()
        canSharePresenceStatus = prefs.canSharePresenceStatus()
    }

    override fun saveTokenDetails(accessToken: String, expiryTime: String) {
        this.accessToken = accessToken
        val expiryMs = parseExpiryToMillis(expiryTime)
        if (expiryMs > 0L) tokenExpiryMs = expiryMs
    }

    override fun saveUsername(username: String) { this.username = username }
    override fun savePassword(password: String) {}
    override fun getPassword(): String? = null
    override fun isNewChatHistory(chatRef: String): Boolean = true
    override fun storeChatHistoryStatus(chatRef: String, isNew: Boolean) {}
    override fun getCutOffDateForMarkingMessagesAsSeen(): String? = null

    private fun parseExpiryToMillis(expiryTime: String): Long = try {
        Instant.parse(expiryTime).toEpochMilli()
    } catch (_: Exception) {
        try {
            ZonedDateTime.parse(
                expiryTime,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z z")
            ).toInstant().toEpochMilli()
        } catch (_: Exception) {
            expiryTime.toLongOrNull() ?: 0L
        }
    }
}

interface IAndroidSessionPrefs {
    fun getUsername(): String?
    fun getAccessToken(): String?
    fun getTokenExpiry(): Long
    fun isReadReceiptEnabled(): Boolean
    fun canSharePresenceStatus(): Boolean
}
