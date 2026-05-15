package com.simulatedtez.gochat.remote.api_usecases

import com.simulatedtez.gochat.remote.IEndpointCaller
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.RemoteParams
import com.simulatedtez.gochat.remote.api_interfaces.IChatApiService

class CreateConversationsUsecase(
    private val chatApiService: IChatApiService
) : IEndpointCaller<CreateConversationsParams, ParentResponse<String>, IResponse<ParentResponse<String>>> {
    override suspend fun call(
        params: CreateConversationsParams,
        handler: IResponseHandler<ParentResponse<String>, IResponse<ParentResponse<String>>>?
    ) {
        handler?.onResponse(chatApiService.createConversations(params))
    }
}

data class CreateConversationsParams(override val request: Request) : RemoteParams(request = request) {
    data class Request(val username: String)
}
