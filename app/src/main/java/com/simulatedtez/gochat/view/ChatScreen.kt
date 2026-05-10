package com.simulatedtez.gochat.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import com.simulatedtez.gochat.model.enums.MessageStatus
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.model.ui.UIMessage
import com.simulatedtez.gochat.ui.theme.GoChatTheme
import com.simulatedtez.gochat.ui.theme.ReceivedBubbleDark
import com.simulatedtez.gochat.ui.theme.ReceivedBubbleLight
import com.simulatedtez.gochat.ui.theme.SentBubbleDark
import com.simulatedtez.gochat.ui.theme.SentBubbleLight
import com.simulatedtez.gochat.util.INetworkMonitor
import com.simulatedtez.gochat.util.NetworkMonitor
import com.simulatedtez.gochat.util.formatDateLabel
import com.simulatedtez.gochat.util.formatTimestamp
import com.simulatedtez.gochat.view_model.ChatViewModel
import com.simulatedtez.gochat.view_model.ChatViewModelProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.isSystemInDarkTheme

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
    val isDark = isSystemInDarkTheme()

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
    val messagesSent by chatViewModel.messagesSent.observeAsState(null)
    val presenceStatus by chatViewModel.recipientStatus.observeAsState()
    val typingTimeLeft by chatViewModel.typingTimeLeft.observeAsState()
    val isUserTyping by chatViewModel.isUserTyping.observeAsState()
    val isInvitePending by chatViewModel.isInvitePending.observeAsState(false)
    val recipientMessageStatus by chatViewModel.recipientMessageStatus.observeAsState(null)

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

    LaunchedEffect(recipientMessageStatus) {
        recipientMessageStatus?.let { newStatus ->
            val updated = messages.map { uiMsg ->
                if (uiMsg.message.sender == session.username && uiMsg.status.ordinal < newStatus.ordinal) {
                    uiMsg.copy(status = newStatus)
                } else uiMsg
            }
            messages.clear()
            messages.addAll(updated)
            chatViewModel.resetRecipientMessageStatus()
        }
    }

    LaunchedEffect(hasFinishedInitialMessagesLoad) {
        if (hasFinishedInitialMessagesLoad && !chatViewModel.isChatServiceConnected()) {
            chatViewModel.connectAndSendPendingMessages()
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                // Top bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Avatar
                        if (chatInfo.isGroup) {
                            val groupName = chatInfo.chatName.ifEmpty { "Group" }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(avatarColorFor(groupName)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Group,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else {
                            val recipientName = chatInfo.recipientsUsernames.firstOrNull() ?: ""
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(avatarColorFor(recipientName)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = recipientName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (chatInfo.isGroup) chatInfo.chatName.ifEmpty { "Group Chat" }
                                       else chatInfo.recipientsUsernames.firstOrNull() ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (chatInfo.isGroup) {
                                Text(
                                    text = "Group chat",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                presenceStatus?.let { status ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(presenceColor(status))
                                        )
                                        Text(
                                            text = when (status) {
                                                PresenceStatus.ONLINE -> "Online"
                                                PresenceStatus.AWAY -> "Away"
                                                PresenceStatus.OFFLINE -> "Offline"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = presenceColor(status)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )
                }

                // Reconnect banner
                AnimatedVisibility(
                    visible = hasFinishedInitialMessagesLoad && isConnected == false,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                        Text(
                            text = "Reconnecting...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Invite pending banner
                AnimatedVisibility(
                    visible = isInvitePending,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Waiting for ${chatInfo.recipientsUsernames[0]} to accept your invitation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
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
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                reverseLayout = true
            ) {
                if (isUserTyping == true) {
                    item {
                        TypingIndicatorBubble()
                        Spacer(modifier = Modifier.height(6.dp))
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
                                isDark = isDark,
                                onLongClick = { selectedMessage = item.uiMessage }
                            )
                            Spacer(modifier = Modifier.height(if (item.isLastInGroup) 6.dp else 2.dp))
                        }
                        is ChatListItem.DateSeparatorItem -> {
                            DateSeparatorChip(label = item.label)
                            Spacer(modifier = Modifier.height(6.dp))
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Rounded.ArrowDownward,
                        contentDescription = "Scroll to bottom",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Long-press options sheet
    selectedMessage?.let { msg ->
        ModalBottomSheet(
            onDismissRequest = { selectedMessage = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
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

// ── Helper ────────────────────────────────────────────────────────────────────

private fun presenceColor(status: PresenceStatus): Color = when (status) {
    PresenceStatus.ONLINE -> Color(0xFF22C55E)
    PresenceStatus.AWAY -> Color(0xFFF59E0B)
    PresenceStatus.OFFLINE -> Color(0xFF94A3B8)
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
fun DateSeparatorChip(label: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun MessageOptionsSheet(
    message: UIMessage,
    onCopy: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Column(modifier = Modifier.padding(bottom = 28.dp)) {
        Text(
            text = message.message.message.take(60).let {
                if (message.message.message.length > 60) "$it…" else it
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            maxLines = 2
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        TextButton(
            onClick = onCopy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                "Copy message",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        if (onDelete != null) {
            TextButton(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    "Delete message",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: UIMessage,
    isLastInGroup: Boolean,
    isDark: Boolean,
    onLongClick: () -> Unit = {}
) {
    val isSender = message.message.sender == session.username

    val bubbleColor = when {
        isSender && isDark -> SentBubbleDark
        isSender -> SentBubbleLight
        isDark -> ReceivedBubbleDark
        else -> ReceivedBubbleLight
    }

    val textColor = when {
        isSender -> Color.White
        isDark -> Color(0xFFE2E8F0)
        else -> Color(0xFF0F172A)
    }

    val alignment = if (isSender) Alignment.End else Alignment.Start

    // Tail corner: flat on the "tail" side for last in group
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (!isSender && isLastInGroup) 4.dp else 18.dp,
        bottomEnd = if (isSender && isLastInGroup) 4.dp else 18.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(bubbleColor)
                .combinedClickable(onLongClick = onLongClick, onClick = {})
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.message.message,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                lineHeight = 22.sp
            )
        }
        if (isLastInGroup) {
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Text(
                    text = formatTimestamp(message.message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSender) {
                    MessageStatusIndicator(status = message.status)
                }
            }
        }
    }
}

@Composable
fun MessageStatusIndicator(status: MessageStatus) {
    val (icon, tint) = when (status) {
        MessageStatus.SENDING -> Icons.Rounded.Schedule to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.SENT -> Icons.Rounded.Check to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.DELIVERED -> Icons.Rounded.DoneAll to MaterialTheme.colorScheme.onSurfaceVariant
        MessageStatus.SEEN -> Icons.Rounded.DoneAll to MaterialTheme.colorScheme.primary
        else -> Icons.Rounded.ErrorOutline to MaterialTheme.colorScheme.error
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(13.dp)
    )
}

@Composable
fun TypingIndicatorBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 600,
                                delayMillis = index * 150,
                                easing = EaseInOutSine
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot-$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

@Composable
fun PresenceIndicator(status: PresenceStatus?) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(if (status != null) presenceColor(status) else Color(0xFF94A3B8))
    )
}

@Composable
fun MessageInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    val hasText = message.isNotBlank()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .imePadding()
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pill-shaped text input
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (message.isEmpty()) {
                        Text(
                            text = "Message",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Send button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasText) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .combinedClickable(
                            enabled = hasText,
                            onClick = onSendClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send",
                        tint = if (hasText) MaterialTheme.colorScheme.onPrimary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    GoChatTheme {
        rememberNavController().ChatScreen(
            chatInfo = ChatInfo(
                username = "me",
                recipientsUsernames = listOf("Jane Doe"),
                chatReference = ""
            )
        )
    }
}
