package repl

import ChatServiceErrorResponse
import com.simulatedtez.gochat.config.JvmConfig
import com.simulatedtez.gochat.listener.ChatEventListener
import com.simulatedtez.gochat.listener.ConversationEventListener
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.model.response.LoginResponse
import com.simulatedtez.gochat.model.response.NewChatResponse
import com.simulatedtez.gochat.remote.IResponse
import com.simulatedtez.gochat.remote.ParentResponse
import com.simulatedtez.gochat.remote.api_services.AuthApiService
import com.simulatedtez.gochat.remote.api_services.ChatApiService
import com.simulatedtez.gochat.remote.api_services.ConversationsApiService
import com.simulatedtez.gochat.remote.api_usecases.AddNewChatUsecase
import com.simulatedtez.gochat.remote.api_usecases.CreateChatRoomUsecase
import com.simulatedtez.gochat.remote.api_usecases.CreateConversationsUsecase
import com.simulatedtez.gochat.remote.api_usecases.LoginUsecase
import com.simulatedtez.gochat.remote.api_usecases.SignupUsecase
import com.simulatedtez.gochat.repository.ChatRepository
import com.simulatedtez.gochat.repository.ConversationsRepository
import com.simulatedtez.gochat.repository.LoginEventListener
import com.simulatedtez.gochat.repository.LoginRepository
import com.simulatedtez.gochat.repository.SignupEventListener
import com.simulatedtez.gochat.repository.SignupRepository
import com.simulatedtez.gochat.session.JvmSession
import com.simulatedtez.gochat.storage.InMemoryChatStorage
import com.simulatedtez.gochat.storage.InMemoryConversationStore
import com.simulatedtez.gochat.util.newAppWideChatService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

private val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

class CommandHandler(private val scope: CoroutineScope) {

    private val session = JvmSession
    private val config = JvmConfig

    private val chatDb = InMemoryChatStorage(session)
    private val conversationStore = InMemoryConversationStore()

    private val authApiService = AuthApiService(httpClient, config)
    private val chatApiService = ChatApiService(httpClient, session, config)
    private val conversationsApiService = ConversationsApiService(httpClient, session, config)

    private val loginRepo = LoginRepository(LoginUsecase(authApiService), session)
    private val signupRepo = SignupRepository(SignupUsecase(authApiService), LoginUsecase(authApiService), session)
    private val conversationsRepo = ConversationsRepository(
        addNewChatUsecase = AddNewChatUsecase(conversationsApiService),
        createConversationsUsecase = CreateConversationsUsecase(chatApiService),
        chatApiService = chatApiService,
        conversationStore = conversationStore,
        chatDb = chatDb,
        session = session
    )

    private var activeChatRepo: ChatRepository? = null
    private var activeChatInfo: ChatInfo? = null

    init {
        wireConversationEvents()
    }

    suspend fun handle(input: String) {
        val parts = input.trim().split(" ", limit = 2)
        val cmd = parts[0].lowercase()
        val args = parts.getOrElse(1) { "" }

        when (cmd) {
            "signup" -> handleSignup(args)
            "login" -> handleLogin(args)
            "conversations", "list" -> handleConversations()
            "chat" -> handleChat(args.trim())
            "send" -> handleSend(args)
            "history" -> handleHistory()
            "help" -> Printer.help()
            "exit", "quit" -> {
                Printer.info("Disconnecting...")
                activeChatRepo?.cancel()
                httpClient.close()
            }
            else -> Printer.error("Unknown command. Type 'help' for a list of commands.")
        }
    }

    private suspend fun handleSignup(args: String) {
        val (username, password) = parseTwoArgs(args) ?: run {
            Printer.error("Usage: signup <username> <password>"); return
        }
        Printer.info("Signing up as $username...")
        suspendCancellableCoroutine { cont ->
            signupRepo.setEventListener(object : SignupEventListener {
                override fun onSignUpAndLoginSuccess() {
                    Printer.info("Signed up and logged in as ${session.username}.")
                    setupAppWideService()
                    cont.resume(Unit)
                }
                override fun onSignUp() {
                    Printer.info("Signed up. Please log in.")
                    cont.resume(Unit)
                }
                override fun onSignUpFailed(errorResponse: IResponse.Failure<ParentResponse<String>>) {
                    Printer.error("Signup failed: ${errorResponse.reason}")
                    cont.resume(Unit)
                }
            })
            scope.launch { signupRepo.signUp(username, password) }
        }
    }

    private suspend fun handleLogin(args: String) {
        val (username, password) = parseTwoArgs(args) ?: run {
            Printer.error("Usage: login <username> <password>"); return
        }
        Printer.info("Logging in as $username...")
        suspendCancellableCoroutine { cont ->
            loginRepo.setEventListener(object : LoginEventListener {
                override fun onLogin(loginInfo: LoginResponse) {
                    Printer.info("Logged in as ${session.username}.")
                    setupAppWideService()
                    cont.resume(Unit)
                }
                override fun onLoginFailed(errorResponse: IResponse.Failure<ParentResponse<LoginResponse>>) {
                    Printer.error("Login failed: ${errorResponse.reason}")
                    cont.resume(Unit)
                }
            })
            scope.launch { loginRepo.login(username, password) }
        }
    }

