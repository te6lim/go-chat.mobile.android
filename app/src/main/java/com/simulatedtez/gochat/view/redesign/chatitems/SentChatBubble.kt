package com.simulatedtez.gochat.view.redesign.chatitems

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simulatedtez.gochat.view.redesign.CaptionStyle
import com.simulatedtez.gochat.view.redesign.GoChatTheme
import com.simulatedtez.gochat.view.redesign.MessageBodyStyle
import com.simulatedtez.gochat.view.redesign.RadiusBubble
import com.simulatedtez.gochat.view.redesign.RadiusBubbleTail
import com.simulatedtez.gochat.view.redesign.TealAccent
import com.simulatedtez.gochat.view.redesign.TextInverse

private val SentBubbleShape = RoundedCornerShape(
    topStart    = RadiusBubble,
    topEnd      = RadiusBubble,
    bottomEnd   = RadiusBubbleTail,
    bottomStart = RadiusBubble
)

@Composable
fun SentChatBubble(
    text: String,
    status: MessageStatus? = null
) {
    val c = GoChatTheme.colors
    Column(
        modifier = Modifier.widthIn(max = 260.dp),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .background(c.primaryBlue, SentBubbleShape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = text, style = MessageBodyStyle.copy(color = TextInverse))
        }

        if (status != null) {
            Text(
                text = when (status) {
                    MessageStatus.DELIVERED -> "Delivered"
                    MessageStatus.SEEN      -> "Seen"
                },
                style = when (status) {
                    MessageStatus.DELIVERED -> CaptionStyle.copy(
                        color      = c.textMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    MessageStatus.SEEN      -> CaptionStyle.copy(
                        color      = TealAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                modifier = Modifier.padding(top = 2.dp, end = 2.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
fun SentBubbleDeliveredPreview() {
    SentChatBubble(text = "On my way!", status = MessageStatus.DELIVERED)
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SentBubbleDeliveredDarkPreview() {
    SentChatBubble(text = "On my way!", status = MessageStatus.DELIVERED)
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
fun SentBubbleSeenPreview() {
    SentChatBubble(text = "Did you get my message?", status = MessageStatus.SEEN)
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SentBubbleSeenDarkPreview() {
    SentChatBubble(text = "Did you get my message?", status = MessageStatus.SEEN)
}
