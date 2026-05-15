package com.simulatedtez.gochat.remote.api_usecases

import com.simulatedtez.gochat.model.response.LoginResponse
import com.simulatedtez.gochat.remote.IEndpointCaller
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.RemoteParams
import com.simulatedtez.gochat.remote.api_interfaces.IAuthApiService
import kotlinx.serialization.Serializable

class LoginUsecase(
    private val authApiService: IAuthApiService
) : IEndpointCaller<LoginParams, ParentResponse<LoginResponse>, IResponse<ParentResponse<LoginResponse>>> {
    override suspend fun call(
        params: LoginParams,
        handler: IResponseHandler<ParentResponse<LoginResponse>, IResponse<ParentResponse<LoginResponse>>>?
    ) {
        handler?.onResponse(authApiService.login(params))
    }
}

data class LoginParams(override val request: Request) : RemoteParams(null, request) {
    @Serializable
    data class Request(val username: String, val password: String)
}
