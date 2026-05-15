package com.simulatedtez.gochat.remote

import com.simulatedtez.gochat.config.IConfig
import com.simulatedtez.gochat.model.response.LoginResponse
import com.simulatedtez.gochat.remote.api_usecases.LoginParams
import com.simulatedtez.gochat.session.ISession
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.datetime.Clock

private const val EXPIRY_BUFFER_MS = 60_000L

private fun isTokenExpired(session: ISession): Boolean {
    val expiry = session.tokenExpiryMs
    if (expiry == 0L) return session.accessToken.isNotEmpty()
    return Clock.System.now().toEpochMilliseconds() >= expiry - EXPIRY_BUFFER_MS
}

private suspend fun refreshToken(client: HttpClient, config: IConfig, session: ISession): Boolean {
    val username = session.username
    val password = session.getPassword() ?: return false
    return try {
        val response = client.doLogin(
            config,
            LoginParams(request = LoginParams.Request(username, password))
        )
        when (response) {
            is IResponse.Success -> {
                response.data.data?.let {
                    session.saveTokenDetails(it.accessToken, it.expiryTime)
                    true
                } ?: false
            }
            else -> false
        }
    } catch (_: Exception) {
        false
    }
}

suspend fun HttpClient.postWithBaseUrl(
    session: ISession,
    config: IConfig,
    endpoint: String,
    block: HttpRequestBuilder.() -> Unit
): HttpResponse {
    if (isTokenExpired(session)) refreshToken(this, config, session)
    val response = post(config.chatBaseUrl + endpoint) {
        contentType(ContentType.Application.Json)
        block()
    }
    if (response.status == HttpStatusCode.Unauthorized) {
        if (refreshToken(this, config, session)) {
            return post(config.chatBaseUrl + endpoint) {
                contentType(ContentType.Application.Json)
                block()
            }
        }
    }
    return response
}

suspend fun HttpClient.getWithBaseUrl(
    session: ISession,
    config: IConfig,
    endpoint: String,
    block: HttpRequestBuilder.() -> Unit
): HttpResponse {
    if (isTokenExpired(session)) refreshToken(this, config, session)
    val response = get(config.chatBaseUrl + endpoint) { block() }
    if (response.status == HttpStatusCode.Unauthorized) {
        if (refreshToken(this, config, session)) {
            return get(config.chatBaseUrl + endpoint) { block() }
        }
    }
    return response
}

suspend fun HttpClient.deleteWithBaseUrl(
    session: ISession,
    config: IConfig,
    endpoint: String,
    block: HttpRequestBuilder.() -> Unit
): HttpResponse {
    if (isTokenExpired(session)) refreshToken(this, config, session)
    val response = delete(config.chatBaseUrl + endpoint) { block() }
    if (response.status == HttpStatusCode.Unauthorized) {
        if (refreshToken(this, config, session)) {
            return delete(config.chatBaseUrl + endpoint) { block() }
        }
    }
    return response
}

suspend fun HttpClient.doLogin(
    config: IConfig,
    params: LoginParams
): IResponse<ParentResponse<LoginResponse>> {
    return Response<ParentResponse<LoginResponse>> {
        post(config.authBaseUrl + "/login") {
            contentType(ContentType.Application.Json)
            setBody(params.request)
        }
    }.invoke()
}
