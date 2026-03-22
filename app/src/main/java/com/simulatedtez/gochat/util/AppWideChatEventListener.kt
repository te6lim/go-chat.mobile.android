package com.simulatedtez.gochat.util

import ChatServiceErrorResponse
import android.content.Context
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.database.ChatDatabase
import com.simulatedtez.gochat.database.IChatStorage
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.remote.api_services.ChatApiService
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.toDBMessage
import com.simulatedtez.gochat.remote.api_usecases.CreateConversationsParams
import com.simulatedtez.gochat.remote.api_usecases.CreateConversationsUsecase
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.client
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import listeners.ChatEngineEventListener
import okhttp3.Response

open class AppWideChatEventListener(
    val createConversationsUsecase: CreateConversationsUsecase,
    val chatDb: IChatStorage
): ChatEngineEventListener<Message> {

    companion object {

        var instance: AppWideChatEventListener? = null

        fun get(context: Context): AppWideChatEventListener {
            return instance ?: AppWideChatEventListener(
                createConversationsUsecase = CreateConversationsUsecase(ChatApiService(client)),
                ChatDatabase.get(context)
            ).apply {
                instance = this
            }
        }
    }

    open val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val userPresenceHelper = UserPresenceHelper(
        session.appWideChatService,PresenceStatus.AWAY, null
    )

    open suspend fun connectToChatService() {
        if (session.appWideChatService?.socketIsConnected == false) {
            createNewConversations {
                session.appWideChatService?.connect()
            }
        }
    }

    open suspend fun createNewConversations(onSuccess: (() -> Unit)) {
        val params = CreateConversationsParams(
            request = CreateConversationsParams.Request(
                username = session.username
            )
        )
        createConversationsUsecase.call(
            params = params, object: IResponseHandler<ParentResponse<String>, IResponse<ParentResponse<String>>> {
                override fun onResponse(response: IResponse<ParentResponse<String>>) {
                    when(response) {
                        is IResponse.Success -> {
                            onSuccess()
                        }
                        is IResponse.Failure -> {
                            Napier.d(response.response?.message ?: "unknown")
                            context.launch(Dispatchers.Main) {
                                // handle error
                            }
                        }
                        else -> {

                        }
                    }
                }
            }
        )
    }

    override fun onClose(code: Int, reason: String) {

    }

    override fun onConnect() {
        userPresenceHelper.postNewUserPresence(PresenceStatus.AWAY)
    }

    override fun onDisconnect(t: Throwable, response: Response?) {

    }

    override fun onError(response: ChatServiceErrorResponse<Message>) {

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
            return
        }

        context.launch(Dispatchers.IO) {
            chatDb.store(message)
        }
    }

    override fun onSent(message: Message) {
        if (message.presenceStatus.isNullOrEmpty()) {
            val dbMessage = message.toDBMessage()
            context.launch(Dispatchers.IO) {
                chatDb.store(message)
                chatDb.setAsSent((dbMessage.id to dbMessage.chatReference))
            }
        } else {
            userPresenceHelper.onPresenceSent(message.id)
        }
    }

    fun cancel() {
        context.cancel()
    }
}