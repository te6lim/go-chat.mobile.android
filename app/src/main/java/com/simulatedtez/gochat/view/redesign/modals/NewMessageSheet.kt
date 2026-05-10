package com.simulatedtez.gochat.view.redesign.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simulatedtez.gochat.view.redesign.GoChatTheme
import com.simulatedtez.gochat.view.redesign.MessageBodyStyle
import com.simulatedtez.gochat.view.redesign.RadiusButton
import com.simulatedtez.gochat.view.redesign.RadiusPill
import com.simulatedtez.gochat.view.redesign.SectionHeaderStyle
import com.simulatedtez.gochat.view.redesign.TextInverse
import com.simulatedtez.gochat.view.redesign.UiLabelStyle

@Composable
fun NewMessageSheet(
    recipientInput: String,
    onRecipientChange: (String) -> Unit,
    isWaiting: Boolean,
    onStartChat: () -> Unit,
    onDismiss: () -> Unit
) {
    val c = GoChatTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            Text(
                text = "New Chat",
                style = SectionHeaderStyle.copy(
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary
                ),
                modifier = Modifier.align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(c.surfaceBubbleIn)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = c.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RadiusPill))
                .background(c.surfaceBubbleIn)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "To:", style = UiLabelStyle.copy(color = c.textMuted))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = recipientInput,
                onValueChange = onRecipientChange,
                singleLine = true,
                textStyle = MessageBodyStyle.copy(color = c.textPrimary),
                cursorBrush = SolidColor(c.primaryBlue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onStartChat() }),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onStartChat,
            enabled = recipientInput.isNotBlank() && !isWaiting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(RadiusButton),
            colors = ButtonDefaults.buttonColors(
                containerColor = c.primaryBlue,
                contentColor = TextInverse,
                disabledContainerColor = c.surfaceBorder,
                disabledContentColor = c.textMuted
            )
        ) {
            Text(
                text = if (isWaiting) "Starting chat…" else "Start chat",
                style = UiLabelStyle.copy(fontWeight = FontWeight.SemiBold, color = TextInverse)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NewMessageSheetPreview() {
    NewMessageSheet(
        recipientInput = "",
        onRecipientChange = {},
        isWaiting = false,
        onStartChat = {},
        onDismiss = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1E293B,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NewMessageSheetDarkPreview() {
    NewMessageSheet(
        recipientInput = "",
        onRecipientChange = {},
        isWaiting = false,
        onStartChat = {},
        onDismiss = {}
    )
}
