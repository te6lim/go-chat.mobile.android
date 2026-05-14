package com.simulatedtez.gochat.remote

interface IResponseHandler<D, R : IResponse<D>> {
    fun onResponse(response: R)
}
