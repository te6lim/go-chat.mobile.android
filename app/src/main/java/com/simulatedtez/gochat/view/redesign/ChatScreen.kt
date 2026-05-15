package com.simulatedtez.gochat.view.redesign

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.simulatedtez.gochat.GoChatApplication
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.Message
import com.simulatedtez.gochat.model.enums.MessageStatus as ModelStatus
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.model.ui.UIMessage
import com.simulatedtez.gochat.util.BackStackActions
import com.simulatedtez.gochat.util.INetworkMonitor
import com.simulatedtez.gochat.util.NetworkMonitor
import com.simulatedtez.gochat.view.ChatListItem
import com.simulatedtez.gochat.view.buildChatList
import com.simulatedtez.gochat.view.redesign.chatitems.MessageStatus as BubbleStatus
import com.simulatedtez.gochat.view.redesign.chatitems.ReceivedChatBubble
import com.simulatedtez.gochat.view.redesign.chatitems.SentChatBubble
import com.simulatedtez.gochat.view_model.ChatViewModel
import com.simulatedtez.gochat.view_model.ChatViewModelProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

interface ChatScreenActions: BackStackActions {
}

// ── Avatar helpers ────────────────────────────────────────────────────────────

private val chatAvatarColors = listOf(
    PrimaryBlue, TealAccent, Color(0xFF7C3AED), Color(0xFFDC2626),
    Color(0xFFD97706), Color(0xFF0891B2), Color(0xFF059669), Color(0xFFDB2777)
)

private fun chatAvatarColor(name: String): Color =
    chatAvatarColors[abs(name.hashCode()) % chatAvatarColors.size]

@Composable
private fun ChatLetterAvatar(name: String, sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(chatAvatarColor(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = TextInverse,
            fontFamily = FontHeading,
            fontWeight = FontWeight.SemiBold,
            fontSize = (sizeDp * 0.38f).sp
        )
    }
}

// ── Presence helpers ──────────────────────────────────────────────────────────

@Composable
private fun presenceColor(status: PresenceStatus): Color = when (status) {
    PresenceStatus.ONLINE  -> ColorOnline
    PresenceStatus.AWAY    -> ColorAway
    PresenceStatus.OFFLINE -> GoChatTheme.colors.textMuted
}

private fun presenceLabel(status: PresenceStatus): String = when (status) {
    PresenceStatus.ONLINE  -> "Online"
    PresenceStatus.AWAY    -> "Away"
    PresenceStatus.OFFLINE -> "Offline"
}

// ── Status mapping ────────────────────────────────────────────────────────────

private fun ModelStatus.toBubbleStatus(): BubbleStatus? = when (this) {
    ModelStatus.DELIVERED -> BubbleStatus.DELIVERED
    ModelStatus.SEEN      -> BubbleStatus.SEEN
    else                  -> null
}

// ── Stateful screen ───────────────────────────────────────────────────────────

