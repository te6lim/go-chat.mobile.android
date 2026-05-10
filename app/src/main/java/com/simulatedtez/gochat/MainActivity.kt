package com.simulatedtez.gochat

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.simulatedtez.gochat.Session.Companion.session
import com.simulatedtez.gochat.model.enums.AuthScreens
import com.simulatedtez.gochat.view.LoginScreen
import com.simulatedtez.gochat.view.SignupScreen
import com.simulatedtez.gochat.model.enums.ChatScreens
import com.simulatedtez.gochat.model.ChatInfo
import com.simulatedtez.gochat.model.enums.PresenceStatus
import com.simulatedtez.gochat.view.ConversationsScreenActions
import com.simulatedtez.gochat.ui.theme.GoChatTheme
import com.simulatedtez.gochat.view.ConversationsScreen
import com.simulatedtez.gochat.view_model.AppViewModel
import com.simulatedtez.gochat.view_model.AppViewModelProvider
import com.simulatedtez.gochat.view.redesign.AuthLandingScreen
import com.simulatedtez.gochat.view.redesign.ChatScreen
import com.simulatedtez.gochat.view.redesign.ChatScreenActions
import com.simulatedtez.gochat.view.redesign.ChatScreen as NewChatScreen
import com.simulatedtez.gochat.view.redesign.ConversationsScreen as NewConversationsScreen
import com.simulatedtez.gochat.view.redesign.LoginScreen as NewLoginScreen
import com.simulatedtez.gochat.view.redesign.SignupScreen as NewSignupScreen

// Flip to true to use the redesigned screens instead of the originals
const val USE_NEW_UI = true

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GoChatTheme {
                AppNavigation(this)
            }
        }
    }
}

@Composable
fun AppNavigation(context: Context) {

    val viewModelFactory = remember { AppViewModelProvider(context) }
    val viewModel: AppViewModel = viewModel(factory = viewModelFactory)
    viewModel.connectToChatService()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->

            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.postNewPresenceStatus(PresenceStatus.AWAY)
                }
                Lifecycle.Event.ON_DESTROY -> {
                    viewModel.postNewPresenceStatus(PresenceStatus.OFFLINE)
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navController = rememberNavController()

    val startDestination = if (UserPreference.getAccessToken() != null) {
        if (USE_NEW_UI) "new_conversations" else ChatScreens.CONVERSATIONS.name
    } else if (USE_NEW_UI) {
        "auth_landing"
    } else {
        AuthScreens.LOGIN.name
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth_landing") {
            AuthLandingScreen(
                onSignInClicked = { navController.navigate("new_login") },
                onSignInWithGoogleClicked = { /* TODO: Google OAuth */ }
            )
        }

        composable("new_login") {
            NewLoginScreen(
                onLoginSuccess = {
                    navController.navigate("new_conversations") {
                        popUpTo("auth_landing") { inclusive = true }
                    }
                },
                onCreateAccountClicked = {navController.navigate("new_signup") },
                onBackPressed = { navController.navigateUp() }
            )
        }

        composable("new_signup") {
            NewSignupScreen(
                onSignupComplete = {
                    navController.navigate("new_conversations") {
                        popUpTo("auth_landing") { inclusive = true }
                    }
                },
                onAlreadyHaveAccount = {
                    navController.navigate("new_login") {
                        popUpTo("new_signup") { inclusive = true }
                    }
                },
                onBackPressed = { navController.popBackStack() }
            )
        }

        composable("new_conversations") {
            NewConversationsScreen(
                conversationsScreenActions = object: ConversationsScreenActions {
                    override fun onChatClicked(chatInfo: ChatInfo) {
                        session.setActiveChat(chatInfo)
                        navController.navigate(ChatScreens.CHAT.name)
                    }
                }
            )
        }

        composable(AuthScreens.LOGIN.name) {
            navController.LoginScreen()
        }

        composable(AuthScreens.SIGNUP.name) {
            navController.SignupScreen()
        }

        composable(ChatScreens.CONVERSATIONS.name) {
            if (USE_NEW_UI) {
                NewConversationsScreen(
                    object: ConversationsScreenActions {
                        override fun onChatClicked(chatInfo: ChatInfo) {
                            session.setActiveChat(chatInfo)
                            navController.navigate(ChatScreens.CHAT.name)
                        }
                    }
                )
            } else {
                navController.ConversationsScreen(
                    screenActions = object: ConversationsScreenActions {
                        override fun onChatClicked(chatInfo: ChatInfo) {
                            session.setActiveChat(chatInfo)
                            navController.navigate(ChatScreens.CHAT.name)
                        }

                    }
                )
            }
        }

        composable(
            ChatScreens.CHAT.name
        ) {
            session.lastActiveChat?.let {
                if (USE_NEW_UI) {
                    NewChatScreen(
                        it, object: ChatScreenActions {
                            override fun onBack() {
                                navController.navigateUp()
                            }
                        }
                    )
                } else {
                    ChatScreen(
                        it, object: ChatScreenActions {
                            override fun onBack() {
                                navController.navigateUp()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    GoChatTheme {
        //AppNavigation(this)
    }
}