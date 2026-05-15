package com.simulatedtez.gochat.repository

import ChatEngine
import ChatServiceErrorResponse
import com.simulatedtez.gochat.listener.ChatEventListener
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.ChatPage
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.model.toDBMessage
import com.simulatedtez.gochat.model.toMessages
import com.simulatedtez.gochat.model.ui.UIMessage
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.api_usecases.CreateChatRoomParams
import com.simulatedtez.gochat.remote.api_usecases.CreateChatRoomUsecase
import com.simulatedtez.gochat.session.ISession
import com.simulatedtez.gochat.storage.IChatStorage
import com.simulatedtez.gochat.storage.IConversationsStore
import com.simulatedtez.gochat.util.UserPresenceHelper
import com.simulatedtez.gochat.util.newPrivateChat
import com.simulatedtez.gochat.util.nowAsISOString
import io.github.aakira.napier.Napier
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import listeners.ChatEngineEventListener
import okhttp3.Response
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ChatRepository(
    private val chatInfo: ChatInfo,
    private val createChatRoomUsecase: CreateChatRoomUsecase,
    private val chatDb: IChatStorage,
    private val conversationStore: IConversationsStore,
    private val session: ISession,
    private val config: com.simulatedtez.gochat.config.IConfig
) : ChatEngineEventListener<Message> {

    private val context = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timesPaginated = 0
    private var isNewChat = session.isNewChatHistory(chatInfo.chatReference)
    private val cutOffForMarkingMessagesAsSeen = session.getCutOffDateForMarkingMessagesAsSeen()

    private var chatEventListener: ChatEventListener? = null
    private var chatService = newPrivateChat(chatInfo, this, config, session.username)

    val userPresenceHelper = UserPresenceHelper(chatService, PresenceStatus.ONLINE, chatInfo, session)

    fun connectAndSendPendingMessages() {
        context.launch {
            val pending = mutableListOf<Message>()
            pending.addAll(chatDb.getUndeliveredMessages(chatInfo.username, chatInfo.chatReference).toMessages())
            pending.addAll(chatDb.getPendingMessages(chatInfo.chatReference).toMessages())
            chatService.resetReconnectionDelay()
            createNewChatRoom { chatService.connectAndSend(pending) }
        }
    }

    fun killChatService() {
        chatService.disconnect()
        chatService = ChatEngine.Builder<Message>().build(Message.serializer())
    }

    fun setChatEventListener(listener: ChatEventListener) { chatEventListener = listener }

    fun sendMessage(message: Message) {
        context.launch {
            chatDb.store(message)
            conversationStore.updateConversationLastMessage(message)
        }
        chatService.sendMessage(message)
    }

    fun postMessageStatus(messageStatus: MessageStatus) {
        chatService.sendMessage(Message(
            id = Uuid.random().toString(),
            message = "",
            sender = chatInfo.username,
            receiver = chatInfo.recipientsUsernames[0],
            timestamp = nowAsISOString(),
            chatReference = chatInfo.chatReference,
            messageStatus = messageStatus.name
        ))
    }

    fun markMessagesAsSeen(message: Message) {
        val cutOff = cutOffForMarkingMessagesAsSeen ?: return
        if (message.timestamp > cutOff) {
            message.seenTimestamp = nowAsISOString()
            chatService.returnMessage(message)
        }
    }

    fun sendMessageForBackupOrDeletion(message: Message) {
        chatService.sendMessage(message.apply { shouldDelete = true })
    }

    suspend fun markConversationAsOpened() {
        conversationStore.updateUnreadCountToZero(chatInfo.chatReference)
    }

    suspend fun loadNextPageMessages(): ChatPage {
        val messages = chatDb.loadNextPage(chatInfo.chatReference)
        if (messages.isNotEmpty()) timesPaginated++
        val uiMessages = messages.map { dbMsg ->
            UIMessage(
                message = Message(
                    id = dbMsg.id, message = dbMsg.message, sender = dbMsg.sender,
                    receiver = dbMsg.receiver, timestamp = dbMsg.timestamp,
                    chatReference = dbMsg.chatReference,
                    deliveredTimestamp = dbMsg.deliveredTimestamp,
                    seenTimestamp = dbMsg.seenTimestamp
                ),
                status = when {
                    dbMsg.seenTimestamp != null -> MessageStatus.SEEN
                    dbMsg.deliveredTimestamp != null -> MessageStatus.DELIVERED
                    dbMsg.isSent == true -> MessageStatus.SENT
                    else -> MessageStatus.SENDING
                }
            )
        }
        return ChatPage(messages = uiMessages, paginationCount = timesPaginated, size = messages.size)
    }

    fun isChatServiceConnected(): Boolean = chatService.socketIsConnected

    fun buildUnsentMessage(text: String): Message = Message(
        id = Uuid.random().toString(),
        message = text,
        sender = chatInfo.username,
        receiver = chatInfo.recipientsUsernames[0],
        timestamp = nowAsISOString(),
        chatReference = chatInfo.chatReference,
        isReadReceiptEnabled = session.isReadReceiptEnabled
    )

    fun cancel() { context.cancel() }

    private suspend fun createNewChatRoom(onSuccess: () -> Unit) {
        createChatRoomUsecase.call(
            CreateChatRoomParams(CreateChatRoomParams.Request(
                user = chatInfo.username,
                other = chatInfo.recipientsUsernames[0],
                chatReference = chatInfo.chatReference
            )),
            object : IResponseHandler<ParentResponse<String>, IResponse<ParentResponse<String>>> {
                override fun onResponse(response: IResponse<ParentResponse<String>>) {
                    when (response) {
                        is IResponse.Success -> onSuccess()
                        is IResponse.Failure -> Napier.d(response.response?.message ?: "unknown")
                        else -> {}
                    }
                }
            }
        )
    }

    override fun onClose(code: Int, reason: String) { chatEventListener?.onClose(code, reason) }

    override fun onConnect() {
        userPresenceHelper.postNewUserPresence(PresenceStatus.ONLINE)
        chatEventListener?.onConnect()
    }

    override fun onDisconnect(t: Throwable, response: Response?) {
        if (response?.code == HttpStatusCode.NotFound.value) {
            context.launch { createNewChatRoom { chatService.connect() } }
        } else {
            chatEventListener?.onDisconnect(t, response)
        }
    }

    override fun onError(response: ChatServiceErrorResponse<Message>) {
        chatEventListener?.onError(response)
    }

    override fun onSent(message: Message) {
        when {
            message.presenceStatus.isNullOrEmpty() && message.messageStatus.isNullOrEmpty() -> {
                context.launch {
                    chatDb.store(message)
                    val dbMessage = message.toDBMessage()
                    chatDb.setAsSent(dbMessage.id to dbMessage.chatReference)
                }
                chatEventListener?.onMessageSent(message)
                returnMessageToBackendIfNeeded(message)
            }
            !message.presenceStatus.isNullOrEmpty() -> userPresenceHelper.onPresenceSent(message.id)
            else -> {}
        }
    }

    private fun returnMessageToBackendIfNeeded(message: Message) {
        if (message.isReadReceiptEnabled == true) {
            if (!message.seenTimestamp.isNullOrEmpty()) sendMessageForBackupOrDeletion(message)
        } else {
            if (!message.deliveredTimestamp.isNullOrEmpty()) sendMessageForBackupOrDeletion(message)
        }
    }

    private val lastMessagesFromRecipient = mutableListOf<Message>()

    override fun onReceive(message: Message) {
        PresenceStatus.getType(message.presenceStatus)?.let {
            context.launch {
                userPresenceHelper.handlePresenceMessage(it, message.id, message.chatReference) { status ->
                    chatEventListener?.onReceiveRecipientActivityStatusMessage(status)
                }
            }
            return
        }

        MessageStatus.getType(message.messageStatus)?.let {
            context.launch { chatEventListener?.onReceiveRecipientMessageStatus(message.chatReference, it) }
            return
        }

        if (message.sender == chatInfo.username) {
            val status = when {
                !message.seenTimestamp.isNullOrEmpty() -> MessageStatus.SEEN
                !message.deliveredTimestamp.isNullOrEmpty() -> MessageStatus.DELIVERED
                else -> null
            }
            status?.let {
                context.launch { chatEventListener?.onReceiveRecipientMessageStatus(message.chatReference, it) }
            }
            return
        }

        context.launch { chatDb.store(message) }
        setDeliveredTimestamp(message)

        if (!isNewChat) {
            context.launch { conversationStore.updateConversationLastMessage(message) }
            chatEventListener?.onReceive(message)
        } else {
            session.storeChatHistoryStatus(chatInfo.chatReference, false)
            isNewChat = false
            if (message.seenTimestamp.isNullOrEmpty()) {
                context.launch { conversationStore.updateConversationLastMessage(message) }
                chatEventListener?.onReceive(message)
            }
        }
    }

    private fun setDeliveredTimestamp(message: Message) {
        val existing = lastMessagesFromRecipient.find { it.id == message.id }
        if (existing == null) {
            message.deliveredTimestamp = nowAsISOString()
            lastMessagesFromRecipient.add(message)
        } else {
            if (message.deliveredTimestamp.isNullOrEmpty()) {
                message.deliveredTimestamp = existing.deliveredTimestamp
                lastMessagesFromRecipient.remove(existing)
            }
        }
    }
}
