package com.simulatedtez.gochat.view_model

import ChatServiceErrorResponse
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.database.ChatDatabase
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.listener.ChatEventListener
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.repository.ChatRepository
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.ChatPage
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.ui.UIMessage
import com.simulatedtez.gochat.remote.api_services.ChatApiService
import com.simulatedtez.gochat.remote.api_usecases.CreateChatRoomUsecase
import com.simulatedtez.gochat.model.toUIMessage
import com.simulatedtez.gochat.database.ConversationDatabase
import com.simulatedtez.gochat.remote.client
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Response
import java.util.LinkedList
import java.util.Queue

class ChatViewModel(
    private val chatInfo: ChatInfo,
    private val chatRepo: ChatRepository
): ViewModel(), ChatEventListener {

    companion object {
        const val TYPING_TOTAL_TIME = 1
    }

    private val _isUserTyping = MutableLiveData(false)
    val isUserTyping: LiveData<Boolean> = _isUserTyping

    private val _typingTimeLeft = MutableLiveData<Int?>(null)
    val typingTimeLeft: LiveData<Int?> = _typingTimeLeft

    var initialCharCount = 0
    var newCharCount = 0

    val isTyping: Boolean
        get() {
            return initialCharCount < newCharCount
        }

    private val _messagesSent = MutableLiveData<UIMessage>()
    val messagesSent: LiveData<UIMessage> = _messagesSent

    private val _newMessage = MutableLiveData<UIMessage>()
    val newMessage: LiveData<UIMessage> = _newMessage

    private val _pagedMessages = MutableLiveData<ChatPage>()
    val pagedMessages: LiveData<ChatPage> = _pagedMessages

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _sendMessageAttempt = MutableLiveData<UIMessage?>()
    val sendMessageAttempt: LiveData<UIMessage?> = _sendMessageAttempt

    private val _recipientStatus = MutableLiveData<PresenceStatus>()
    val recipientStatus: LiveData<PresenceStatus> = _recipientStatus

    private val _isInvitePending = MutableLiveData(false)
    val isInvitePending: LiveData<Boolean> = _isInvitePending

    private val _recipientMessageStatus = MutableLiveData<MessageStatus?>()
    val recipientMessageStatus: LiveData<MessageStatus?> = _recipientMessageStatus

    private val sentMessagesQueue: Queue<Message> = LinkedList()
    private val receivedMessagesQueue: Queue<Message> = LinkedList()

    fun stopTypingTimer() {
        _typingTimeLeft.value = null
    }

    fun restartTypingTimer(charCount: Int) {
        newCharCount = charCount
        _typingTimeLeft.value = TYPING_TOTAL_TIME
    }

    fun countdownTypingTimeBy(amount: Int) {
        initialCharCount = newCharCount
        _typingTimeLeft.value = _typingTimeLeft.value?.minus(amount)
    }

    fun resetSendAttempt() {
        _sendMessageAttempt.value = null
    }

    fun loadMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            _pagedMessages.postValue(chatRepo.loadNextPageMessages())
        }
    }

    fun sendMessage(message: String) {
        val message = chatRepo.buildUnsentMessage(message)
        _sendMessageAttempt.value = message.toUIMessage(false)
        chatRepo.sendMessage(message)
    }

    fun postMessageStatus(messageStatus: MessageStatus) {
        chatRepo.postMessageStatus(messageStatus)
    }

    fun postPresence(presenceStatus: PresenceStatus) {
        chatRepo.userPresenceHelper.postPresence(
            presenceStatus, chatInfo.chatReference
        )
    }

    fun markConversationAsOpened() {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepo.markConversationAsOpened()
        }
    }

    fun markMessagesAsSeenIfEnabled(messages: List<Message>) {
        messages.forEach {
            if (it.seenTimestamp.isNullOrEmpty()) {
                if (it.isReadReceiptEnabled == true) {
                    chatRepo.markMessagesAsSeen(it)
                }
            }
        }
    }

    fun connectAndSendPendingMessages() {
        chatRepo.connectAndSendPendingMessages()
    }

    fun deleteMessage(message: com.simulatedtez.gochat.model.Message) {
        chatRepo.sendMessageForBackupOrDeletion(message)
    }

    fun exitChat() {
        chatRepo.killChatService()
    }

    override fun onClose(code: Int, reason: String) {
        _isConnected.value = false
    }

    override fun onSend(message: Message) {
        Napier.d("message: ${message.message} sent to ${chatInfo.recipientsUsernames[0]}")
    }

    override fun onConnect() {
        Napier.d("socket connected")
        _isConnected.value = true
    }

    override fun onDisconnect(t: Throwable, response: Response?) {
        Napier.d("socket disconnected")
        _isConnected.value = false
    }

    override fun onError(error: ChatServiceErrorResponse<Message>) {
        Napier.d(error.reason)
    }

    override fun onReceiveRecipientActivityStatusMessage(presenceStatus: PresenceStatus) {
        _recipientStatus.value = presenceStatus
    }

    override fun onReceiveRecipientMessageStatus(chatRef: String, messageStatus: MessageStatus) {
        when (messageStatus) {
            MessageStatus.TYPING -> _isUserTyping.value = true
            MessageStatus.INVITE_PENDING -> _isInvitePending.value = true
            MessageStatus.DELIVERED -> {
                _isUserTyping.value = false
                _recipientMessageStatus.value = MessageStatus.DELIVERED
            }
            MessageStatus.SEEN -> {
                _isUserTyping.value = false
                _recipientMessageStatus.value = MessageStatus.SEEN
            }
            else -> _isUserTyping.value = false
        }
    }

    fun resetRecipientMessageStatus() {
        _recipientMessageStatus.value = null
    }

    fun popSentMessagesQueue() {
        if (sentMessagesQueue.isNotEmpty()) {
            sentMessagesQueue.remove()
            sentMessagesQueue.peek()?.let {
                _messagesSent.value = it.toUIMessage(true)
            }
        }
    }

    fun popReceivedMessagesQueue() {
        if (receivedMessagesQueue.isNotEmpty()) {
            receivedMessagesQueue.remove()
            receivedMessagesQueue.peek()?.let {
                _newMessage.value = it.toUIMessage(true)
            }
        }
    }

    override fun onMessageSent(message: Message) {
        if (sentMessagesQueue.isEmpty()) {
            sentMessagesQueue.add(message)
            _messagesSent.value = message.toUIMessage(true)
        } else {
            sentMessagesQueue.add(message)
        }
    }

    override fun onReceive(message: Message) {
        _isUserTyping.postValue(false)
        if (receivedMessagesQueue.isEmpty()) {
            receivedMessagesQueue.add(message)
            _newMessage.value = message.toUIMessage(true)
        } else {
            receivedMessagesQueue.add(message)
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        chatRepo.cancel()
    }

    fun isChatServiceConnected(): Boolean {
        return chatRepo.isChatServiceConnected()
    }

    fun onUserPresenceOnline(action: () -> Unit) {
        when (chatRepo.userPresenceHelper.presenceStatus) {
            PresenceStatus.ONLINE -> {
                action()
            }
            else -> {}
        }
    }
}

class ChatViewModelProvider(
    private val chatInfo: ChatInfo, private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = ChatRepository(
            chatInfo = chatInfo,
            CreateChatRoomUsecase(ChatApiService(client)),
            chatDb = ChatDatabase.get(context),
            ConversationDatabase.get(context)
        )

        val chatViewModel = ChatViewModel(chatInfo, repo).apply {
            repo.setChatEventListener(this)
        }
        return chatViewModel as T
    }
}