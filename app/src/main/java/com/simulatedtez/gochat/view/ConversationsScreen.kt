package com.simulatedtez.gochat.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.simulatedtez.gochat.R
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.model.enums.AuthScreens
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.database.DBConversation
import com.simulatedtez.gochat.view_model.ConversationsViewModel
import com.simulatedtez.gochat.view_model.ConversationsViewModelProvider
import com.simulatedtez.gochat.ui.theme.GoChatTheme
import com.simulatedtez.gochat.util.INetworkMonitor
import com.simulatedtez.gochat.util.NetworkMonitor
import com.simulatedtez.gochat.util.formatTimestamp
import io.ktor.websocket.Frame

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

    val isConnected by viewModel.isConnected.observeAsState()

    val tokenExpired by viewModel.tokenExpired.observeAsState()

    val chatInvite by viewModel.chatInvite.observeAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val networkCallbacks = object: NetworkMonitor.Callbacks {
        override fun onAvailable() {
            viewModel.connectToChatService()
        }

        override fun onLost() {

        }
    }
    (app as INetworkMonitor).setCallback(networkCallbacks)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->

            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    viewModel.fetchConversations()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(tokenExpired) {
        tokenExpired?.let {
            if (it) {
                navigate(AuthScreens.LOGIN.name)
                viewModel.resetTokenExpired()
            }
        }
    }

    LaunchedEffect(isConnected) {
        isConnected?.let {
            /*if (it) {
                snackbarHostState.showSnackbar("socket is connected")
            } else {
                snackbarHostState.showSnackbar("socket is disconnected")
            }*/
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
            conversations.add(it)
            showBottomSheet = false
        }
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
                onClick = {
                    showBottomSheet = true
                },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Chat", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(conversations) { chat ->
                ChatItem(chat = chat, isUserTyping, screenActions)
            }
        }
    }

    chatInvite?.let { invite ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Chat Invitation") },
            text = { Text("${invite.sender} wants to chat with you.") },
            confirmButton = {
                TextButton(onClick = { viewModel.acceptInvite(invite.chatReference) }) {
                    Text("Accept")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.declineInvite(invite.chatReference) }) {
                    Text("Decline")
                }
            }
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            NewChatSheetContent(
                !waiting,
                onAddClick = { username ->
                    viewModel.addNewConversation(username, 0)
                }
            )
        }
    }
}

@Composable
fun ChatItem(chat: DBConversation, typingUser: Pair<String, Boolean>?, screenActions: ConversationsScreenActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        Image(
            painter = painterResource(id = R.drawable.ic_default_user_image),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = chat.otherUser,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            if (typingUser?.first == chat.chatReference && typingUser.second) {
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
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = formatTimestamp(chat.timestamp),
                color = Color.Gray,
                fontSize = 12.sp
            )
            if (chat.unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
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



@Composable
fun NewChatSheetContent(isEnabled:Boolean, onAddClick: (String) -> Unit) {
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
        Spacer(modifier = Modifier.height(16.dp)) // Add padding at the bottom
    }
}

interface ConversationsScreenActions {
    fun onChatClicked(chatInfo: ChatInfo)
}

@Preview(showBackground = true)
@Composable
fun ConversationsPreview() {
    GoChatTheme {
        rememberNavController().ConversationsScreen(
            screenActions = object: ConversationsScreenActions {
                override fun onChatClicked(chatInfo: ChatInfo) {

                }
            }
        )
    }
}