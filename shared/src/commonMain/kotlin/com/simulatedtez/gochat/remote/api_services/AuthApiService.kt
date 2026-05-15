package com.simulatedtez.gochat.remote.api_services

import com.simulatedtez.gochat.config.IConfig
import com.simulatedtez.gochat.model.response.LoginResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import com.simulatedtez.gochat.remote.api_interfaces.IAuthApiService
import com.simulatedtez.gochat.remote.api_usecases.LoginParams
import com.simulatedtez.gochat.remote.api_usecases.SignupParams
import com.simulatedtez.gochat.remote.doLogin
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApiService(
    private val client: HttpClient,
    private val config: IConfig
) : IAuthApiService {

    override suspend fun login(params: LoginParams): IResponse<ParentResponse<LoginResponse>> {
        return client.doLogin(config, params)
    }

    override suspend fun signup(params: SignupParams): IResponse<ParentResponse<String>> {
        return Response<ParentResponse<String>> {
            client.post(config.authBaseUrl + "/register") {
                contentType(ContentType.Application.Json)
                setBody(params.request)
            }
        }.invoke()
    }
}
