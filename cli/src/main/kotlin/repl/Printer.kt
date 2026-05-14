package repl

import com.simulatedtez.gochat.model.DBConversation
import com.simulatedtez.gochat.model.DBMessage
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.enums.PresenceStatus

object Printer {

    fun prompt() { print("> ") }

    fun incomingMessage(message: Message) {
        println("\r[${message.sender}] ${message.message}")
        prompt()
    }

    fun messageSent(message: Message) {
        println("\r[you -> ${message.receiver}] ${message.message}")
        prompt()
    }

    fun messageStatus(chatRef: String, status: MessageStatus) {
        val label = when (status) {
            MessageStatus.DELIVERED -> "delivered"
            MessageStatus.SEEN -> "seen"
            MessageStatus.TYPING -> "$chatRef is typing..."
            MessageStatus.NOT_TYPING -> ""
            else -> status.name.lowercase()
        }
        if (label.isNotEmpty()) {
            println("\r[$label]")
            prompt()
        }
    }

    fun presenceStatus(status: PresenceStatus) {
        println("\r[presence: ${status.name.lowercase()}]")
        prompt()
    }

    fun conversations(list: List<DBConversation>) {
        if (list.isEmpty()) { println("No conversations yet."); return }
        println("--- conversations ---")
        list.forEachIndexed { i, c ->
            val name = if (c.chatType == "group") c.chatName else c.otherUser
            val last = if (c.lastMessage.isNotEmpty()) "  \"${c.lastMessage}\"" else ""
            println("  ${i + 1}. $name$last")
        }
        println("---------------------")
    }

    fun history(messages: List<DBMessage>, currentUser: String) {
        if (messages.isEmpty()) { println("No messages in memory for this chat."); return }
        println("--- history (this session) ---")
        messages.forEach { m ->
            val who = if (m.sender == currentUser) "you" else m.sender
            val status = when {
                m.seenTimestamp != null -> " [seen]"
                m.deliveredTimestamp != null -> " [delivered]"
                m.isSent == true -> " [sent]"
                else -> ""
            }
            println("  [$who]$status ${m.message}")
        }
        println("------------------------------")
    }

    fun connected(to: String) = println("Connected to chat with $to. Type messages or a command.")
    fun disconnected() = println("[disconnected]")
    fun error(msg: String) = println("[error] $msg")
    fun info(msg: String) = println(msg)

    fun help() {
        println("""
            |commands:
            |  signup <username> <password>
            |  login <username> <password>
            |  conversations
            |  chat <username>
            |  send <message text>
            |  history
            |  exit / quit
        """.trimMargin())
    }
}
