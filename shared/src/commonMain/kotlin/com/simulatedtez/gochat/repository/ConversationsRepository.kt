package com.simulatedtez.gochat.repository

import ChatServiceErrorResponse
import com.simulatedtez.gochat.listener.ConversationEventListener
import com.simulatedtez.gochat.model.DBConversation
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.model.response.NewChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.api_interfaces.IChatApiService
import com.simulatedtez.gochat.remote.api_usecases.AddNewChatUsecase
import com.simulatedtez.gochat.remote.api_usecases.CreateConversationsParams
import com.simulatedtez.gochat.remote.api_usecases.CreateConversationsUsecase
import com.simulatedtez.gochat.remote.api_usecases.CreateGroupChatParams
import com.simulatedtez.gochat.remote.api_usecases.CreateGroupChatUsecase
import com.simulatedtez.gochat.remote.api_usecases.StartNewChatParams
import com.simulatedtez.gochat.session.ISession
import com.simulatedtez.gochat.storage.IChatStorage
import com.simulatedtez.gochat.storage.IConversationsStore
import com.simulatedtez.gochat.util.nowAsISOString
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import okhttp3.Response
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ConversationsRepository(
    private val addNewChatUsecase: AddNewChatUsecase,
    createConversationsUsecase: CreateConversationsUsecase,
    private val chatApiService: IChatApiService,
    private val conversationStore: IConversationsStore,
    chatDb: IChatStorage,
    session: ISession,
    private val createGroupChatUsecase: CreateGroupChatUsecase? = null
) : AppWideChatEventListener(createConversationsUsecase, chatDb, session) {

    private var conversationEventListener: ConversationEventListener? = null

    fun setListener(listener: ConversationEventListener) { conversationEventListener = listener }

    suspend fun getConversations(): List<DBConversation> {
        val local = conversationStore.getConversations()
        if (local.isNotEmpty()) return local

        val response = chatApiService.fetchUserConversations(session.username)
        if (response is IResponse.Success) {
            val summaries = response.data?.data ?: return emptyList()
            val seeded = summaries.map { s ->
                val isGroup = s.chatType == "group"
                DBConversation(
                    chatReference = s.chatReference,
                    otherUser = if (isGroup) "" else s.otherUsername,
                    chatType = s.chatType,
                    chatName = if (isGroup) s.otherUsername else ""
                )
            }
            conversationStore.insertConversations(seeded)
            return seeded
        }
        return emptyList()
    }

    suspend fun storeConversation(conversation: DBConversation) =
        conversationStore.insertConversation(conversation)

    suspend fun getOrCreateChatReference(otherUser: String): String? {
        val existing = conversationStore.getConversations().find { it.otherUser == otherUser }
        if (existing != null) return existing.chatReference

        val deferred = CompletableDeferred<String?>()
        addNewChatUsecase.call(
            StartNewChatParams(StartNewChatParams.Request(user = session.username, other = otherUser)),
            object : IResponseHandler<ParentResponse<NewChatResponse>, IResponse<ParentResponse<NewChatResponse>>> {
                override fun onResponse(response: IResponse<ParentResponse<NewChatResponse>>) {
                    when (response) {
                        is IResponse.Success -> {
                            response.data.data?.let { chat ->
                                context.launch {
                                    storeConversation(DBConversation(
                                        otherUser = chat.other,
                                        chatReference = chat.chatReference,
                                        isPendingSentInvite = true
                                    ))
                                    deferred.complete(chat.chatReference)
                                }
                            } ?: deferred.complete(null)
                        }
                        is IResponse.Failure -> {
                            Napier.d("getOrCreateChatReference failed: ${response.reason}")
                            deferred.complete(null)
                        }
                        else -> deferred.complete(null)
                    }
                }
            }
        )
        return deferred.await()
    }

    suspend fun addNewConversation(other: String, messageCount: Int) {
        addNewChat(session.username, other, messageCount) { isAdded ->
            if (isAdded) context.launch { connectToChatService() }
        }
    }

    private suspend fun addNewChat(
        username: String,
        otherUser: String,
        messageCount: Int,
        completion: (Boolean) -> Unit
    ) {
        addNewChatUsecase.call(
            StartNewChatParams(StartNewChatParams.Request(user = username, other = otherUser)),
            object : IResponseHandler<ParentResponse<NewChatResponse>, IResponse<ParentResponse<NewChatResponse>>> {
                override fun onResponse(response: IResponse<ParentResponse<NewChatResponse>>) {
                    when (response) {
                        is IResponse.Success -> {
                            response.data.data?.let { chat ->
                                context.launch {
                                    storeConversation(DBConversation(
                                        otherUser = chat.other,
                                        chatReference = chat.chatReference,
                                        unreadCount = messageCount,
                                        isPendingSentInvite = true
                                    ))
                                    completion(true)
                                    conversationEventListener?.onNewChatAdded(chat)
                                }
                            }
                        }
                        is IResponse.Failure -> context.launch {
                            completion(false)
                            conversationEventListener?.onAddNewChatFailed(response)
                        }
                        else -> {}
                    }
                }
            }
        )
    }

    suspend fun createGroupChat(name: String, participants: List<String>) {
        createGroupChatUsecase?.call(
            CreateGroupChatParams(CreateGroupChatParams.Request(name = name, participants = participants)),
            null
        )
    }

    fun acceptInvite(chatReference: String) {
        session.appWideChatService?.sendMessage(Message(
            id = Uuid.random().toString(), message = "",
            sender = session.username, receiver = "",
            timestamp = nowAsISOString(), chatReference = chatReference,
            messageStatus = MessageStatus.ACCEPT_INVITE.name
        ))
    }

    fun declineInvite(chatReference: String) {
        session.appWideChatService?.sendMessage(Message(
            id = Uuid.random().toString(), message = "",
            sender = session.username, receiver = "",
            timestamp = nowAsISOString(), chatReference = chatReference,
            messageStatus = MessageStatus.DECLINE_INVITE.name
        ))
    }

    suspend fun deleteConversation(chatReference: String) {
        chatApiService.deleteConversation(chatReference)
        conversationStore.deleteConversation(chatReference)
    }

    override suspend fun createNewConversations(onSuccess: () -> Unit) {
        createConversationsUsecase.call(
            CreateConversationsParams(CreateConversationsParams.Request(session.username)),
            object : IResponseHandler<ParentResponse<String>, IResponse<ParentResponse<String>>> {
                override fun onResponse(response: IResponse<ParentResponse<String>>) {
                    when (response) {
                        is IResponse.Success -> onSuccess()
                        is IResponse.Failure -> {
                            Napier.d(response.response?.message ?: "unknown")
                            context.launch { conversationEventListener?.onError(response) }
                        }
                        else -> {}
                    }
                }
            }
        )
    }

    override fun onClose(code: Int, reason: String) { conversationEventListener?.onClose(code, reason) }

    override fun onConnect() {
        userPresenceHelper.postNewUserPresence(PresenceStatus.AWAY)
        conversationEventListener?.onConnect()
    }

    override fun onDisconnect(t: Throwable, response: Response?) {
        conversationEventListener?.onDisconnect(t, response)
    }

    override fun onError(response: ChatServiceErrorResponse<Message>) {
        conversationEventListener?.onError(response)
    }

    override fun onReceive(message: Message) {
        PresenceStatus.getType(message.presenceStatus)?.let {
            context.launch {
                userPresenceHelper.handlePresenceMessage(it, message.id, message.chatReference) {}
            }
            return
        }

        MessageStatus.getType(message.messageStatus)?.let { status ->
            when (status) {
                MessageStatus.CHAT_INVITE -> context.launch {
                    conversationEventListener?.onChatInviteReceived(message)
                }
                MessageStatus.INVITE_ACCEPTED -> context.launch {
                    conversationEventListener?.onInviteAccepted(message.chatReference)
                }
                MessageStatus.INVITE_DECLINED -> context.launch {
                    conversationEventListener?.onInviteDeclined(message.chatReference)
                }
                MessageStatus.INVITE_REVOKED -> context.launch {
                    conversationEventListener?.onInviteRevoked(message.chatReference)
                }
                MessageStatus.GROUP_INVITE -> context.launch {
                    storeConversation(DBConversation(
                        chatReference = message.chatReference,
                        otherUser = "",
                        chatType = "group",
                        chatName = "Group Chat"
                    ))
                    conversationEventListener?.onGroupInviteReceived(message)
                }
                MessageStatus.GROUP_REMOVED -> context.launch {
                    conversationStore.deleteConversation(message.chatReference)
                    conversationEventListener?.onGroupRemoved(message.chatReference)
                }
                else -> context.launch {
                    conversationEventListener?.onReceiveRecipientMessageStatus(message.chatReference, status)
                }
            }
            return
        }

        context.launch { chatDb.store(message) }
        conversationEventListener?.onReceive(message)
    }
}
