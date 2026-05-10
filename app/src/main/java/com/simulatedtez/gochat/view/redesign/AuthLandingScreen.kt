package com.simulatedtez.gochat.view.redesign

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AuthLandingScreen(
    onSignInClicked: () -> Unit,
    onSignInWithGoogleClicked: () -> Unit
) {
    val c = GoChatTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.surfacePage)
            .systemBarsPadding()
    ) {
        // Placeholder logo — centered on the screen
        Icon(
            imageVector = Icons.Outlined.Chat,
            contentDescription = "GoChat",
            modifier = Modifier
                .size(96.dp)
                .align(Alignment.Center),
            tint = c.primaryBlue
        )

        // Bottom section: buttons + Terms of Service
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Secondary action — outlined, white fill
            OutlinedButton(
                onClick = onSignInWithGoogleClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(RadiusButton),
                border = BorderStroke(1.5.dp, c.surfaceBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = c.surfaceCard,
                    contentColor = c.textPrimary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // "G" stands in for the Google logo until the real asset is added
                    Text(
                        text = "G",
                        style = UiLabelStyle.copy(
                            fontSize = UiLabelStyle.fontSize * 1.2f,
                            fontWeight = FontWeight.Bold,
                            color = c.primaryBlue
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sign in with Google",
                        style = UiLabelStyle.copy(color = c.textPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary CTA — filled with brand blue
            Button(
                onClick = onSignInClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(RadiusButton),
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.primaryBlue,
                    contentColor = TextInverse
                )
            ) {
                Text(
                    text = "Sign in",
                    style = UiLabelStyle.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextInverse
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Terms of Service — two-tone annotated string
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = c.textMuted)) {
                        append("By continuing you agree to the ")
                    }
                    withStyle(SpanStyle(color = c.textPrimary, fontWeight = FontWeight.SemiBold)) {
                        append("Terms of Service")
                    }
                    withStyle(SpanStyle(color = c.textMuted)) {
                        append(" and\n")
                    }
                    withStyle(SpanStyle(color = c.textPrimary, fontWeight = FontWeight.SemiBold)) {
                        append("Privacy Policy")
                    }
                },
                style = CaptionStyle,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthLandingPreview() {
    AuthLandingScreen(
        onSignInClicked = {},
        onSignInWithGoogleClicked = {}
    )
}

@Preview(showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AuthLandingDarkPreview() {
    AuthLandingScreen(
        onSignInClicked = {},
        onSignInWithGoogleClicked = {}
    )
}
