package com.simulatedtez.gochat.repository

import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.remote.api_usecases.LoginParams
import com.simulatedtez.gochat.remote.api_usecases.LoginUsecase
import com.simulatedtez.gochat.remote.api_usecases.SignupParams
import com.simulatedtez.gochat.remote.api_usecases.SignupUsecase
import com.simulatedtez.gochat.model.response.LoginResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.IResponseHandler
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SignupRepository(
    private val signupUsecase: SignupUsecase,
    private val loginUsecase: LoginUsecase,
) {
    private val context = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var signupEventListener: SignupEventListener? = null

    fun setEventListener(listener: SignupEventListener) {
        signupEventListener = listener
    }

    suspend fun signUp(username: String, password: String) {
        val signupParams = SignupParams(
            request = SignupParams.Request(
                username = username,
                password = password
            )
        )
        signupUsecase.call(
            signupParams, object: IResponseHandler<ParentResponse<String>,
                    IResponse<ParentResponse<String>>> {
                override fun onResponse(response: IResponse<ParentResponse<String>>) {
                    when (response) {
                        is IResponse.Success -> {
                            response.data.data?.let {
                                // Auto-login after successful signup
                                context.launch(Dispatchers.IO) {
                                    loginAfterSignup(username, password)
                                }
                            }
                        }
                        is IResponse.Failure -> {
                            context.launch(Dispatchers.Main) {
                                signupEventListener?.onSignUpFailed(response)
                            }
                        }

                        is Response -> {}
                    }
                }
            }
        )
    }

    private suspend fun loginAfterSignup(username: String, password: String) {
        val loginParams = LoginParams(
            request = LoginParams.Request(
                username = username,
                password = password
            )
        )
        loginUsecase.call(
            loginParams, object: IResponseHandler<ParentResponse<LoginResponse>,
                    IResponse<ParentResponse<LoginResponse>>> {
                override fun onResponse(response: IResponse<ParentResponse<LoginResponse>>) {
                    when (response) {
                        is IResponse.Success -> {
                            response.data.data?.let {
                                session.saveTokenDetails(it.accessToken, it.expiryTime)
                                session.saveUsername(username)
                                session.savePassword(password)

                                context.launch(Dispatchers.Main) {
                                    signupEventListener?.onSignUpAndLoginSuccess()
                                }
                            }
                        }
                        is IResponse.Failure -> {
                            // Signup succeeded but login failed — fall back to login screen
                            context.launch(Dispatchers.Main) {
                                signupEventListener?.onSignUp()
                            }
                        }

                        is Response -> {}
                    }
                }
            }
        )
    }

    fun cancel() {
        context.cancel()
    }
}

interface SignupEventListener {
    fun onSignUp()
    fun onSignUpAndLoginSuccess()
    fun onSignUpFailed(errorResponse: IResponse.Failure<ParentResponse<String>>)
}
