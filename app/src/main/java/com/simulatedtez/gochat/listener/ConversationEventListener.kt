package com.simulatedtez.gochat.listener

import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.response.NewChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse

interface ConversationEventListener: SocketConnection, MessageReceiver, MessageSender {
    fun onAddNewChatFailed(error: IResponse.Failure<ParentResponse<NewChatResponse>>)
    fun onNewChatAdded(chat: NewChatResponse)
    fun onError(response: IResponse.Failure<ParentResponse<String>>)
    fun onChatInviteReceived(message: Message)
    fun onInviteAccepted(chatReference: String)
    fun onInviteDeclined(chatReference: String)
    fun onInviteRevoked(chatReference: String)
    fun onGroupInviteReceived(message: Message)
    fun onGroupRemoved(chatReference: String)
}