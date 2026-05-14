package com.simulatedtez.gochat.repository

import ChatServiceErrorResponse
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.model.toDBMessage
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.api_usecases.CreateConversationsParams
import com.simulatedtez.gochat.remote.api_usecases.CreateConversationsUsecase
import com.simulatedtez.gochat.session.ISession
import com.simulatedtez.gochat.storage.IChatStorage
import com.simulatedtez.gochat.util.UserPresenceHelper
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import listeners.ChatEngineEventListener
import okhttp3.Response

open class AppWideChatEventListener(
    val createConversationsUsecase: CreateConversationsUsecase,
    val chatDb: IChatStorage,
    protected val session: ISession
) : ChatEngineEventListener<Message> {

    open val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val userPresenceHelper = UserPresenceHelper(
        chatEngine = session.appWideChatService,
        statusToBeSent = PresenceStatus.AWAY,
        chatInfo = null,
        session = session
    )

    open suspend fun connectToChatService() {
        if (session.appWideChatService?.socketIsConnected == false) {
            session.appWideChatService?.resetReconnectionDelay()
            createNewConversations { session.appWideChatService?.connect() }
        }
    }

    open suspend fun createNewConversations(onSuccess: () -> Unit) {
        createConversationsUsecase.call(
            CreateConversationsParams(CreateConversationsParams.Request(session.username)),
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

    override fun onClose(code: Int, reason: String) {}

    override fun onConnect() {
        userPresenceHelper.postNewUserPresence(PresenceStatus.AWAY)
    }

    override fun onDisconnect(t: Throwable, response: Response?) {}

    override fun onError(response: ChatServiceErrorResponse<Message>) {}

    override fun onReceive(message: Message) {
        PresenceStatus.getType(message.presenceStatus)?.let {
            context.launch {
                userPresenceHelper.handlePresenceMessage(it, message.id, message.chatReference) {}
            }
            return
        }
        MessageStatus.getType(message.messageStatus)?.let { return }
        context.launch { chatDb.store(message) }
    }

    override fun onSent(message: Message) {
        if (message.presenceStatus.isNullOrEmpty()) {
            val dbMessage = message.toDBMessage()
            context.launch {
                chatDb.store(message)
                chatDb.setAsSent(dbMessage.id to dbMessage.chatReference)
            }
        } else {
            userPresenceHelper.onPresenceSent(message.id)
        }
    }
}
