package com.simulatedtez.gochat.remote.api_usecases

import com.simulatedtez.gochat.remote.IEndpointCaller
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.RemoteParams
import com.simulatedtez.gochat.remote.api_interfaces.IAuthApiService
import kotlinx.serialization.Serializable

class SignupUsecase(
    private val authApiService: IAuthApiService
) : IEndpointCaller<SignupParams, ParentResponse<String>, IResponse<ParentResponse<String>>> {
    override suspend fun call(
        params: SignupParams,
        handler: IResponseHandler<ParentResponse<String>, IResponse<ParentResponse<String>>>?
    ) {
        handler?.onResponse(authApiService.signup(params))
    }
}

data class SignupParams(override val request: Request) : RemoteParams(null, request) {
    @Serializable
    data class Request(val username: String, val password: String)
}
