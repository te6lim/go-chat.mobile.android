package com.simulatedtez.gochat.remote

import com.simulatedtez.gochat.BuildConfig
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.model.response.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class LoginRequest(val username: String, val password: String)

private val tokenRefreshLock = Any()

private suspend fun refreshToken(httpClient: HttpClient): Boolean {
    val username = session.username
    val password = session.getPassword() ?: return false

    return try {
        val response = httpClient.post(BuildConfig.AUTH_BASE_URL + "/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = username, password = password))
        }
        if (response.status.isSuccess()) {
            val body: ParentResponse<LoginResponse> = response.body()
            body.data?.let {
                session.saveAccessToken(it.accessToken)
                true
            } ?: false
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

suspend fun HttpClient.postWithBaseUrl(endpoint: String, block: HttpRequestBuilder.() -> Unit): HttpResponse {
    val response = post(BuildConfig.CHAT_HISTORY_BASE_URL + endpoint) {
        contentType(ContentType.Application.Json)
        block()
    }
    if (response.status == HttpStatusCode.Unauthorized) {
        if (refreshToken(this)) {
            return post(BuildConfig.CHAT_HISTORY_BASE_URL + endpoint) {
                contentType(ContentType.Application.Json)
                block()
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            }
        }
    }
    return response
}

suspend fun HttpClient.getWithBaseUrl(endpoint: String, block: HttpRequestBuilder.() -> Unit): HttpResponse {
    val response = get(BuildConfig.CHAT_HISTORY_BASE_URL + endpoint) {
        block()
    }
    if (response.status == HttpStatusCode.Unauthorized) {
        if (refreshToken(this)) {
            return get(BuildConfig.CHAT_HISTORY_BASE_URL + endpoint) {
                block()
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            }
        }
    }
    return response
}

suspend fun HttpClient.deleteWithBaseUrl(endpoint: String, block: HttpRequestBuilder.() -> Unit): HttpResponse {
    val response = delete(BuildConfig.CHAT_HISTORY_BASE_URL + endpoint) {
        block()
    }
    if (response.status == HttpStatusCode.Unauthorized) {
        if (refreshToken(this)) {
            return delete(BuildConfig.CHAT_HISTORY_BASE_URL + endpoint) {
                block()
                header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            }
        }
    }
    return response
}
