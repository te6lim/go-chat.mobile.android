package com.simulatedtez.gochat.remote.api_services

import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.remote.api_usecases.CreateChatRoomParams
import com.simulatedtez.gochat.remote.api_usecases.CreateConversationsParams
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import com.simulatedtez.gochat.remote.api_interfaces.IChatApiService
import com.simulatedtez.gochat.remote.deleteWithBaseUrl
import com.simulatedtez.gochat.remote.postWithBaseUrl
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders

class ChatApiService(private val client: HttpClient): IChatApiService {

    override suspend fun createChatRoom(params: CreateChatRoomParams): IResponse<ParentResponse<String>> {
        return Response<String> {
            client.postWithBaseUrl("/chat") {
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                setBody(params.request)
            }
        }.invoke()
    }

    override suspend fun createConversations(params: CreateConversationsParams): IResponse<ParentResponse<String>> {
        return Response<String> {
            client.postWithBaseUrl("/conversations/${params.request.username}") {
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            }
        }.invoke()
    }

    override suspend fun deleteConversation(chatReference: String): IResponse<ParentResponse<String>> {
        return Response<String> {
            client.deleteWithBaseUrl("/conversations/$chatReference") {
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            }
        }.invoke()
    }
}