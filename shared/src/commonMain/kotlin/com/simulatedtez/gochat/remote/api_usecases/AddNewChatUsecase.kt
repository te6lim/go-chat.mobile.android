package com.simulatedtez.gochat.remote.api_usecases

import com.simulatedtez.gochat.model.response.NewChatResponse
import com.simulatedtez.gochat.remote.IEndpointCaller
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.RemoteParams
import com.simulatedtez.gochat.remote.api_interfaces.IConversationsApiService
import kotlinx.serialization.Serializable

class AddNewChatUsecase(
    private val conversationsApiService: IConversationsApiService
) : IEndpointCaller<StartNewChatParams, ParentResponse<NewChatResponse>, IResponse<ParentResponse<NewChatResponse>>> {
    override suspend fun call(
        params: StartNewChatParams,
        handler: IResponseHandler<ParentResponse<NewChatResponse>, IResponse<ParentResponse<NewChatResponse>>>?
    ) {
        handler?.onResponse(conversationsApiService.addNewConversation(params))
    }
}

data class StartNewChatParams(override val request: Request) : RemoteParams(request = request) {
    @Serializable
    data class Request(val user: String, val other: String)
}
