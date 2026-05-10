package com.simulatedtez.gochat.view.redesign

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.database.DBConversation
import com.simulatedtez.gochat.view.redesign.modals.NewMessageSheet
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.util.formatTimestamp
import com.simulatedtez.gochat.view.ConversationsScreenActions
import com.simulatedtez.gochat.view_model.ConversationsViewModel
import com.simulatedtez.gochat.view_model.ConversationsViewModelProvider
import kotlin.math.abs

// ── Avatar helpers ────────────────────────────────────────────────────────────

private val avatarColors = listOf(
    PrimaryBlue, TealAccent, Color(0xFF7C3AED), Color(0xFFDC2626),
    Color(0xFFD97706), Color(0xFF0891B2), Color(0xFF059669), Color(0xFFDB2777)
)

private fun avatarColor(name: String): Color =
    avatarColors[abs(name.hashCode()) % avatarColors.size]

@Composable
private fun LetterAvatar(name: String, sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(avatarColor(name)),
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

// ── Stateful screen — connects ViewModel and session to the pure UI ────────────

@Composable
fun ConversationsScreen(conversationsScreenActions: ConversationsScreenActions) {
    val context = LocalContext.current
    val factory = remember { ConversationsViewModelProvider(context) }
    val viewModel: ConversationsViewModel = viewModel(factory = factory)

    val allConversations by viewModel.conversations.observeAsState(emptyList())
    val newConversation  by viewModel.newConversation.collectAsState(null)
    val errorMessage     by viewModel.errorMessage.observeAsState()
    val isUserTyping     by viewModel.isUserTyping.observeAsState()
    val isWaiting        by viewModel.waiting.observeAsState(false)

    var searchQuery    by remember { mutableStateOf("") }
    var showSheet      by remember { mutableStateOf(false) }
    var recipientInput by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> viewModel.fetchConversations()
                Lifecycle.Event.ON_RESUME -> viewModel.postPresence(PresenceStatus.ONLINE)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetErrorMessage()
        }
    }

    LaunchedEffect(newConversation) {
        newConversation?.let { convo ->
            showSheet = false
            recipientInput = ""
            conversationsScreenActions.onChatClicked(
                ChatInfo(
                    username = session.username,
                    recipientsUsernames = listOf(convo.otherUser),
                    chatReference = convo.chatReference,
                    isGroup = convo.chatType == "group",
                    chatName = convo.chatName
                )
            )
        }
    }

    val displayedConversations = remember(allConversations, searchQuery) {
        val visible = allConversations.filter { !it.isPendingSentInvite }
        if (searchQuery.isBlank()) visible
        else visible.filter {
            val name = if (it.chatType == "group") it.chatName else it.otherUser
            name.contains(searchQuery, ignoreCase = true)
        }
    }

    ConversationsContent(
        currentUsername = session.username,
        conversations = displayedConversations,
        isUserTyping = isUserTyping,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        onNewChatClick = { showSheet = true },
        conversationsScreenActions = conversationsScreenActions,
        snackbarHostState = snackbarHostState,
        showSheet = showSheet,
        recipientInput = recipientInput,
        onRecipientChange = { recipientInput = it },
        isWaiting = isWaiting,
        onStartChat = {
            if (recipientInput.isNotBlank()) viewModel.addNewConversation(recipientInput.trim(), 0)
        },
        onDismissSheet = { showSheet = false; recipientInput = "" }
    )
}

// ── Pure UI — no ViewModel or session references, safe to preview ─────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationsContent(
    currentUsername: String,
    conversations: List<DBConversation>,
    isUserTyping: Pair<String, Boolean>?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNewChatClick: () -> Unit,
    conversationsScreenActions: ConversationsScreenActions,
    snackbarHostState: SnackbarHostState,
    showSheet: Boolean,
    recipientInput: String,
    onRecipientChange: (String) -> Unit,
    isWaiting: Boolean,
    onStartChat: () -> Unit,
    onDismissSheet: () -> Unit
) {
    val c = GoChatTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = c.surfacePage,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TopBar(currentUsername = currentUsername)

            ConversationList(
                conversations = conversations,
                currentUsername = currentUsername,
                isUserTyping = isUserTyping,
                conversationsScreenActions = conversationsScreenActions,
                modifier = Modifier.weight(1f)
            )

            BottomBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onNewChatClick = onNewChatClick
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissSheet,
            sheetState = sheetState,
            containerColor = c.surfaceCard,
            shape = RoundedCornerShape(
                topStart = RadiusSheet, topEnd = RadiusSheet,
                bottomStart = 0.dp, bottomEnd = 0.dp
            )
        ) {
            NewMessageSheet(
                recipientInput = recipientInput,
                onRecipientChange = onRecipientChange,
                isWaiting = isWaiting,
                onStartChat = onStartChat,
                onDismiss = onDismissSheet
            )
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(currentUsername: String) {
    val c = GoChatTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LetterAvatar(name = currentUsername.ifBlank { "?" }, sizeDp = 36)

        Text(
            text = "Chat",
            style = ScreenTitleStyle,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
        }
    }
}

