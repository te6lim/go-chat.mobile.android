package com.simulatedtez.gochat.remote

import com.simulatedtez.gochat.BuildConfig
import com.simulatedtez.gochat.model.response.LoginResponse
import com.simulatedtez.gochat.remote.api_usecases.LoginParams
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType


suspend fun HttpClient.doLogin(params: LoginParams): IResponse<ParentResponse<LoginResponse>> {
    return Response< ParentResponse<LoginResponse>> {
        client.post(BuildConfig.AUTH_BASE_URL + "/login") {
            contentType(ContentType.Application.Json)
            setBody(params.request)
        }
    }.invoke()
}