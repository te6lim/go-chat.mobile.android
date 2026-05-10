package com.simulatedtez.gochat.database

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.Conversation

class ConversationDatabase private constructor(private val conversationsDao: ConversationDao) {

    companion object {
        private var instance: ConversationDatabase? = null
        fun get(context: Context): ConversationDatabase {
            return instance ?: synchronized(this) {
                ConversationDatabase(
                    AppDatabase.getInstance(context).conversationsDao()
                )
            }
        }
    }

    suspend fun deleteAllConversations() {
        conversationsDao.deleteAllConversations()
    }

    suspend fun getConversations(): List<DBConversation> {
        return conversationsDao.getAll()
    }

    suspend fun updateConversationLastMessage(message: Message) {
        conversationsDao.updateConversationLastMessage(
            message.chatReference, message.message, message.timestamp
        )
    }

    suspend fun updateUnreadCountToZero(chatRef: String) {
        conversationsDao.updateUnreadCountToZero(chatRef)
    }

    suspend fun insertConversation(convo: DBConversation) {
        conversationsDao.insert(convo)
    }

    suspend fun insertConversations(convos: List<DBConversation>) {
        conversationsDao.insert(convos)
    }

    suspend fun deleteConversation(chatRef: String) {
        conversationsDao.deleteByChatReference(chatRef)
    }

    suspend fun markAsPendingSentInvite(chatRef: String, isPending: Boolean) {
        conversationsDao.updatePendingSentInvite(chatRef, isPending)
    }
}

@Entity(tableName = "conversations")
data class DBConversation(
    @PrimaryKey
    @ColumnInfo("chatReference")
    val chatReference: String,
    @ColumnInfo("otherUser")
    val otherUser: String,
    @ColumnInfo("lastMessage")
    val lastMessage: String = "",
    @ColumnInfo("timestamp")
    var timestamp: String = "",
    @ColumnInfo("unreadCount")
    var unreadCount: Int = 0,
    @ColumnInfo("contactAvi")
    val contactAvi: String = "",
    @ColumnInfo("isPendingSentInvite")
    val isPendingSentInvite: Boolean = false,
    @ColumnInfo("chatType")
    val chatType: String = "private",
    @ColumnInfo("chatName")
    val chatName: String = "",
)

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE chatReference = :chatRef")
    suspend fun getByChatReference(chatRef: String): DBConversation

    @Query("UPDATE conversations SET lastMessage = :message, timestamp = :timestamp WHERE chatReference = :chatRef")
    suspend fun updateConversationLastMessage(chatRef: String, message: String, timestamp: String)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE chatReference =:chatRef")
    suspend fun updateUnreadCountToZero(chatRef: String)

    @Query("UPDATE conversations SET isPendingSentInvite = :isPending WHERE chatReference = :chatRef")
    suspend fun updatePendingSentInvite(chatRef: String, isPending: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conv: DBConversation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(convos: List<DBConversation>)

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    suspend fun getAll(): List<DBConversation>

    @Query("DELETE FROM conversations WHERE chatReference = :chatRef")
    suspend fun deleteByChatReference(chatRef: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}

fun DBConversation.toConversation(): Conversation {
    return Conversation(
        other = otherUser,
        chatReference = chatReference,
        lastMessage = lastMessage,
        timestamp = timestamp,
        unreadCount = unreadCount,
        contactAvi = contactAvi,
        isPendingSentInvite = isPendingSentInvite,
        chatType = chatType,
        chatName = chatName
    )
}

fun List<DBConversation>.toConversations(): List<Conversation> {
    return map {
        it.toConversation()
    }
}
