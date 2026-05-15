package com.simulatedtez.gochat.remote

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParentResponse<R>(
    @SerialName("data") val data: R?,
    @SerialName("message") val message: String,
    @SerialName("error") val error: String,
    @SerialName("statusCode") val statusCode: Int,
    @SerialName("isSuccessful") val isSuccessful: Boolean
)

class Response<R>(val call: suspend () -> HttpResponse) : IResponse<R> {
    suspend inline operator fun <reified R> invoke(): IResponse<R> = try {
        val response = call()
        val data: R = response.body()
        if (response.status.isSuccess()) IResponse.Success(data = data)
        else IResponse.Failure(reason = response.status.description, response = data)
    } catch (e: Exception) {
        IResponse.Failure(exception = e, reason = e.message ?: "unknown reason")
    }
}

sealed interface IResponse<R> {
    data class Success<R>(val data: R) : IResponse<R>
    data class Failure<R>(
        val exception: Exception? = null,
        val reason: String,
        val response: R? = null
    ) : IResponse<R>
}
