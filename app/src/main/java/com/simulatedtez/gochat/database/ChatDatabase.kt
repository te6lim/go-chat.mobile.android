package com.simulatedtez.gochat.database

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.ui.UIMessage
import com.simulatedtez.gochat.model.Message
import models.ComparableMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatDatabase private constructor(private val messagesDao: MessagesDao): IChatStorage {

    private var pageSize: Int? = null
    private var timestampsOfLastMessageInPages = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()
    ).format(Date())

    companion object {
        private const val PAGE_SIZE = 1000

        fun get(context: Context): ChatDatabase {
            return synchronized(this) {
                ChatDatabase(AppDatabase.getInstance(context).messagesDao())
            }
        }
    }

    override suspend fun isEmpty(chatRef: String): Boolean {
        return messagesDao.getAny(chatRef) != null
    }
    override suspend fun loadNextPage(chatRef: String): List<DBMessage> {
        return timestampsOfLastMessageInPages.let { topMessageTimestamp ->
            messagesDao.getMessages(
                chatRef = chatRef,
                topMessageTimestamp,
                pageSize ?: PAGE_SIZE).sortedBy {
                it.timestamp
            }.also {
                if (it.isNotEmpty()) {
                    timestampsOfLastMessageInPages = it.first().timestamp
                }
            }
        }
    }

    override suspend fun getMessage(messageRef: String): DBMessage? {
        return messagesDao.getMessage(messageRef)
    }

    override suspend fun getPendingMessages(chatRef: String): List<DBMessage> {
        return messagesDao.getPendingMessages(session.username, chatRef)
    }

    override suspend fun deleteAllMessages() {
        messagesDao.deleteAllMessages()
    }

    override suspend fun getUndeliveredMessages(username: String, chatRef: String): List<DBMessage> {
        return messagesDao.getUndeliveredMessages(username, chatRef)
    }

    override suspend fun setAsSeen(vararg messageRefToChatRef: Pair<String, String>) {
        messageRefToChatRef.forEach { pair ->
            messagesDao.updateAsSeen(pair.first, pair.second)
        }
    }

    override suspend fun setAsSent(vararg messageRefToChatRef: Pair<String, String>) {
        messageRefToChatRef.forEach { pair ->
            messagesDao.updateAsSent(pair.first, pair.second)
        }
    }

    override suspend fun store(message: Message) {
        messagesDao.insertMessage(message.toRoomMessage().apply {
            if (sender == session.username) isSent = false
        })
    }

    override suspend fun store(messages: List<Message>) {
        messagesDao.insertMessages(messages.toRoomMessages().apply {
            forEach {
                if (it.sender == session.username) it.isSent = false
            }
        })
    }

}

@Entity(tableName = "messages")
class DBMessage(
    @PrimaryKey @ColumnInfo(name = "messageReference") override val id: String,
    @ColumnInfo(name = "textMessage") override val message: String,
    @ColumnInfo("senderUsername") override val sender: String,
    @ColumnInfo("receiverUsername") override val receiver: String,
    @ColumnInfo("messageTimestamp") override var timestamp: String,
    @ColumnInfo("chatReference") val chatReference: String,
    @ColumnInfo("ack") val ack: Boolean? = null,
    @ColumnInfo("deliveredTimestamp") val deliveredTimestamp: String? = null,
    @ColumnInfo("seenTimestamp") val seenTimestamp: String? = null,
    @ColumnInfo("isSent") var isSent: Boolean?,
    @ColumnInfo("isReadReceiptEnabled") var isReadReceiptEnabled: Boolean?
): ComparableMessage()

fun Message.toRoomMessage(): DBMessage = DBMessage(
    id = id,
    message = message,
    sender = sender,
    receiver = receiver,
    timestamp = timestamp,
    chatReference = chatReference,
    deliveredTimestamp = deliveredTimestamp,
    seenTimestamp = seenTimestamp,
    isSent = null,
    isReadReceiptEnabled = isReadReceiptEnabled
)

fun List<Message>.toRoomMessages(): List<DBMessage> = map { it.toRoomMessage() }

fun DBMessage.toUIMessage(): UIMessage {
    return UIMessage(
        message = this.toMessage(),
        status = when {
            seenTimestamp != null -> MessageStatus.SEEN
            deliveredTimestamp != null -> MessageStatus.DELIVERED
            isSent == true -> MessageStatus.SENT
            else -> MessageStatus.SENDING
        }
    )
}

fun List<DBMessage>.toUIMessages(): List<UIMessage> {
    return map {
        it.toUIMessage()
    }
}

fun DBMessage.toMessage(): Message {
    return Message(
        id = id,
        sender = sender,
        receiver = receiver,
        message = message,
        chatReference = chatReference,
        timestamp = timestamp,
        deliveredTimestamp = deliveredTimestamp,
        seenTimestamp = seenTimestamp
    )
}

fun List<DBMessage>.toMessages(): List<Message> {
    return map {
        it.toMessage()
    }
}


@Dao
interface MessagesDao {

    @Query("SELECT * FROM messages WHERE chatReference =:chatRef AND senderUsername =:username AND deliveredTimestamp IS NULL")
    suspend fun getUndeliveredMessages(username: String, chatRef: String): List<DBMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DBMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<DBMessage>)

    @Query("UPDATE messages SET ack = 1 WHERE chatReference =:chatRef AND messageReference =:messageRef")
    suspend fun updateAsSeen(messageRef: String, chatRef: String)

    @Query("UPDATE messages SET isSent = 1 WHERE chatReference =:chatRef AND messageReference =:messageRef")
    suspend fun updateAsSent(messageRef: String, chatRef: String)

    @Query("SELECT * FROM messages WHERE " +
            "chatReference =:chatRef AND messageTimestamp < :startTimestamp ORDER BY messageTimestamp DESC LIMIT :size")
    suspend fun getMessages(chatRef: String, startTimestamp: String, size: Int): List<DBMessage>

    @Query("SELECT * FROM messages WHERE chatReference =:chatRef ORDER BY messageTimestamp DESC LIMIT 1")
    suspend fun getLastMessage(chatRef: String): DBMessage?

    @Query("SELECT * FROM messages WHERE chatReference =:chatRef LIMIT 1")
    suspend fun getAny(chatRef: String): DBMessage?

    @Query("SELECT * FROM messages WHERE chatReference =:chatRef AND isSent = 0 AND senderUsername =:username")
    suspend fun getPendingMessages(username: String, chatRef: String): List<DBMessage>

    @Query("SELECT * FROM messages WHERE messageReference =:messageRef")
    suspend fun getMessage(messageRef: String): DBMessage?

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}