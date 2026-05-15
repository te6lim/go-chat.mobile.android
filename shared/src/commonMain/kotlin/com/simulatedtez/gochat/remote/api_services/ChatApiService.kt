package com.simulatedtez.gochat.remote.api_services

import com.simulatedtez.gochat.config.IConfig
import com.simulatedtez.gochat.model.response.ConversationSummary
import com.simulatedtez.gochat.model.response.GroupChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import com.simulatedtez.gochat.remote.api_interfaces.IChatApiService
import com.simulatedtez.gochat.remote.api_usecases.CreateChatRoomParams
import com.simulatedtez.gochat.remote.api_usecases.CreateConversationsParams
import com.simulatedtez.gochat.remote.api_usecases.CreateGroupChatParams
import com.simulatedtez.gochat.remote.deleteWithBaseUrl
import com.simulatedtez.gochat.remote.getWithBaseUrl
import com.simulatedtez.gochat.remote.postWithBaseUrl
import com.simulatedtez.gochat.session.ISession
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders

class ChatApiService(
    private val client: HttpClient,
    private val session: ISession,
    private val config: IConfig
) : IChatApiService {

    override suspend fun createChatRoom(params: CreateChatRoomParams): IResponse<ParentResponse<String>> {
        return Response<String> {
            client.postWithBaseUrl(session, config, "/chat") {
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                setBody(params.request)
            }
        }.invoke()
    }

    override suspend fun createConversations(params: CreateConversationsParams): IResponse<ParentResponse<String>> {
        return Response<String> {
            client.postWithBaseUrl(session, config, "/conversations/${params.request.username}") {
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            }
        }.invoke()
    }

    override suspend fun deleteConversation(chatReference: String): IResponse<ParentResponse<String>> {
        return Response<String> {
            client.deleteWithBaseUrl(session, config, "/conversations/$chatReference") {
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            }
        }.invoke()
    }

    override suspend fun fetchUserConversations(username: String): IResponse<ParentResponse<List<ConversationSummary>>> {
        return Response<List<ConversationSummary>> {
            client.getWithBaseUrl(session, config, "/user/$username/conversations") {
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            }
        }.invoke()
    }

    override suspend fun createGroupChat(params: CreateGroupChatParams): IResponse<ParentResponse<GroupChatResponse>> {
        return Response<GroupChatResponse> {
            client.postWithBaseUrl(session, config, "/create-groupchat") {
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                setBody(params.request)
            }
        }.invoke()
    }
}
