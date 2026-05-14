package com.simulatedtez.gochat.remote.api_interfaces

import com.simulatedtez.gochat.model.response.LoginResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.api_usecases.LoginParams
import com.simulatedtez.gochat.remote.api_usecases.SignupParams

interface IAuthApiService {
    suspend fun login(params: LoginParams): IResponse<ParentResponse<LoginResponse>>
    suspend fun signup(params: SignupParams): IResponse<ParentResponse<String>>
}
