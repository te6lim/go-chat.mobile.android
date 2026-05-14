package com.simulatedtez.gochat.remote.api_services

import com.simulatedtez.gochat.config.IConfig
import com.simulatedtez.gochat.model.response.NewChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import com.simulatedtez.gochat.remote.api_interfaces.IConversationsApiService
import com.simulatedtez.gochat.remote.api_usecases.StartNewChatParams
import com.simulatedtez.gochat.remote.postWithBaseUrl
import com.simulatedtez.gochat.session.ISession
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class ConversationsApiService(
    private val client: HttpClient,
    private val session: ISession,
    private val config: IConfig
) : IConversationsApiService {

    override suspend fun addNewConversation(params: StartNewChatParams): IResponse<ParentResponse<NewChatResponse>> {
        return Response<ParentResponse<NewChatResponse>> {
            client.postWithBaseUrl(session, config, "/chatReference") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
                setBody(params.request)
            }
        }.invoke()
    }
}
