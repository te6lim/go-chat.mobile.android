package com.simulatedtez.gochat.remote

import com.simulatedtez.gochat.BuildConfig
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.remote.api_usecases.LoginParams
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Refresh 60 seconds before actual expiry to avoid using a token that's about to die
private const val EXPIRY_BUFFER_MS = 60_000L

private fun isTokenExpired(): Boolean {
    val expiry = session.tokenExpiryMs
    // No expiry saved but a token exists — legacy session or parse failure.
    // Refresh proactively so the next call goes out with a fresh token and
    // a known expiry, rather than risking a server-side 401.
    // No token at all means the user isn't logged in — nothing to refresh.
    if (expiry == 0L) return session.accessToken.isNotEmpty()
    return System.currentTimeMillis() >= expiry - EXPIRY_BUFFER_MS
}

private suspend fun refreshToken(httpClient: HttpClient): Boolean {
    val username = session.username
    val password = session.getPassword() ?: return false

    return try {
        val response = httpClient.doLogin(
            LoginParams(
                request = LoginParams.Request(
                    username, password
                )
            )
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
    if (isTokenExpired()) {
        refreshToken(this)
    }
    val response = post(BuildConfig.CHAT_HISTORY_BASE_URL + endpoint) {
        contentType(ContentType.Application.Json)
        block()
    }
    if (response.status == HttpStatusCode.Unauthorized) {
        if (refreshToken(this)) {
            return post(BuildConfig.CHAT_HISTORY_BASE_URL + endpoint) {
                contentType(ContentType.Application.Json)
                block()
            }
        }
    }
    return response
}

suspend fun HttpClient.getWithBaseUrl(endpoint: String, block: HttpRequestBuilder.() -> Unit): HttpResponse {
    if (isTokenExpired()) refreshToken(this)
    val response = get(BuildConfig.CHAT_HISTORY_BASE_URL + endpoint) {
        block()
    }
    if (response.status == HttpStatusCode.Unauthorized) {
        if (refreshToken(this)) {
            return get(BuildConfig.CHAT_HISTORY_BASE_URL + endpoint) {
                block()
            }
        }
    }
    return response
}

suspend fun HttpClient.deleteWithBaseUrl(endpoint: String, block: HttpRequestBuilder.() -> Unit): HttpResponse {
    if (isTokenExpired()) refreshToken(this)
    val response = delete(BuildConfig.CHAT_HISTORY_BASE_URL + endpoint) {
        block()
    }
    if (response.status == HttpStatusCode.Unauthorized) {
        if (refreshToken(this)) {
            return delete(BuildConfig.CHAT_HISTORY_BASE_URL + endpoint) {
                block()
            }
        }
    }
    return response
}
