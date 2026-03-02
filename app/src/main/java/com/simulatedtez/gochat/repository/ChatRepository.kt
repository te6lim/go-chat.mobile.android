package com.simulatedtez.gochat.repository

import ChatServiceErrorResponse
import com.simulatedtez.gochat.Session
import com.simulatedtez.gochat.UserPreference
import com.simulatedtez.gochat.database.IChatStorage
import com.simulatedtez.gochat.database.toMessages
import com.simulatedtez.gochat.database.toUIMessages
import com.simulatedtez.gochat.listener.ChatEventListener
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.ChatPage
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.remote.api_usecases.CreateChatRoomParams
import com.simulatedtez.gochat.remote.api_usecases.CreateChatRoomUsecase
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.toDBMessage
import com.simulatedtez.gochat.database.ConversationDatabase
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.util.UserPresenceHelper
import com.simulatedtez.gochat.util.newPrivateChat
import com.simulatedtez.gochat.util.toISOString
import io.github.aakira.napier.Napier
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import listeners.ChatEngineEventListener
import okhttp3.Response
import java.time.LocalDateTime
import java.util.UUID

class ChatRepository(
    private val chatInfo: ChatInfo,
    private val createChatRoomUsecase: CreateChatRoomUsecase,
    private val chatDb: IChatStorage,
    private val conversationDB: ConversationDatabase
): ChatEngineEventListener<Message> {

    private val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var timesPaginated = 0
    private var isNewChat = UserPreference.isNewChatHistory(chatInfo.chatReference)

    private var chatEventListener: ChatEventListener? = null
    private var chatService = newPrivateChat(chatInfo, this)

    val userPresenceHelper = UserPresenceHelper(
        chatService, PresenceStatus.ONLINE, chatInfo
    )

    val cutOffForMarkingMessagesAsSeen = UserPreference.getCutOffDateForMarkingMessagesAsSeen()

    fun connectAndSendPendingMessages() {
        context.launch(Dispatchers.IO) {
            val pendingMessages = mutableListOf<Message>()
            pendingMessages.addAll(chatDb.getUndeliveredMessages(
                chatInfo.username, chatInfo.chatReference).toMessages()
            )
            pendingMessages.addAll(
                chatDb.getPendingMessages(chatInfo.chatReference).toMessages()
            )
            context.launch(Dispatchers.Main) {
                createNewChatRoom {
                    chatService.connectAndSend(pendingMessages)
                }
            }
        }
    }

    fun killChatService() {
        chatService.disconnect()
        chatService = ChatEngine.Builder<Message>()
            .build(Message.serializer())
    }

    fun setChatEventListener(listener: ChatEventListener) {
        chatEventListener = listener
    }

    fun markMessagesAsSeen(message: Message) {
        if (message.timestamp > cutOffForMarkingMessagesAsSeen!!) {
            message.seenTimestamp = LocalDateTime.now().toISOString()
            chatService.returnMessage(message)
        }
    }

    fun sendMessageForBackupOrDeletion(message: Message) {
        chatService.sendMessage(message.apply { shouldDelete = true })
    }

    fun sendMessage(message: Message) {
        context.launch(Dispatchers.IO) {
            chatDb.store(message)
            updateConversationLastMessage(message)
        }
        chatService.sendMessage(message)
    }

    fun postMessageStatus(messageStatus: MessageStatus) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            message = "",
            sender = chatInfo.username,
            receiver = chatInfo.recipientsUsernames[0],
            timestamp = LocalDateTime.now().toISOString(),
            chatReference = chatInfo.chatReference,
            messageStatus = messageStatus.name
        )
        chatService.sendMessage(message)
    }

    suspend fun updateConversationLastMessage(message: Message) {
        conversationDB.updateConversationLastMessage(message)
    }

    suspend fun markConversationAsOpened() {
        conversationDB.updateUnreadCountToZero(chatInfo.chatReference)
    }

    suspend fun loadNextPageMessages(): ChatPage {
        val messages = chatDb.loadNextPage(chatInfo.chatReference)
        if (messages.isNotEmpty()) {
            timesPaginated++
        }
        return ChatPage(
            messages = messages.toUIMessages(),
            paginationCount = timesPaginated,
            size = messages.size
        )
    }

    private suspend fun createNewChatRoom(onSuccess: (() -> Unit)) {
        val params = CreateChatRoomParams(
            request = CreateChatRoomParams.Request(
                user = chatInfo.username,
                other = chatInfo.recipientsUsernames[0],
                chatReference = chatInfo.chatReference
            )
        )
        createChatRoomUsecase.call(
            params = params, object:
                IResponseHandler<ParentResponse<String>, IResponse<ParentResponse<String>>> {
                override fun onResponse(response: IResponse<ParentResponse<String>>) {
                    when(response) {
                        is IResponse.Success -> {
                            onSuccess()
                        }
                        is IResponse.Failure -> {
                            Napier.d(response.response?.message ?: "unknown")
                        }
                        else -> {

                        }
                    }
                }
            }
        )
    }

    override fun onClose(code: Int, reason: String) {
        chatEventListener?.onClose(code, reason)
    }

    override fun onConnect() {
        userPresenceHelper.postNewUserPresence(PresenceStatus.ONLINE)
        chatEventListener?.onConnect()
    }

    override fun onDisconnect(t: Throwable, response: Response?) {
        when {
            response?.code == HttpStatusCode.Companion.NotFound.value -> {
                context.launch(Dispatchers.IO) {
                    createNewChatRoom {
                        chatService.connect()
                    }
                }
            } else -> {
                Napier.d(response?.message ?: "unknown")
                chatEventListener?.onDisconnect(t, response)
            }
        }
    }

    override fun onError(response: ChatServiceErrorResponse) {
        chatEventListener?.onError(response)
    }

    override fun onSent(message: Message) {
        when {
            message.presenceStatus.isNullOrEmpty() && message.messageStatus.isNullOrEmpty() -> {
                context.launch(Dispatchers.IO) {
                    chatDb.store(message)
                    val dbMessage = message.toDBMessage()
                    chatDb.setAsSent((dbMessage.id to dbMessage.chatReference))
                }
                chatEventListener?.onMessageSent(message)
                returnMessageToBackendForBackupOrDeletion(message)
            }

            !message.presenceStatus.isNullOrEmpty() -> {
                userPresenceHelper.onPresenceSent(message.id)
            }

            !message.messageStatus.isNullOrEmpty() -> {
            }
        }
    }

    private fun returnMessageToBackendForBackupOrDeletion(message: Message) {
        if (message.isReadReceiptEnabled == true) {
            if (!message.seenTimestamp.isNullOrEmpty()) {
                sendMessageForBackupOrDeletion(message)
            }
        } else {
            if (!message.deliveredTimestamp.isNullOrEmpty()) {
                sendMessageForBackupOrDeletion(message)
            }
        }
    }

    private var lastMessagesFromRecipient = mutableListOf<Message>()

    override fun onReceive(message: Message) {
        PresenceStatus.getType(message.presenceStatus)?.let {
            context.launch(Dispatchers.Main) {
                userPresenceHelper.handlePresenceMessage(
                    it, message.id, message.chatReference
                ) { status ->
                    chatEventListener?.onReceiveRecipientActivityStatusMessage(status)
                }
            }
            return
        }

        MessageStatus.getType(message.messageStatus)?.let {
            context.launch(Dispatchers.Main) {
                chatEventListener?.onReceiveRecipientMessageStatus(message.chatReference,it)
            }
            return
        }

        context.launch(Dispatchers.IO) {
            chatDb.store(message)
        }
        setDeliveredTimestampForMessage(message)
        if (!isNewChat) {
            context.launch(Dispatchers.IO) {
                updateConversationLastMessage(message)
            }
            chatEventListener?.onReceive(message)
        } else {
            UserPreference.storeChatHistoryStatus(chatInfo.chatReference, false)
            isNewChat = false
            if (message.seenTimestamp.isNullOrEmpty()) {
                context.launch(Dispatchers.IO) {
                    updateConversationLastMessage(message)
                }
                chatEventListener?.onReceive(message)
            }
        }
    }

    private fun setDeliveredTimestampForMessage(message: Message) {
        val lastMessageOfTheSameId = lastMessagesFromRecipient.find { it.id == message.id }
        if (lastMessageOfTheSameId == null) {
            message.deliveredTimestamp = LocalDateTime.now().toISOString()
            lastMessagesFromRecipient.add(message)
        } else {
            if (message.deliveredTimestamp.isNullOrEmpty()) {
                message.deliveredTimestamp = lastMessageOfTheSameId.deliveredTimestamp
                lastMessagesFromRecipient.remove(lastMessageOfTheSameId)
            }
        }
    }

    fun isChatServiceConnected(): Boolean {
        return chatService.socketIsConnected
    }

    fun cancel() {
        context.cancel()
    }

    fun buildUnsentMessage(message: String): Message {
        return Message(
            id = UUID.randomUUID().toString(),
            message = message,
            sender = chatInfo.username,
            receiver = chatInfo.recipientsUsernames[0],
            timestamp = LocalDateTime.now().toISOString(),
            chatReference = chatInfo.chatReference,
            isBackedUp = false,
            isReadReceiptEnabled = Session.session.isReadReceiptEnabled
        )
    }
}