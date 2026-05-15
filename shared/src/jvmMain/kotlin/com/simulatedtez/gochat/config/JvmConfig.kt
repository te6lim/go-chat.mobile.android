package com.simulatedtez.gochat.config

object JvmConfig : IConfig {
    override val authBaseUrl: String = System.getenv("GOCHAT_AUTH_URL") ?: "http://192.168.0.3:50051"
    override val chatBaseUrl: String = System.getenv("GOCHAT_CHAT_URL") ?: "http://192.168.0.3:50053"
    override val wsBaseUrl: String = System.getenv("GOCHAT_WS_URL") ?: "ws://192.168.0.3:50053"
}
