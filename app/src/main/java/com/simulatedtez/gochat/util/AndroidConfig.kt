package com.simulatedtez.gochat.util

import com.simulatedtez.gochat.BuildConfig
import com.simulatedtez.gochat.config.IConfig

val androidConfig: IConfig = object : IConfig {
    override val authBaseUrl: String get() = BuildConfig.AUTH_BASE_URL
    override val chatBaseUrl: String get() = BuildConfig.CHAT_HISTORY_BASE_URL
    override val wsBaseUrl: String get() = BuildConfig.WEBSOCKET_BASE_URL
}
