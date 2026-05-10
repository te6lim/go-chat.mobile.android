package com.simulatedtez.gochat.view.redesign

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simulatedtez.gochat.model.enums.ChatScreens
import com.simulatedtez.gochat.view_model.SignupViewModel
import com.simulatedtez.gochat.view_model.SignupViewModelProvider
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(
    onSignupComplete: () -> Unit,
    onAlreadyHaveAccount: () -> Unit,
    onBackPressed: () -> Unit
) {
    val c = GoChatTheme.colors
    val context = LocalContext.current
    val viewModelFactory = remember { SignupViewModelProvider(context) }
    val viewModel: SignupViewModel = viewModel(factory = viewModelFactory)

    val isSignupSuccessful by viewModel.isSignupSuccessful.observeAsState()
    val isAutoLoginSuccessful by viewModel.isAutoLoginSuccessful.observeAsState()
    val isSigningUp by viewModel.isSigningUp.observeAsState(false)

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Auto-login after signup succeeded — go straight to conversations
    LaunchedEffect(isAutoLoginSuccessful) {
        isAutoLoginSuccessful?.let {
            if (it) {
                viewModel.initializeAppWideChatService(context)
                onSignupComplete()
            }
        }
    }

    // Signup succeeded but auto-login failed — send user to login screen
    LaunchedEffect(isSignupSuccessful) {
        isSignupSuccessful?.let {
            if (it) {
                onAlreadyHaveAccount()
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Signup failed. That username may already be taken.")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = c.surfacePage
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            IconButton(
                onClick = onBackPressed,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = c.textPrimary
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Create\nAccount",
                style = TextStyle(
                    fontFamily = FontHeading,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 48.sp,
                    letterSpacing = (-0.02 * 40).sp,
                    color = c.textPrimary
                )
            )

            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = {
                    Text(
                        text = "Username",
                        style = UiLabelStyle.copy(color = c.textMuted)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(RadiusInput),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.primaryBlue,
                    unfocusedBorderColor = c.surfaceBorder,
                    focusedContainerColor = c.surfaceCard,
                    unfocusedContainerColor = c.surfaceCard,
                    focusedLabelColor = c.primaryBlue,
                    unfocusedLabelColor = c.textMuted,
                    focusedTextColor = c.textPrimary,
                    unfocusedTextColor = c.textPrimary
                ),
                textStyle = MessageBodyStyle
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = {
                    Text(
                        text = "Password",
                        style = UiLabelStyle.copy(color = c.textMuted)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff
                                          else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Hide password"
                                                 else "Show password",
                            tint = c.textMuted
                        )
                    }
                },
                shape = RoundedCornerShape(RadiusInput),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.primaryBlue,
                    unfocusedBorderColor = c.surfaceBorder,
                    focusedContainerColor = c.surfaceCard,
                    unfocusedContainerColor = c.surfaceCard,
                    focusedLabelColor = c.primaryBlue,
                    unfocusedLabelColor = c.textMuted,
                    focusedTextColor = c.textPrimary,
                    unfocusedTextColor = c.textPrimary
                ),
                textStyle = MessageBodyStyle
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { viewModel.signUp(username, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = username.isNotBlank() && password.isNotBlank() && !isSigningUp,
                shape = RoundedCornerShape(RadiusButton),
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.primaryBlue,
                    contentColor = TextInverse,
                    disabledContainerColor = c.surfaceBorder,
                    disabledContentColor = c.textMuted
                )
            ) {
                Text(
                    text = if (isSigningUp) "Creating account…" else "Sign up",
                    style = UiLabelStyle.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextInverse
                    )
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = UiLabelStyle.copy(color = c.textSecondary)
                )
                Text(
                    text = "Log in",
                    style = UiLabelStyle.copy(
                        color = c.primaryBlue,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.clickable { onAlreadyHaveAccount() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignupScreenPreview() {
    SignupScreen(
        onSignupComplete = {},
        onAlreadyHaveAccount = {},
        onBackPressed = {}
    )
}

@Preview(showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SignupScreenDarkPreview() {
    SignupScreen(
        onSignupComplete = {},
        onAlreadyHaveAccount = {},
        onBackPressed = {}
    )
}
