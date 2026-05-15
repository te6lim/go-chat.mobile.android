package com.simulatedtez.gochat.remote.api_usecases

import com.simulatedtez.gochat.model.response.GroupChatResponse
import com.simulatedtez.gochat.remote.IEndpointCaller
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.RemoteParams
import com.simulatedtez.gochat.remote.api_interfaces.IChatApiService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class CreateGroupChatUsecase(
    private val chatApiService: IChatApiService
) : IEndpointCaller<CreateGroupChatParams, ParentResponse<GroupChatResponse>, IResponse<ParentResponse<GroupChatResponse>>> {
    override suspend fun call(
        params: CreateGroupChatParams,
        handler: IResponseHandler<ParentResponse<GroupChatResponse>, IResponse<ParentResponse<GroupChatResponse>>>?
    ) {
        handler?.onResponse(chatApiService.createGroupChat(params))
    }
}

data class CreateGroupChatParams(override val request: Request) : RemoteParams(request = request) {
    @Serializable
    data class Request(
        @SerialName("name") val name: String,
        @SerialName("participants") val participants: List<String>
    )
}
