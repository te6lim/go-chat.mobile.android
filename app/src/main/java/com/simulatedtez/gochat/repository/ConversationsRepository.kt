package com.simulatedtez.gochat.repository

import ChatServiceErrorResponse
import com.simulatedtez.gochat.Session
import com.simulatedtez.gochat.UserPreference
import com.simulatedtez.gochat.database.IChatStorage
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.database.ConversationDatabase
import com.simulatedtez.gochat.database.DBConversation
import com.simulatedtez.gochat.listener.ConversationEventListener
import com.simulatedtez.gochat.remote.api_interfaces.IChatApiService
import com.simulatedtez.gochat.remote.api_usecases.AddNewChatUsecase
import com.simulatedtez.gochat.remote.api_usecases.CreateConversationsParams
import com.simulatedtez.gochat.remote.api_usecases.CreateConversationsUsecase
import com.simulatedtez.gochat.remote.api_usecases.StartNewChatParams
import com.simulatedtez.gochat.model.response.NewChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import com.simulatedtez.gochat.util.AppWideChatEventListener
import com.simulatedtez.gochat.util.toISOString
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

class ConversationsRepository(
    private val addNewChatUsecase: AddNewChatUsecase,
    createConversationsUsecase: CreateConversationsUsecase,
    private val chatApiService: IChatApiService,
    private val conversationDB: ConversationDatabase,
    chatDb: IChatStorage,
): AppWideChatEventListener(createConversationsUsecase, chatDb) {

    override val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var conversationEventListener: ConversationEventListener? = null

    fun setListener(listener: ConversationEventListener) {
        conversationEventListener = listener
    }

    suspend fun getConversations(): List<DBConversation> {
        return conversationDB.getConversations()
    }

    override suspend fun createNewConversations(onSuccess: (() -> Unit)) {
        val params = CreateConversationsParams(
            request = CreateConversationsParams.Request(
                username = Session.Companion.session.username
            )
        )
        createConversationsUsecase.call(
            params = params, object:
                IResponseHandler<ParentResponse<String>, IResponse<ParentResponse<String>>> {
                override fun onResponse(response: IResponse<ParentResponse<String>>) {
                    when(response) {
                        is IResponse.Success -> {
                            onSuccess()
                        }
                        is IResponse.Failure -> {
                            Napier.d(response.response?.message ?: "unknown")
                            context.launch(Dispatchers.Main) {
                                conversationEventListener?.onError(response)
                            }
                        }
                        else -> {

                        }
                    }
                }
            }
        )
    }

    suspend fun storeConversation(conversation: DBConversation) {
        conversationDB.insertConversation(conversation)
    }

    suspend fun rebuildConversations(newMessage: Message): List<DBConversation> {
        val temporaryConversationList = getConversations().toMutableList()
        val convo = temporaryConversationList.find { newMessage.chatReference == it.chatReference }
        if (convo != null) {
            val conversation = DBConversation(
                otherUser = convo.otherUser,
                chatReference = convo.chatReference,
                lastMessage = newMessage.message,
                timestamp = newMessage.timestamp,
                unreadCount = convo.unreadCount + 1
            )
            temporaryConversationList.remove(convo)
            temporaryConversationList.add(conversation)
            storeConversation(conversation)
        } else {
            addNewConversation(newMessage.sender, 1)
        }
        return temporaryConversationList.sortedBy { it.timestamp }
    }

    private suspend fun addNewChat(username: String, otherUser: String, messageCount: Int, completion: (isSuccess: Boolean) -> Unit) {
        val params = StartNewChatParams(
            request = StartNewChatParams.Request(
                user = username, other = otherUser
            )
        )
        addNewChatUsecase.call(
            params, object:
                IResponseHandler<ParentResponse<NewChatResponse>, IResponse<ParentResponse<NewChatResponse>>> {
            override fun onResponse(response: IResponse<ParentResponse<NewChatResponse>>) {
               when (response) {
                   is IResponse.Success -> {
                       response.data.data?.let {
                           context.launch(Dispatchers.IO) {
                               storeConversation(
                                   DBConversation(
                                       otherUser = it.other,
                                       chatReference = it.chatReference,
                                       lastMessage = "",
                                       timestamp = "",
                                       unreadCount = messageCount,
                                       contactAvi = ""
                                   )
                               )
                           }
                           context.launch(Dispatchers.Main) {
                               completion(true)
                               conversationEventListener?.onNewChatAdded(it)
                           }
                       }
                   }
                   is IResponse.Failure -> {
                       context.launch(Dispatchers.Main) {
                           completion(false)
                           conversationEventListener?.onAddNewChatFailed(response)
                       }
                   }

                   is Response<*> -> {}
               }
            }
        })
    }

    override fun onClose(code: Int, reason: String) {
        conversationEventListener?.onClose(code, reason)
    }

    override fun onConnect() {
        userPresenceHelper.postNewUserPresence(PresenceStatus.AWAY)
        conversationEventListener?.onConnect()
    }

    override fun onDisconnect(t: Throwable, response: okhttp3.Response?) {
        Napier.d(response?.message ?: "unknown")
        conversationEventListener?.onDisconnect(t, response)
    }

    override fun onError(response: ChatServiceErrorResponse<Message>) {
        conversationEventListener?.onError(response)
    }

    private fun isNewChat(chatRef: String): Boolean {
        return UserPreference.isNewChatHistory(chatRef)
    }

    override fun onReceive(message: Message) {
        PresenceStatus.getType(message.presenceStatus)?.let {
            context.launch(Dispatchers.Main) {
                userPresenceHelper.handlePresenceMessage(
                    it, message.id, message.chatReference
                ) {}
            }
            return
        }

        MessageStatus.getType(message.messageStatus)?.let {
            when (it) {
                MessageStatus.CHAT_INVITE -> {
                    context.launch(Dispatchers.Main) {
                        conversationEventListener?.onChatInviteReceived(message)
                    }
                }
                MessageStatus.INVITE_ACCEPTED -> {
                    context.launch(Dispatchers.Main) {
                        conversationEventListener?.onInviteAccepted(message.chatReference)
                    }
                }
                MessageStatus.INVITE_DECLINED -> {
                    context.launch(Dispatchers.Main) {
                        conversationEventListener?.onInviteDeclined(message.chatReference)
                    }
                }
                else -> conversationEventListener?.onReceiveRecipientMessageStatus(message.chatReference, it)
            }
            return
        }

        context.launch(Dispatchers.IO) {
            chatDb.store(message)
        }
        if (!isNewChat(message.chatReference)) {
            conversationEventListener?.onReceive(message)
        } else {
            UserPreference.storeChatHistoryStatus(
                message.chatReference, false)
            conversationEventListener?.onReceive(message)
        }
    }

    suspend fun deleteConversation(chatReference: String) {
        chatApiService.deleteConversation(chatReference)
        conversationDB.deleteConversation(chatReference)
    }

    fun acceptInvite(chatReference: String) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            message = "",
            sender = Session.session.username,
            receiver = "",
            timestamp = LocalDateTime.now().toISOString(),
            chatReference = chatReference,
            messageStatus = MessageStatus.ACCEPT_INVITE.name
        )
        Session.session.appWideChatService?.sendMessage(message)
    }

    fun declineInvite(chatReference: String) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            message = "",
            sender = Session.session.username,
            receiver = "",
            timestamp = LocalDateTime.now().toISOString(),
            chatReference = chatReference,
            messageStatus = MessageStatus.DECLINE_INVITE.name
        )
        Session.session.appWideChatService?.sendMessage(message)
    }

    suspend fun addNewConversation(other: String, messageCount: Int) {
        addNewChat(Session.session.username, other, messageCount) { isAdded ->
            if (isAdded) {
                context.launch(Dispatchers.IO) {
                    connectToChatService()
                }
            }
        }
    }
}