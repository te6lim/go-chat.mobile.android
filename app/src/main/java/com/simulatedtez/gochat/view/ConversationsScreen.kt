package com.simulatedtez.gochat.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.simulatedtez.gochat.model.enums.PresenceStatus
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.simulatedtez.gochat.GoChatApplication
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.database.DBConversation
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.enums.AuthScreens
import com.simulatedtez.gochat.ui.theme.GoChatTheme
import com.simulatedtez.gochat.util.INetworkMonitor
import com.simulatedtez.gochat.util.NetworkMonitor
import com.simulatedtez.gochat.util.formatTimestamp
import com.simulatedtez.gochat.view_model.ConversationsViewModel
import com.simulatedtez.gochat.view_model.ConversationsViewModelProvider
import io.ktor.websocket.Frame
import kotlin.math.abs

// ── Avatar helpers ────────────────────────────────────────────────────────────

private val avatarPalette = listOf(
    Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF6A1B9A),
    Color(0xFFC62828), Color(0xFF00695C), Color(0xFFE65100),
    Color(0xFF37474F), Color(0xFFAD1457)
)

private fun avatarColorFor(name: String): Color =
    avatarPalette[abs(name.hashCode()) % avatarPalette.size]

@Composable
private fun LetterAvatar(name: String, size: Int = 50) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(avatarColorFor(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.4).sp
        )
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavController.ConversationsScreen(screenActions: ConversationsScreenActions) {

    val app = LocalContext.current.applicationContext as GoChatApplication
    val snackbarHostState = remember { SnackbarHostState() }
    val conversations = remember { mutableStateListOf<DBConversation>() }

    val viewModelFactory = remember { ConversationsViewModelProvider(context) }
    val viewModel: ConversationsViewModel = viewModel(factory = viewModelFactory)

    val waiting by viewModel.waiting.observeAsState(false)
    val newConversation by viewModel.newConversation.collectAsState(null)
    val conversationHistory by viewModel.conversations.observeAsState(listOf())
    val errorMessage by viewModel.errorMessage.observeAsState()
    val isUserTyping by viewModel.isUserTyping.observeAsState()
    val tokenExpired by viewModel.tokenExpired.observeAsState()
    val pendingInvites by viewModel.pendingInvites.observeAsState(emptyList())
    val acceptedInviteChat by viewModel.acceptedInviteChat.collectAsState(null)

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val networkCallbacks = object : NetworkMonitor.Callbacks {
        override fun onAvailable() { viewModel.connectToChatService() }
        override fun onLost() {}
    }
    (app as INetworkMonitor).setCallback(networkCallbacks)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> viewModel.fetchConversations()
                Lifecycle.Event.ON_RESUME -> viewModel.postPresence(PresenceStatus.AWAY)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(tokenExpired) {
        tokenExpired?.let {
            if (it) {
                navigate(AuthScreens.LOGIN.name)
                viewModel.resetTokenExpired()
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            showBottomSheet = false
            snackbarHostState.showSnackbar(it)
            viewModel.resetErrorMessage()
        }
    }

    LaunchedEffect(conversationHistory) {
        if (conversationHistory.isNotEmpty()) {
            conversations.clear()
            conversations.addAll(conversationHistory)
            viewModel.connectToChatService()
        }
        viewModel.popReceivedMessagesQueue()
    }

    LaunchedEffect(newConversation) {
        newConversation?.let {
            if (conversations.none { c -> c.chatReference == it.chatReference }) {
                conversations.add(it)
            }
            showBottomSheet = false
        }
    }

    // Navigate into chat immediately on accepting an invite
    LaunchedEffect(acceptedInviteChat) {
        acceptedInviteChat?.let { screenActions.onChatClicked(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Frame.Text("Chats") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Chat", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (pendingInvites.isEmpty() && conversations.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No chats yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to start a conversation",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    // Pending invites — always at the top
                    items(pendingInvites, key = { "invite_${it.chatReference}" }) { invite ->
                        PendingInviteItem(
                            invite = invite,
                            onAccept = { viewModel.acceptInvite(invite.chatReference) },
                            onDecline = { viewModel.declineInvite(invite.chatReference) }
                        )
                    }

                    // Regular conversations — swipe-to-delete
                    items(conversations, key = { it.chatReference }) { chat ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    conversations.remove(chat)
                                    viewModel.deleteConversation(chat.chatReference)
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val bgColor by animateColorAsState(
                                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                                        Color(0xFFD32F2F) else Color.Transparent,
                                    label = "swipe-bg"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(bgColor)
                                        .padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                                }
                            }
                        ) {
                            ChatItem(chat = chat, isUserTyping = isUserTyping, screenActions = screenActions)
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            NewChatSheetContent(
                !waiting,
                onAddClick = { username -> viewModel.addNewConversation(username, 0) }
            )
        }
    }
}

// ── Pending invite item ───────────────────────────────────────────────────────

@Composable
fun PendingInviteItem(
    invite: Message,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LetterAvatar(name = invite.sender)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invite.sender,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "wants to chat with you",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Accept — filled green pill
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2E7D32)
                )
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Accept", color = Color.White, fontSize = 13.sp)
            }

            // Decline — outlined red pill
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFC62828)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC62828))
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFC62828)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Decline", fontSize = 13.sp)
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 82.dp),
        thickness = DividerDefaults.Thickness,
        color = DividerDefaults.color
    )
}

// ── Regular chat item ─────────────────────────────────────────────────────────

@Composable
fun ChatItem(
    chat: DBConversation,
    isUserTyping: Pair<String, Boolean>?,
    screenActions: ConversationsScreenActions
) {
    val isUnread = chat.unreadCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                screenActions.onChatClicked(
                    ChatInfo(
                        username = session.username,
                        recipientsUsernames = listOf(chat.otherUser),
                        chatReference = chat.chatReference
                    )
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LetterAvatar(name = chat.otherUser)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.otherUser,
                fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
                fontSize = 16.sp
            )
            if (isUserTyping?.first == chat.chatReference && isUserTyping.second) {
                Text(
                    text = "typing...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = chat.lastMessage,
                    color = if (isUnread) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatTimestamp(chat.timestamp),
                color = if (isUnread) MaterialTheme.colorScheme.primary else Color.Gray,
                fontSize = 12.sp,
                fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
            )
            if (isUnread) {
                Spacer(modifier = Modifier.height(4.dp))
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Text(text = chat.unreadCount.toString(), color = Color.White)
                }
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 82.dp),
        thickness = DividerDefaults.Thickness,
        color = DividerDefaults.color
    )
}

// ── New chat sheet ────────────────────────────────────────────────────────────

@Composable
fun NewChatSheetContent(isEnabled: Boolean, onAddClick: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Start a new chat", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Enter username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onAddClick(username) },
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() && isEnabled
        ) {
            Text("Add")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Actions interface ─────────────────────────────────────────────────────────

interface ConversationsScreenActions {
    fun onChatClicked(chatInfo: ChatInfo)
}

@Preview(showBackground = true)
@Composable
fun ConversationsPreview() {
    GoChatTheme {
        rememberNavController().ConversationsScreen(
            screenActions = object : ConversationsScreenActions {
                override fun onChatClicked(chatInfo: ChatInfo) {}
            }
        )
    }
}
