package com.simulatedtez.gochat.remote.api_interfaces

import com.simulatedtez.gochat.model.response.NewChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.api_usecases.StartNewChatParams

interface IConversationsApiService {
    suspend fun addNewConversation(params: StartNewChatParams): IResponse<ParentResponse<NewChatResponse>>
}
