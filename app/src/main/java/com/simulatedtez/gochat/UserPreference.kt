package com.simulatedtez.gochat

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.simulatedtez.gochat.util.toISOString
import java.time.LocalDateTime

object UserPreference {
    private const val NAME = "user_pref"
    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    fun isBackupEnabled(): Boolean {
        return preferences.getBoolean(BACKUP_MESSAGES, false)
    }

    fun enableChatBackup() {
        preferences.edit {
            putBoolean(BACKUP_MESSAGES, true)
        }
    }

    fun canSharePresenceStatus(): Boolean {
        return preferences.getBoolean(PRESENCE_SHARING_TOGGLE, true)
    }

    fun presenceSharingToggle(isEnabled: Boolean) {
        preferences.edit {
            putBoolean(PRESENCE_SHARING_TOGGLE, isEnabled)
        }
    }

    fun readReceiptToggle(setEnabled: Boolean) {
        preferences.edit {
            putBoolean(READ_RECEIPT_TOGGLE, setEnabled)
        }
    }

    fun isReadReceiptEnabled(): Boolean {
        return preferences.getBoolean(READ_RECEIPT_TOGGLE, true)
    }

    fun storeCutOffDateForMarkingMessagesAsSeen(date: LocalDateTime) {
        preferences.edit {
            putString(CUTOFF_DATE_FOR_MARKING_MESSAGES_AS_SEEN, date.toISOString())
        }
    }

    fun getCutOffDateForMarkingMessagesAsSeen(): String? {
        return preferences.getString(
            CUTOFF_DATE_FOR_MARKING_MESSAGES_AS_SEEN, null
        )
    }

    fun storeUsername(value: String) {
        preferences.edit { putString(USERNAME_PREF, value) }
    }

    fun getUsername(): String? {
        return preferences.getString(USERNAME_PREF, null)
    }

    fun deleteUsername() {
        preferences.edit {
            remove(USERNAME_PREF)
        }
    }

    fun storeAccessToken(value: String) {
        preferences.edit {
            putString(ACCESS_TOKEN_PREF, value)
        }
    }

    fun getAccessToken(): String? {
        return preferences.getString(ACCESS_TOKEN_PREF, null)
    }

    fun deleteAccessToken() {
        preferences.edit {
            remove(ACCESS_TOKEN_PREF)
        }
    }

    fun storePassword(value: String) {
        preferences.edit {
            putString(PASSWORD_PREF, value)
        }
    }

    fun getPassword(): String? {
        return preferences.getString(PASSWORD_PREF, null)
    }

    fun deletePassword() {
        preferences.edit {
            remove(PASSWORD_PREF)
        }
    }

    fun storeChatHistoryStatus(chatRef: String, isNew: Boolean) {
        preferences.edit {
            putBoolean(chatRef, isNew)
        }
    }

    fun isNewChatHistory(chatRef: String): Boolean {
        return preferences.getBoolean(chatRef, true)
    }
}

const val BACKUP_MESSAGES = "backup-chat"
const val PRESENCE_SHARING_TOGGLE = "presence-sharing-toggle"
const val READ_RECEIPT_TOGGLE = "read-receipt-toggle"
const val ACCESS_TOKEN_PREF = "access-token-pref"
const val USERNAME_PREF = "username-pref"
const val PASSWORD_PREF = "password-pref"
const val CUTOFF_DATE_FOR_MARKING_MESSAGES_AS_SEEN = "cut-off date for marking messages as seen"