// ── Conversation list ─────────────────────────────────────────────────────────

@Composable
private fun ConversationList(
    conversations: List<DBConversation>,
    currentUsername: String,
    isUserTyping: Pair<String, Boolean>?,
    conversationsScreenActions: ConversationsScreenActions,
    modifier: Modifier = Modifier
) {
    val c = GoChatTheme.colors
    if (conversations.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No conversations yet", style = CaptionStyle, color = c.textMuted)
        }
    } else {
        LazyColumn(modifier = modifier) {
            items(conversations, key = { it.chatReference }) { convo ->
                ConversationItem(
                    convo = convo,
                    currentUsername = currentUsername,
                    isTyping = isUserTyping?.first == convo.chatReference && isUserTyping.second,
                    conversationsScreenActions = conversationsScreenActions
                )
            }
        }
    }
}

@Composable
private fun ConversationItem(
    convo: DBConversation,
    currentUsername: String,
    isTyping: Boolean,
    conversationsScreenActions: ConversationsScreenActions
) {
    val c = GoChatTheme.colors
    val displayName = if (convo.chatType == "group") convo.chatName else convo.otherUser

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                conversationsScreenActions.onChatClicked(
                    ChatInfo(
                        username = currentUsername,
                        recipientsUsernames = listOf(convo.otherUser),
                        chatReference = convo.chatReference,
                        isGroup = convo.chatType == "group",
                        chatName = convo.chatName
                    )
                )
            }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LetterAvatar(name = displayName, sizeDp = 52)

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = UsernameStyle,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (isTyping) "typing…" else convo.lastMessage,
                style = UiLabelStyle.copy(
                    color = if (isTyping) TealAccent else c.textSecondary,
                    fontStyle = if (isTyping) FontStyle.Italic else FontStyle.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatTimestamp(convo.timestamp),
                style = TimestampStyle,
                color = c.textMuted
            )
            if (convo.unreadCount > 0) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(c.primaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (convo.unreadCount > 99) "99+" else convo.unreadCount.toString(),
                        color = TextInverse,
                        fontFamily = FontBody,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// ── Bottom bar ────────────────────────────────────────────────────────────────

@Composable
private fun BottomBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNewChatClick: () -> Unit
) {
    val c = GoChatTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.surfacePage)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter",
                tint = c.textPrimary
            )
        }

        Spacer(Modifier.width(6.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(RadiusPill))
                .background(c.surfaceBubbleIn)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (searchQuery.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = c.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = "Search", style = UiLabelStyle.copy(color = c.textMuted))
                }
            }
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                textStyle = UiLabelStyle.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.primaryBlue),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.width(6.dp))

        IconButton(onClick = onNewChatClick) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "New chat",
                tint = c.textPrimary
            )
        }
    }
}

// ── New message sheet ─────────────────────────────────────────────────────────

// ── Previews ──────────────────────────────────────────────────────────────────

private val previewConversations = listOf(
    DBConversation(chatReference = "1", otherUser = "Anansi",   lastMessage = "mate's stopped making direct logic sinc…", timestamp = "", unreadCount = 0),
    DBConversation(chatReference = "2", otherUser = "Olaniyi",  lastMessage = "how's it been?",  timestamp = "", unreadCount = 3),
    DBConversation(chatReference = "3", otherUser = "pacl!taxel", lastMessage = "Reacted 💯 to \"😅\"", timestamp = "", unreadCount = 0),
    DBConversation(chatReference = "4", otherUser = "Eshilokun", lastMessage = "You reacted 🙌 to \"Sharp. Thank you, K…\"", timestamp = "", unreadCount = 0),
    DBConversation(chatReference = "5", otherUser = "Maester",  lastMessage = "Awesome", timestamp = "", unreadCount = 1),
)

@Preview(showBackground = true)
@Composable
fun ConversationsScreenPreview() {
    ConversationsContent(
        currentUsername = "Anansi",
        conversations = previewConversations,
        isUserTyping = null,
        searchQuery = "",
        onSearchQueryChange = {},
        onNewChatClick = {},
        conversationsScreenActions = object: ConversationsScreenActions {
            override fun onChatClicked(chatInfo: ChatInfo) {

            }
        },
        snackbarHostState = remember { SnackbarHostState() },
        showSheet = false,
        recipientInput = "",
        onRecipientChange = {},
        isWaiting = false,
        onStartChat = {},
        onDismissSheet = {}
    )
}

@Preview(showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ConversationsScreenDarkPreview() {
    ConversationsContent(
        currentUsername = "Anansi",
        conversations = previewConversations,
        isUserTyping = null,
        searchQuery = "",
        onSearchQueryChange = {},
        onNewChatClick = {},
        conversationsScreenActions = object: ConversationsScreenActions {
            override fun onChatClicked(chatInfo: ChatInfo) {

            }
        },
        snackbarHostState = remember { SnackbarHostState() },
        showSheet = false,
        recipientInput = "",
        onRecipientChange = {},
        isWaiting = false,
        onStartChat = {},
        onDismissSheet = {}
    )
}