@Composable
fun ChatScreen(chatInfo: ChatInfo, chatScreenActions: ChatScreenActions) {
    val context        = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope          = rememberCoroutineScope()
    val app            = context.applicationContext as GoChatApplication

    val chatViewModelProvider = remember { ChatViewModelProvider(chatInfo, context) }
    val chatViewModel: ChatViewModel = viewModel(factory = chatViewModelProvider)

    val messages = remember { mutableStateSetOf<UIMessage>() }
    var messageText          by remember { mutableStateOf("") }
    var hasFinishedInitialLoad by remember { mutableStateOf(false) }
    var selectedMessage      by remember { mutableStateOf<UIMessage?>(null) }

    val newMessage             by chatViewModel.newMessage.observeAsState()
    val pagedMessages          by chatViewModel.pagedMessages.observeAsState()
    val sentMessage            by chatViewModel.sendMessageAttempt.observeAsState()
    val isConnected            by chatViewModel.isConnected.observeAsState()
    val messagesSent           by chatViewModel.messagesSent.observeAsState()
    val presenceStatus         by chatViewModel.recipientStatus.observeAsState()
    val typingTimeLeft         by chatViewModel.typingTimeLeft.observeAsState()
    val isUserTyping           by chatViewModel.isUserTyping.observeAsState()
    val isInvitePending        by chatViewModel.isInvitePending.observeAsState(false)
    val recipientMessageStatus by chatViewModel.recipientMessageStatus.observeAsState()

    val listState = rememberLazyListState()
    val chatList by remember { derivedStateOf { buildChatList(messages).reversed() } }

    val networkCallbacks = remember {
        object : NetworkMonitor.Callbacks {
            override fun onAvailable() {
                if (hasFinishedInitialLoad) chatViewModel.connectAndSendPendingMessages()
            }
            override fun onLost() {}
        }
    }
    (app as INetworkMonitor).setCallback(networkCallbacks)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE  -> chatViewModel.loadMessages()
                Lifecycle.Event.ON_PAUSE   -> chatViewModel.postPresence(PresenceStatus.AWAY)
                Lifecycle.Event.ON_RESUME  -> {
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
                    chatViewModel.postMessageStatus(ModelStatus.NOT_TYPING)
                    chatViewModel.stopTypingTimer()
                }
            }
        }
    }

    LaunchedEffect(messagesSent) {
        messagesSent?.let { msg ->
            val modified = messages.toMutableList()
            val idx = modified.indexOfFirst { it.message.id == msg.message.id }
            if (idx != -1) modified[idx] = msg
            messages.clear()
            messages.addAll(modified)
            chatViewModel.popSentMessagesQueue()
        }
    }

    LaunchedEffect(recipientMessageStatus) {
        recipientMessageStatus?.let { newStatus ->
            val updated = messages.map { uiMsg ->
                if (uiMsg.message.sender == session.username && uiMsg.status.ordinal < newStatus.ordinal)
                    uiMsg.copy(status = newStatus)
                else uiMsg
            }
            messages.clear()
            messages.addAll(updated)
            chatViewModel.resetRecipientMessageStatus()
        }
    }

    LaunchedEffect(hasFinishedInitialLoad) {
        if (hasFinishedInitialLoad && !chatViewModel.isChatServiceConnected())
            chatViewModel.connectAndSendPendingMessages()
    }

    LaunchedEffect(sentMessage) {
        sentMessage?.let {
            messages.add(it)
            chatViewModel.resetSendAttempt()
            if (listState.firstVisibleItemIndex <= 2) {
                listState.animateScrollToItem(0)
            }
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
            if (!hasFinishedInitialLoad) hasFinishedInitialLoad = true
        }
    }

    LaunchedEffect(newMessage) {
        newMessage?.let {
            // Guard against duplicates: pagedMessages may already contain this message
            // with a different status value (UIMessage equality includes status), which
            // would let both copies into the Set and cause a LazyColumn key collision.
            if (messages.none { m -> m.message.id == it.message.id }) {
                messages.add(it)
            }
            chatViewModel.onUserPresenceOnline {
                chatViewModel.markMessagesAsSeenIfEnabled(listOf(it.message))
            }
            chatViewModel.popReceivedMessagesQueue()
            if (listState.firstVisibleItemIndex <= 2) {
                listState.animateScrollToItem(0)
            }
        }
    }

    LaunchedEffect(isUserTyping) {
        if (isUserTyping == true && listState.firstVisibleItemIndex <= 2) {
            listState.animateScrollToItem(0)
        }
    }

    ChatContent(
        chatInfo            = chatInfo,
        currentUsername     = session.username,
        chatList            = chatList,
        newMessageId        = newMessage?.message?.id,
        messageText         = messageText,
        onMessageChange     = { newText ->
            if (messageText.length < newText.length) {
                if (messageText.isEmpty()) {
                    chatViewModel.restartTypingTimer(newText.length)
                    chatViewModel.postMessageStatus(ModelStatus.TYPING)
                } else {
                    if (typingTimeLeft == null) {
                        chatViewModel.restartTypingTimer(newText.length)
                        chatViewModel.postMessageStatus(ModelStatus.TYPING)
                    } else {
                        chatViewModel.newCharCount = newText.length
                    }
                }
            } else {
                chatViewModel.newCharCount = newText.length
            }
            messageText = newText
        },
        onSendClick         = {
            chatViewModel.stopTypingTimer()
            chatViewModel.sendMessage(messageText)
            messageText = ""
        },
        presenceStatus      = presenceStatus,
        isUserTyping        = isUserTyping,
        isConnected         = isConnected,
        hasFinishedInitialLoad = hasFinishedInitialLoad,
        isInvitePending     = isInvitePending,
        selectedMessage     = selectedMessage,
        onMessageLongClick  = { selectedMessage = it },
        onDismissSheet      = { selectedMessage = null },
        onCopyMessage       = {
            clipboardManager.setText(AnnotatedString(it.message.message))
            selectedMessage = null
        },
        onDeleteMessage     = {
            chatViewModel.deleteMessage(it.message)
            messages.remove(it)
            selectedMessage = null
        },
        listState           = listState,
        onScrollToBottom    = { scope.launch { listState.animateScrollToItem(0) } },
        chatScreenActions   = chatScreenActions
    )
}

