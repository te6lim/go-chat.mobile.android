package com.simulatedtez.gochat.remote

interface IEndpointCaller<P : RemoteParams, D : Any, R : IResponse<D>> {
    suspend fun call(params: P, handler: IResponseHandler<D, R>?)
}

abstract class RemoteParams(
    open val headers: Any? = null,
    open val request: Any
)
