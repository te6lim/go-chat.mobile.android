package com.simulatedtez.gochat.model.enums

enum class PresenceStatus {
    ONLINE, AWAY, OFFLINE;

    companion object {
        fun getType(value: String?): PresenceStatus? = when (value) {
            ONLINE.name -> ONLINE
            AWAY.name -> AWAY
            OFFLINE.name -> OFFLINE
            else -> null
        }
    }
}
