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
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.remote.api_services.ChatApiService
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.response.NewChatResponse
import com.simulatedtez.gochat.database.ConversationDatabase
import com.simulatedtez.gochat.database.DBConversation
import com.simulatedtez.gochat.listener.ConversationEventListener
import com.simulatedtez.gochat.remote.api_services.ConversationsService
import com.simulatedtez.gochat.remote.api_usecases.AddNewChatUsecase
import com.simulatedtez.gochat.remote.api_usecases.CreateConversationsUsecase
import com.simulatedtez.gochat.repository.ConversationsRepository
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.client
import io.github.aakira.napier.Napier
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import okhttp3.Response
import java.util.LinkedList
import java.util.Queue

class ConversationsViewModel(
    private val conversationsRepository: ConversationsRepository
): ViewModel(), ConversationEventListener {

    private val _tokenExpired = MutableLiveData<Boolean>()
    val tokenExpired: LiveData<Boolean> = _tokenExpired

    private val _waiting = MutableLiveData<Boolean>()
    val waiting: LiveData<Boolean> = _waiting

    private val _newConversation = Channel<DBConversation>()
    val newConversation = _newConversation.receiveAsFlow()

    private val _conversations = MutableLiveData<List<DBConversation>>()
    val conversations: LiveData<List<DBConversation>> = _conversations

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _isConnected = MutableLiveData<Boolean>()
    val isConnected: LiveData<Boolean> = _isConnected

    private val _isUserTyping = MutableLiveData<Pair<String, Boolean>>()
    val isUserTyping: LiveData<Pair<String, Boolean>> = _isUserTyping

    private val _pendingInvites = MutableLiveData<List<Message>>(emptyList())
    val pendingInvites: LiveData<List<Message>> = _pendingInvites

    private val _acceptedInviteChat = Channel<ChatInfo>(Channel.BUFFERED)
    val acceptedInviteChat = _acceptedInviteChat.receiveAsFlow()

    private val receivedMessagesQueue: Queue<Message> = LinkedList()

    fun resetTokenExpired() {
        _tokenExpired.value = false
    }

    fun fetchConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            _conversations.postValue(
                conversationsRepository.getConversations().toMutableList()
            )
        }
    }

    fun addNewConversation(other: String, messageCount: Int) {
        _waiting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            conversationsRepository.addNewConversation(other, messageCount)
        }
    }

    fun deleteConversation(chatReference: String) {
        viewModelScope.launch(Dispatchers.IO) {
            conversationsRepository.deleteConversation(chatReference)
        }
    }

    fun acceptInvite(chatReference: String) {
        val invite = _pendingInvites.value?.find { it.chatReference == chatReference } ?: return
        _pendingInvites.value = _pendingInvites.value?.filter { it.chatReference != chatReference }
        conversationsRepository.acceptInvite(chatReference)
        viewModelScope.launch(Dispatchers.IO) {
            conversationsRepository.storeConversation(
                DBConversation(chatReference = chatReference, otherUser = invite.sender)
            )
            _acceptedInviteChat.send(
                ChatInfo(
                    username = session.username,
                    recipientsUsernames = listOf(invite.sender),
                    chatReference = chatReference
                )
            )
        }
    }

    fun declineInvite(chatReference: String) {
        _pendingInvites.value = _pendingInvites.value?.filter { it.chatReference != chatReference }
        conversationsRepository.declineInvite(chatReference)
    }

    fun resetErrorMessage() {
        _errorMessage.value = null
    }

    override fun onAddNewChatFailed(error: IResponse.Failure<ParentResponse<NewChatResponse>>) {
        _waiting.value = false
        if (error.response?.statusCode == HttpStatusCode.NotFound.value) {
            _errorMessage.value = error.response.message
        }
    }

    override fun onNewChatAdded(chat: NewChatResponse) {
        val newConversation = DBConversation(
            otherUser = chat.other,
            chatReference = chat.chatReference
        )
        viewModelScope.launch(Dispatchers.IO) {
            _newConversation.send(newConversation)
            _waiting.postValue(false)
        }
    }

    override fun onError(response: IResponse.Failure<ParentResponse<String>>) {
        if (response.response?.statusCode == HttpStatusCode.Unauthorized.value) {
            _tokenExpired.value = true
        }
    }

    fun connectToChatService() {
        viewModelScope.launch(Dispatchers.IO) {
            conversationsRepository.connectToChatService()
        }
    }

    override fun onClose(code: Int, reason: String) {
        _isConnected.value = false
    }

    override fun onConnect() {
        Napier.d("socket connected")
        _isConnected.value = true
    }

    override fun onDisconnect(t: Throwable, response: Response?) {
        Napier.d("socket disconnected")
        _isConnected.value = false
    }

    override fun onError(error: ChatServiceErrorResponse<*>) {

    }

    fun popReceivedMessagesQueue() {
        if (receivedMessagesQueue.isNotEmpty()) {
            receivedMessagesQueue.remove()
            viewModelScope.launch(Dispatchers.Default) {
                receivedMessagesQueue.peek()?.let {
                    _conversations.postValue(
                        conversationsRepository.rebuildConversations(it)
                    )
                }
            }
        }
    }

    override fun onChatInviteReceived(message: Message) {
        val current = _pendingInvites.value ?: emptyList()
        if (current.none { it.chatReference == message.chatReference }) {
            _pendingInvites.value = current + message
        }
    }

    override fun onInviteAccepted(chatReference: String) {
        // Someone accepted OUR invite — refresh conversations
        viewModelScope.launch(Dispatchers.IO) {
            _conversations.postValue(conversationsRepository.getConversations().toMutableList())
        }
    }

    override fun onInviteDeclined(chatReference: String) {
        // Someone declined OUR invite — clean up locally and notify user
        viewModelScope.launch(Dispatchers.IO) {
            conversationsRepository.deleteConversation(chatReference)
            _conversations.postValue(conversationsRepository.getConversations().toMutableList())
        }
        _errorMessage.postValue("Your invitation was declined.")
    }

    override fun onReceiveRecipientMessageStatus(chatRef: String, messageStatus: MessageStatus) {
        when (messageStatus) {
            MessageStatus.TYPING -> {
                _isUserTyping.value = chatRef to true
            }
            else -> _isUserTyping.value = chatRef to false
        }
    }

    override fun onReceive(message: Message) {
        _isUserTyping.value = message.chatReference to false
        viewModelScope.launch(Dispatchers.Default) {
            if (receivedMessagesQueue.isEmpty()) {
                receivedMessagesQueue.add(message)
                _conversations.postValue(
                    conversationsRepository.rebuildConversations(message)
                )
            } else {
                receivedMessagesQueue.add(message)
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
        conversationsRepository.cancel()
    }
}

class ConversationsViewModelProvider(private val context: Context): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val chatApiService = ChatApiService(client)
        val repo = ConversationsRepository(
            AddNewChatUsecase(ConversationsService(client)),
            createConversationsUsecase = CreateConversationsUsecase(chatApiService),
            chatApiService = chatApiService,
            ConversationDatabase.get(context),
            ChatDatabase.get(context)
        ).apply {
            session.appWideChatService?.setListener(this)
        }
        return ConversationsViewModel(repo).apply {
            repo.setListener(this)
        } as T
    }
}