    private suspend fun handleConversations() {
        if (session.username.isEmpty()) { Printer.error("Not logged in."); return }
        val list = conversationsRepo.getConversations()
        Printer.conversations(list)
    }

    private suspend fun handleChat(otherUser: String) {
        if (otherUser.isEmpty()) { Printer.error("Usage: chat <username>"); return }
        if (session.username.isEmpty()) { Printer.error("Not logged in."); return }

        Printer.info("Looking up chat with $otherUser...")
        val chatRef = conversationsRepo.getOrCreateChatReference(otherUser) ?: run {
            Printer.error("Could not start chat with $otherUser. Check the username and try again.")
            return
        }

        activeChatRepo?.cancel()

        val chatInfo = ChatInfo(
            username = session.username,
            recipientsUsernames = listOf(otherUser),
            chatReference = chatRef
        )
        activeChatInfo = chatInfo

        val repo = ChatRepository(
            chatInfo = chatInfo,
            createChatRoomUsecase = CreateChatRoomUsecase(chatApiService),
            chatDb = chatDb,
            conversationStore = conversationStore,
            session = session,
            config = config
        )
        activeChatRepo = repo
        wireChatEvents(repo, otherUser)
        repo.connectAndSendPendingMessages()
        Printer.connected(otherUser)
    }

    private fun handleSend(text: String) {
        if (text.isEmpty()) { Printer.error("Usage: send <message text>"); return }
        val repo = activeChatRepo ?: run { Printer.error("No active chat. Use 'chat <username>' first."); return }
        if (!repo.isChatServiceConnected()) { Printer.error("Not connected yet, please wait."); return }
        val message = repo.buildUnsentMessage(text)
        repo.sendMessage(message)
    }

    private fun handleHistory() {
        val chatInfo = activeChatInfo ?: run { Printer.error("No active chat."); return }
        val messages = chatDb.getAllMessages(chatInfo.chatReference)
        Printer.history(messages, session.username)
    }

    private fun setupAppWideService() {
        if (session.appWideChatService == null) {
            session.appWideChatService = newAppWideChatService(
                session.username,
                conversationsRepo,
                config
            )
        }
        scope.launch { conversationsRepo.connectToChatService() }
    }

    private fun wireChatEvents(repo: ChatRepository, otherUser: String) {
        val renderedIds = mutableSetOf<String>()
        repo.setChatEventListener(object : ChatEventListener {
            override fun onReceive(message: Message) = Printer.incomingMessage(message)
            override fun onMessageSent(message: Message) {
                if (renderedIds.add(message.id)) Printer.messageSent(message)
            }
            override fun onReceiveRecipientMessageStatus(chatRef: String, messageStatus: MessageStatus) =
                Printer.messageStatus(chatRef, messageStatus)
            override fun onReceiveRecipientActivityStatusMessage(presenceStatus: PresenceStatus) =
                Printer.presenceStatus(presenceStatus)
            override fun onConnect() = Printer.info("[connected]")
            override fun onClose(code: Int, reason: String) = Printer.info("[closed: $reason]")
            override fun onDisconnect(t: Throwable, response: okhttp3.Response?) = Printer.disconnected()
            override fun onError(error: ChatServiceErrorResponse<Message>) = Printer.error(error.reason ?: "unknown error")
        })
    }

    private fun wireConversationEvents() {
        conversationsRepo.setListener(object : ConversationEventListener {
            override fun onNewChatAdded(chat: NewChatResponse) = Printer.info("New chat started with ${chat.other}.")
            override fun onChatInviteReceived(message: Message) = Printer.info("Chat invite from ${message.sender}. Use 'accept ${message.chatReference}' or 'decline'.")
            override fun onInviteAccepted(chatReference: String) = Printer.info("Invite accepted: $chatReference")
            override fun onInviteDeclined(chatReference: String) = Printer.info("Invite declined: $chatReference")
            override fun onInviteRevoked(chatReference: String) = Printer.info("Invite revoked: $chatReference")
            override fun onGroupInviteReceived(message: Message) = Printer.info("Added to group chat: ${message.chatReference}")
            override fun onGroupRemoved(chatReference: String) = Printer.info("Removed from group: $chatReference")
            override fun onAddNewChatFailed(error: IResponse.Failure<ParentResponse<NewChatResponse>>) = Printer.error("Failed to start chat: ${error.reason}")
            override fun onError(response: IResponse.Failure<ParentResponse<String>>) = Printer.error(response.reason)
            override fun onReceive(message: Message) {}
            override fun onReceiveRecipientMessageStatus(chatRef: String, messageStatus: MessageStatus) {}
            override fun onConnect() {}
            override fun onClose(code: Int, reason: String) {}
            override fun onDisconnect(t: Throwable, response: okhttp3.Response?) {}
            override fun onError(error: ChatServiceErrorResponse<Message>) {}
        })
    }

    private fun parseTwoArgs(args: String): Pair<String, String>? {
        val parts = args.trim().split(" ", limit = 2)
        return if (parts.size == 2) parts[0] to parts[1] else null
    }
}
