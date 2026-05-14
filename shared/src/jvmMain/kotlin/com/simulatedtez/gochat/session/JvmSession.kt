package com.simulatedtez.gochat.session

import ChatEngine
import com.simulatedtez.gochat.model.Message
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object JvmSession : ISession {
    override var username: String = ""
    override var accessToken: String = ""
    override var tokenExpiryMs: Long = 0L
    override var isReadReceiptEnabled: Boolean = false
    override var canSharePresenceStatus: Boolean = false
    override var appWideChatService: ChatEngine<Message>? = null

    override fun saveTokenDetails(accessToken: String, expiryTime: String) {
        this.accessToken = accessToken
        val expiryMs = parseExpiryToMillis(expiryTime)
        if (expiryMs > 0L) tokenExpiryMs = expiryMs
    }

    override fun saveUsername(username: String) { this.username = username }
    override fun savePassword(password: String) {}
    override fun getPassword(): String? = null
    override fun isNewChatHistory(chatRef: String): Boolean = false
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
