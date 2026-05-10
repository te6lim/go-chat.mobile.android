package com.simulatedtez.gochat.model.enums

enum class MessageStatus {
    TYPING, NOT_TYPING, SENDING, SENT, NOT_SENT, DELIVERED, SEEN, FAILED,
    CHAT_INVITE, ACCEPT_INVITE, DECLINE_INVITE, INVITE_ACCEPTED, INVITE_DECLINED,
    INVITE_PENDING, INVITE_REVOKED, GROUP_INVITE, GROUP_REMOVED;

    companion object {
        fun getType(value: String?): MessageStatus? {
            return when(value) {
                TYPING.name -> TYPING
                NOT_TYPING.name -> NOT_TYPING
                SENDING.name -> SENDING
                SENT.name -> SENT
                NOT_SENT.name -> NOT_SENT
                DELIVERED.name -> DELIVERED
                SEEN.name -> SEEN
                FAILED.name -> FAILED
                CHAT_INVITE.name -> CHAT_INVITE
                ACCEPT_INVITE.name -> ACCEPT_INVITE
                DECLINE_INVITE.name -> DECLINE_INVITE
                INVITE_ACCEPTED.name -> INVITE_ACCEPTED
                INVITE_DECLINED.name -> INVITE_DECLINED
                INVITE_PENDING.name -> INVITE_PENDING
                INVITE_REVOKED.name -> INVITE_REVOKED
                GROUP_INVITE.name -> GROUP_INVITE
                GROUP_REMOVED.name -> GROUP_REMOVED
                else -> null
            }
        }
    }
}