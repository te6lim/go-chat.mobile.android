package com.simulatedtez.gochat.remote.api_usecases

import com.simulatedtez.gochat.remote.IEndpointCaller
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.RemoteParams
import com.simulatedtez.gochat.remote.api_interfaces.IChatApiService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class CreateChatRoomUsecase(
    private val chatApiService: IChatApiService
) : IEndpointCaller<CreateChatRoomParams, ParentResponse<String>, IResponse<ParentResponse<String>>> {
    override suspend fun call(
        params: CreateChatRoomParams,
        handler: IResponseHandler<ParentResponse<String>, IResponse<ParentResponse<String>>>?
    ) {
        handler?.onResponse(chatApiService.createChatRoom(params))
    }
}

data class CreateChatRoomParams(override val request: Request) : RemoteParams(request = request) {
    @Serializable
    data class Request(
        @SerialName("user") val user: String,
        @SerialName("other") val other: String,
        @SerialName("chatReference") val chatReference: String
    )
}
