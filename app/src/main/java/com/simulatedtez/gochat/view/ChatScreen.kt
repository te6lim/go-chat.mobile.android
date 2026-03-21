package com.simulatedtez.gochat.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.simulatedtez.gochat.GoChatApplication
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.enums.AuthScreens
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.model.ui.UIMessage
import com.simulatedtez.gochat.util.INetworkMonitor
import com.simulatedtez.gochat.util.NetworkMonitor
import com.simulatedtez.gochat.util.formatDateLabel
import com.simulatedtez.gochat.util.formatTimestamp
import com.simulatedtez.gochat.view_model.ChatViewModel
import com.simulatedtez.gochat.view_model.ChatViewModelProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Chat list item types ──────────────────────────────────────────────────────

sealed class ChatListItem {
    data class MessageItem(val uiMessage: UIMessage, val isLastInGroup: Boolean) : ChatListItem()
    data class DateSeparatorItem(val label: String) : ChatListItem()
}

fun buildChatList(messages: Collection<UIMessage>): List<ChatListItem> {
    val sorted = messages.sortedBy { it.message.timestamp }
    val result = mutableListOf<ChatListItem>()
    var lastDate = ""
    for (i in sorted.indices) {
        val currentDate = sorted[i].message.timestamp.take(10)
        if (currentDate != lastDate) {
            result.add(ChatListItem.DateSeparatorItem(formatDateLabel(sorted[i].message.timestamp)))
            lastDate = currentDate
        }
        val isLastInGroup = i == sorted.size - 1 ||
                sorted[i + 1].message.sender != sorted[i].message.sender
        result.add(ChatListItem.MessageItem(sorted[i], isLastInGroup))
    }
    return result
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NavController.ChatScreen(chatInfo: ChatInfo) {

    val app = LocalContext.current.applicationContext as GoChatApplication
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val chatViewModelProvider = remember { ChatViewModelProvider(chatInfo = chatInfo, context) }
    val chatViewModel: ChatViewModel = viewModel(factory = chatViewModelProvider)

    val messages = remember { mutableStateSetOf<UIMessage>() }
    var messageText by remember { mutableStateOf("") }
    var hasFinishedInitialMessagesLoad by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<UIMessage?>(null) }
    val sheetState = rememberModalBottomSheetState()

    val newMessage by chatViewModel.newMessage.observeAsState(null)
    val pagedMessages by chatViewModel.pagedMessages.observeAsState()
    val sentMessage by chatViewModel.sendMessageAttempt.observeAsState()
    val isConnected by chatViewModel.isConnected.observeAsState()
    val tokenExpired by chatViewModel.tokenExpired.observeAsState()
    val messagesSent by chatViewModel.messagesSent.observeAsState(null)
    val presenceStatus by chatViewModel.recipientStatus.observeAsState()
    val typingTimeLeft by chatViewModel.typingTimeLeft.observeAsState()
    val isUserTyping by chatViewModel.isUserTyping.observeAsState()
    val isInvitePending by chatViewModel.isInvitePending.observeAsState(false)

    val listState = rememberLazyListState()
    val showScrollToBottom by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
    val chatList by remember { derivedStateOf { buildChatList(messages).reversed() } }

    val networkCallbacks = object : NetworkMonitor.Callbacks {
        override fun onAvailable() {
            if (hasFinishedInitialMessagesLoad) chatViewModel.connectAndSendPendingMessages()
        }
        override fun onLost() {}
    }
    (app as INetworkMonitor).setCallback(networkCallbacks)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> chatViewModel.loadMessages()
                Lifecycle.Event.ON_PAUSE -> chatViewModel.postPresence(PresenceStatus.AWAY)
                Lifecycle.Event.ON_RESUME -> {
                    chatViewModel.postPresence(PresenceStatus.ONLINE)
                    if (!chatViewModel.isChatServiceConnected()) chatViewModel.connectAndSendPendingMessages()
                }
                Lifecycle.Event.ON_DESTROY -> chatViewModel.exitChat()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            chatViewModel.exitChat()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(typingTimeLeft) {
        typingTimeLeft?.let {
            if (it > 0) {
                delay(1000L)
                chatViewModel.countdownTypingTimeBy(1)
            } else {
                delay(1000L)
                if (chatViewModel.isTyping) {
                    chatViewModel.restartTypingTimer(chatViewModel.newCharCount)
                } else {
                    chatViewModel.postMessageStatus(MessageStatus.NOT_TYPING)
                    chatViewModel.stopTypingTimer()
                }
            }
        }
    }

    LaunchedEffect(messagesSent) {
        messagesSent?.let { msg ->
            val modifiedMessages = messages.toMutableList()
            val idx = modifiedMessages.indexOfFirst { it.message.id == msg.message.id }
            if (idx != -1) modifiedMessages[idx] = msg
            messages.clear()
            messages.addAll(modifiedMessages)
            chatViewModel.popSentMessagesQueue()
        }
    }

    LaunchedEffect(hasFinishedInitialMessagesLoad) {
        if (hasFinishedInitialMessagesLoad && !chatViewModel.isChatServiceConnected()) {
            chatViewModel.connectAndSendPendingMessages()
        }
    }

    LaunchedEffect(tokenExpired) {
        tokenExpired?.let {
            if (it) {
                navigate(AuthScreens.LOGIN.name)
                chatViewModel.resetTokenExpired()
            }
        }
    }

    LaunchedEffect(sentMessage) {
        sentMessage?.let {
            messages.add(it)
            chatViewModel.resetSendAttempt()
        }
    }

    LaunchedEffect(pagedMessages) {
        pagedMessages?.let {
            if (it.paginationCount <= 1) {
                messages.clear()
                messages.addAll(it.messages)
                chatViewModel.markConversationAsOpened()
            } else {
                messages.addAll(it.messages)
            }
            if (!hasFinishedInitialMessagesLoad) hasFinishedInitialMessagesLoad = true
        }
    }

    LaunchedEffect(newMessage) {
        newMessage?.let {
            messages.add(it)
            chatViewModel.onUserPresenceOnline {
                chatViewModel.markMessagesAsSeenIfEnabled(listOf(it.message))
            }
            chatViewModel.popReceivedMessagesQueue()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }.collect { visibleItems ->
            if (isUserTyping != true && session.isReadReceiptEnabled) {
                val messageList = messages.toList()
                val visibleMessages = visibleItems.mapNotNull { item ->
                    messageList.getOrNull((messageList.size - 1) - item.index)
                }
                val unseenMessages = visibleMessages.filter { m ->
                    m.message.sender != session.username && m.message.seenTimestamp.isNullOrEmpty()
                }
                chatViewModel.markMessagesAsSeenIfEnabled(unseenMessages.map { it.message })
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(chatInfo.recipientsUsernames[0], fontWeight = FontWeight.Bold)
                            presenceStatus?.let { status ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PresenceIndicator(status = status)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when (status) {
                                            PresenceStatus.ONLINE -> "Online"
                                            PresenceStatus.AWAY -> "Away"
                                            PresenceStatus.OFFLINE -> "Offline"
                                        },
                                        fontSize = 12.sp,
                                        color = when (status) {
                                            PresenceStatus.ONLINE -> Color(0xFF4CAF50)
                                            PresenceStatus.AWAY -> Color(0xFFFFC107)
                                            PresenceStatus.OFFLINE -> Color.Gray
                                        }
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigateUp() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Reconnect banner
                AnimatedVisibility(
                    visible = hasFinishedInitialMessagesLoad && isConnected == false,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFC107))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Reconnecting...",
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Invite pending banner
                AnimatedVisibility(
                    visible = isInvitePending,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF9C4))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Waiting for ${chatInfo.recipientsUsernames[0]} to accept your invitation.",
                            fontSize = 13.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                }
            }
        },
        bottomBar = {
            MessageInputBar(
                message = messageText,
                onMessageChange = {
                    if (messageText.length < it.length) {
                        if (messageText.isEmpty()) {
                            chatViewModel.restartTypingTimer(it.length)
                            chatViewModel.postMessageStatus(MessageStatus.TYPING)
                        } else {
                            if (typingTimeLeft == null) {
                                chatViewModel.restartTypingTimer(it.length)
                                chatViewModel.postMessageStatus(MessageStatus.TYPING)
                            } else {
                                chatViewModel.newCharCount = it.length
                            }
                        }
                    } else {
                        chatViewModel.newCharCount = it.length
                    }
                    messageText = it
                },
                onSendClick = {
                    chatViewModel.stopTypingTimer()
                    chatViewModel.sendMessage(messageText)
                    messageText = ""
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                reverseLayout = true
            ) {
                if (isUserTyping == true) {
                    item {
                        TypingIndicatorBubble()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                items(
                    chatList,
                    key = { item ->
                        when (item) {
                            is ChatListItem.MessageItem -> item.uiMessage.message.id
                            is ChatListItem.DateSeparatorItem -> "sep_${item.label}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is ChatListItem.MessageItem -> {
                            MessageBubble(
                                message = item.uiMessage,
                                isLastInGroup = item.isLastInGroup,
                                onLongClick = { selectedMessage = item.uiMessage }
                            )
                            Spacer(modifier = Modifier.height(if (item.isLastInGroup) 8.dp else 2.dp))
                        }
                        is ChatListItem.DateSeparatorItem -> {
                            DateSeparatorChip(label = item.label)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Scroll-to-bottom FAB
            AnimatedVisibility(
                visible = showScrollToBottom,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = "Scroll to bottom",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Long-press options sheet
    selectedMessage?.let { msg ->
        ModalBottomSheet(
            onDismissRequest = { selectedMessage = null },
            sheetState = sheetState
        ) {
            MessageOptionsSheet(
                message = msg,
                onCopy = {
                    clipboardManager.setText(AnnotatedString(msg.message.message))
                    selectedMessage = null
                },
                onDelete = if (msg.message.sender == session.username) ({
                    chatViewModel.deleteMessage(msg.message)
                    messages.remove(msg)
                    selectedMessage = null
                }) else null
            )
        }
    }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
fun DateSeparatorChip(label: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MessageOptionsSheet(
    message: UIMessage,
    onCopy: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        TextButton(
            onClick = onCopy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Copy", modifier = Modifier.weight(1f))
        }
        if (onDelete != null) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Delete", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: UIMessage,
    isLastInGroup: Boolean,
    onLongClick: () -> Unit = {}
) {
    val isSender = message.message.sender == session.username

    val bubbleColor = if (isSender) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.inversePrimary

    val textColor = if (isSender) Color.White else Color.Black
    val alignment = if (isSender) Alignment.End else Alignment.Start

    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (!isSender && isLastInGroup) 0.dp else 16.dp,
        bottomEnd = if (isSender && isLastInGroup) 0.dp else 16.dp
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = alignment
        ) {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bubbleColor)
                    .combinedClickable(onLongClick = onLongClick, onClick = {})
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(text = message.message.message, color = textColor)
            }
            if (isLastInGroup) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatTimestamp(message.message.timestamp),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    if (isSender) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIndicator(status = message.status)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageStatusIndicator(status: MessageStatus) {
    val (icon, color) = when (status) {
        MessageStatus.SENDING -> Icons.Default.Schedule to Color.Gray
        MessageStatus.SENT -> Icons.Default.Check to Color.Gray
        MessageStatus.DELIVERED -> Icons.Default.DoneAll to Color.Gray
        MessageStatus.SEEN -> Icons.Default.DoneAll to Color(0xFF2196F3)
        else -> Icons.Default.Error to MaterialTheme.colorScheme.error
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(14.dp)
    )
}

@Composable
fun TypingIndicatorBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotBounceOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "dot-bounce"
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypingDot(delay = 0.dp, bounce = dotBounceOffsetY)
                TypingDot(delay = 160.dp, bounce = dotBounceOffsetY)
                TypingDot(delay = 320.dp, bounce = dotBounceOffsetY)
            }
        }
    }
}

@Composable
fun TypingDot(delay: Dp, bounce: Float) {
    var yOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(bounce) {
        delay(delay.value.toLong())
        yOffset = bounce
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .offset(y = yOffset.dp)
            .clip(CircleShape)
            .background(Color.Gray.copy(alpha = 0.7f))
    )
}

@Composable
fun PresenceIndicator(status: PresenceStatus?) {
    val color = when (status) {
        PresenceStatus.ONLINE -> Color(0xFF4CAF50)
        PresenceStatus.AWAY -> Color(0xFFFFC107)
        else -> Color.Gray
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun MessageInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSendClick,
                enabled = message.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    rememberNavController().ChatScreen(
        chatInfo = ChatInfo(
            username = session.username,
            recipientsUsernames = listOf("Jane Doe"),
            chatReference = ""
        )
    )
}