// ── Pure UI ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ChatContent(
    chatInfo: ChatInfo,
    currentUsername: String,
    chatList: List<ChatListItem>,
    newMessageId: String?,
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    presenceStatus: PresenceStatus?,
    isUserTyping: Boolean?,
    isConnected: Boolean?,
    hasFinishedInitialLoad: Boolean,
    isInvitePending: Boolean,
    selectedMessage: UIMessage?,
    onMessageLongClick: (UIMessage) -> Unit,
    onDismissSheet: () -> Unit,
    onCopyMessage: (UIMessage) -> Unit,
    onDeleteMessage: (UIMessage) -> Unit,
    listState: LazyListState,
    onScrollToBottom: () -> Unit,
    chatScreenActions: ChatScreenActions
) {
    val c = GoChatTheme.colors
    val sheetState = rememberModalBottomSheetState()
    val showScrollToBottom by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
    val displayName = if (chatInfo.isGroup) chatInfo.chatName.ifEmpty { "Group" }
                      else chatInfo.recipientsUsernames.firstOrNull() ?: ""

    Scaffold(
        containerColor = c.surfacePage,
        topBar = {
            Column {
                ChatTopBar(
                    displayName    = displayName,
                    presenceStatus = presenceStatus,
                    onBack         = chatScreenActions::onBack
                )

                AnimatedVisibility(
                    visible = hasFinishedInitialLoad && isConnected == false,
                    enter   = slideInVertically() + fadeIn(),
                    exit    = slideOutVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorError)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(TextInverse)
                        )
                        Text(
                            text = "Reconnecting…",
                            style = UiLabelStyle.copy(color = TextInverse)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isInvitePending,
                    enter   = slideInVertically() + fadeIn(),
                    exit    = slideOutVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF92400E)
                        )
                        Text(
                            text = "Waiting for ${chatInfo.recipientsUsernames.firstOrNull() ?: "them"} to accept your invitation",
                            style = UiLabelStyle.copy(color = Color(0xFF92400E))
                        )
                    }
                }
            }
        },
        bottomBar = {
            ChatInputBar(
                message         = messageText,
                onMessageChange = onMessageChange,
                onSendClick     = onSendClick
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state          = listState,
                modifier       = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                reverseLayout  = true,
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                if (isUserTyping == true) {
                    item {
                        ChatTypingIndicator()
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                items(
                    chatList,
                    key = { item ->
                        when (item) {
                            is ChatListItem.MessageItem      -> item.uiMessage.message.id
                            is ChatListItem.DateSeparatorItem -> "sep_${item.label}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is ChatListItem.MessageItem -> {
                            val isSender = item.uiMessage.message.sender == currentUsername
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onLongClick = { onMessageLongClick(item.uiMessage) },
                                        onClick     = {}
                                    ),
                                contentAlignment = if (isSender) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                if (isSender) {
                                    SentChatBubble(
                                        text   = item.uiMessage.message.message,
                                        status = if (item.isLastInGroup) item.uiMessage.status.toBubbleStatus() else null
                                    )
                                } else {
                                    val msgId = item.uiMessage.message.id
                                    ReceivedChatBubble(
                                        text      = item.uiMessage.message.message,
                                        messageId = msgId,
                                        animateIn = msgId == newMessageId && session.isRevealAnimationEnabled
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(if (item.isLastInGroup) 6.dp else 2.dp))
                        }

                        is ChatListItem.DateSeparatorItem -> {
                            ChatDateSeparator(label = item.label)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible  = showScrollToBottom,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                enter = fadeIn(),
                exit  = fadeOut()
            ) {
                FloatingActionButton(
                    onClick        = onScrollToBottom,
                    modifier       = Modifier.size(40.dp),
                    containerColor = c.surfaceCard,
                    contentColor   = c.primaryBlue,
                    shape          = CircleShape
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

    selectedMessage?.let { msg ->
        ModalBottomSheet(
            onDismissRequest = onDismissSheet,
            sheetState       = sheetState,
            containerColor   = c.surfaceCard,
            shape = RoundedCornerShape(
                topStart = RadiusSheet, topEnd = RadiusSheet,
                bottomStart = 0.dp, bottomEnd = 0.dp
            )
        ) {
            ChatMessageOptionsSheet(
                message   = msg,
                onCopy    = { onCopyMessage(msg) },
                onDelete  = if (msg.message.sender == currentUsername) ({ onDeleteMessage(msg) }) else null
            )
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatTopBar(
    displayName: String,
    presenceStatus: PresenceStatus?,
    onBack: () -> Unit
) {
    val c = GoChatTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(36.dp)
                .clip(CircleShape)
                .background(c.surfaceBubbleIn)
                .combinedClickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint               = c.textPrimary,
                modifier           = Modifier.size(18.dp)
            )
        }

        Column(
            modifier              = Modifier.align(Alignment.Center),
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Box {
                ChatLetterAvatar(name = displayName, sizeDp = 40)
                if (presenceStatus != null && presenceStatus != PresenceStatus.OFFLINE) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(c.surfacePage)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(presenceColor(presenceStatus))
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text  = displayName,
                style = UsernameStyle,
                color = c.textPrimary
            )
            if (presenceStatus != null) {
                Text(
                    text  = presenceLabel(presenceStatus),
                    style = CaptionStyle.copy(color = presenceColor(presenceStatus))
                )
            }
        }
    }

    HorizontalDivider(color = c.surfaceBorder, thickness = 1.dp)
}

// ── Date separator ────────────────────────────────────────────────────────────

@Composable
private fun ChatDateSeparator(label: String) {
    val c = GoChatTheme.colors
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text      = label,
            style     = CaptionStyle.copy(color = c.textSecondary, fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center,
            modifier  = Modifier
                .clip(RoundedCornerShape(RadiusPill))
                .background(c.surfaceBorder)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

// ── Typing indicator ──────────────────────────────────────────────────────────

@Composable
private fun ChatTypingIndicator() {
    val c = GoChatTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    topStart    = RadiusBubble,
                    topEnd      = RadiusBubble,
                    bottomEnd   = RadiusBubble,
                    bottomStart = RadiusBubbleTail
                )
            )
            .background(c.surfaceBubbleIn)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue  = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 600,
                            delayMillis    = index * 150,
                            easing         = EaseInOutSine
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot-$index"
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(c.textSecondary.copy(alpha = alpha))
                )
            }
        }
    }
}

// ── Input bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    val c = GoChatTheme.colors
    val hasText = message.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surfacePage)
    ) {
        HorizontalDivider(color = c.surfaceBorder, thickness = 1.dp)
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.surfaceBubbleIn),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Rounded.Add,
                    contentDescription = "Attach",
                    tint               = c.textSecondary,
                    modifier           = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(RadiusInput))
                    .background(c.surfaceBubbleIn)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (message.isEmpty()) {
                    Text(text = "Message", style = MessageBodyStyle.copy(color = c.textMuted))
                }
                BasicTextField(
                    value           = message,
                    onValueChange   = onMessageChange,
                    textStyle       = MessageBodyStyle.copy(color = c.textPrimary),
                    cursorBrush     = SolidColor(c.primaryBlue),
                    modifier        = Modifier.fillMaxWidth(),
                    maxLines        = 5
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (hasText) c.primaryBlue else c.surfaceBubbleIn)
                    .combinedClickable(
                        enabled = hasText,
                        onClick = onSendClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send",
                    tint               = if (hasText) TextInverse else c.textMuted,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Message options sheet ─────────────────────────────────────────────────────

@Composable
private fun ChatMessageOptionsSheet(
    message: UIMessage,
    onCopy: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val c = GoChatTheme.colors
    Column(modifier = Modifier.padding(bottom = 28.dp)) {
        Text(
            text     = message.message.message.take(60).let {
                if (message.message.message.length > 60) "$it…" else it
            },
            style    = UiLabelStyle.copy(color = c.textSecondary),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            maxLines = 2
        )
        HorizontalDivider(color = c.surfaceBorder)
        TextButton(
            onClick  = onCopy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = null,
                modifier           = Modifier.size(18.dp),
                tint               = c.textPrimary
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text     = "Copy message",
                style    = UiLabelStyle.copy(color = c.textPrimary),
                modifier = Modifier.weight(1f)
            )
        }
        if (onDelete != null) {
            TextButton(
                onClick  = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp),
                    tint               = ColorError
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text     = "Delete message",
                    style    = UiLabelStyle.copy(color = ColorError),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

private const val PreviewMe   = "Anansi"
private const val PreviewThem = "Olaniyi"

private fun previewMsg(
    id: String,
    text: String,
    sender: String,
    status: ModelStatus = ModelStatus.SENT,
    isLastInGroup: Boolean = true
) = ChatListItem.MessageItem(
    uiMessage = UIMessage(
        message = Message(
            id            = id,
            message       = text,
            sender        = sender,
            receiver      = if (sender == PreviewMe) PreviewThem else PreviewMe,
            timestamp     = "2025-04-05T10:0${id}:00",
            chatReference = "preview"
        ),
        status = status
    ),
    isLastInGroup = isLastInGroup
)

private val previewChatList = listOf(
    ChatListItem.DateSeparatorItem("Sun, Apr 5"),
    previewMsg("1", "Hey, you around?",                     PreviewThem),
    previewMsg("2", "Yeah, what's up?",                     PreviewMe,   ModelStatus.SEEN,      isLastInGroup = false),
    previewMsg("3", "On my way!",                           PreviewMe,   ModelStatus.SEEN,      isLastInGroup = false),
    previewMsg("4", "Be there in 10",                       PreviewMe,   ModelStatus.DELIVERED, isLastInGroup = true),
    previewMsg("5", "Nice, I'll grab us a table",           PreviewThem, isLastInGroup = false),
    previewMsg("6", "The one near the window 🪟",           PreviewThem, isLastInGroup = true),
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChatScreenLightPreview() {
    ChatContent(
        chatInfo               = ChatInfo(
            username            = PreviewMe,
            recipientsUsernames = listOf(PreviewThem),
            chatReference       = "preview"
        ),
        currentUsername        = PreviewMe,
        chatList               = previewChatList.reversed(),
        newMessageId           = null,
        messageText            = "",
        onMessageChange        = {},
        onSendClick            = {},
        presenceStatus         = PresenceStatus.ONLINE,
        isUserTyping           = false,
        isConnected            = true,
        hasFinishedInitialLoad = true,
        isInvitePending        = false,
        selectedMessage        = null,
        onMessageLongClick     = {},
        onDismissSheet         = {},
        onCopyMessage          = {},
        onDeleteMessage        = {},
        listState              = rememberLazyListState(),
        onScrollToBottom       = {},
        chatScreenActions      = object: ChatScreenActions {
            override fun onBack() {

            }
        }
    )
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ChatScreenDarkPreview() {
    ChatContent(
        chatInfo               = ChatInfo(
            username            = PreviewMe,
            recipientsUsernames = listOf(PreviewThem),
            chatReference       = "preview"
        ),
        currentUsername        = PreviewMe,
        chatList               = previewChatList.reversed(),
        newMessageId           = null,
        messageText            = "",
        onMessageChange        = {},
        onSendClick            = {},
        presenceStatus         = PresenceStatus.ONLINE,
        isUserTyping           = false,
        isConnected            = true,
        hasFinishedInitialLoad = true,
        isInvitePending        = false,
        selectedMessage        = null,
        onMessageLongClick     = {},
        onDismissSheet         = {},
        onCopyMessage          = {},
        onDeleteMessage        = {},
        listState              = rememberLazyListState(),
        onScrollToBottom       = {},
        chatScreenActions      = object: ChatScreenActions {
            override fun onBack() {

            }
        }
    )
}
