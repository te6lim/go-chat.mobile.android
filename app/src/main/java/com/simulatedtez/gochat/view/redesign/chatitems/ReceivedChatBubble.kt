package com.simulatedtez.gochat.view.redesign.chatitems

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.simulatedtez.gochat.view.redesign.GoChatTheme
import com.simulatedtez.gochat.view.redesign.MessageBodyStyle
import com.simulatedtez.gochat.view.redesign.RadiusBubble
import com.simulatedtez.gochat.view.redesign.RadiusBubbleTail
import kotlinx.coroutines.delay

private val ReceivedBubbleShape = RoundedCornerShape(
    topStart    = RadiusBubble,
    topEnd      = RadiusBubble,
    bottomEnd   = RadiusBubble,
    bottomStart = RadiusBubbleTail
)

private val BubblePaddingHorizontal = 12.dp
private val BubblePaddingVertical   = 8.dp

private const val CHUNK_FADE_MS  = 300L
private const val CHUNK_PAUSE_MS = 150L
private const val FRAME_MS       = 16L

@Composable
fun ReceivedChatBubble(
    text: String,
    messageId: String = text,
    animateIn: Boolean = false
) {
    val c = GoChatTheme.colors

    val tier = remember(messageId) { classifyRevealTier(text) }

    // Capture animateIn once — immune to later recompositions (e.g. next socket message
    // arriving and shifting which message is "new"). On scroll-off/back the composable
    // is recreated with animateIn=false, so shouldAnimate=false and it renders fully.
    val shouldAnimate = remember(messageId) { animateIn }

    var revealedCharCount  by remember(messageId) { mutableIntStateOf(0) }
    var revealedChunkIndex by remember(messageId) { mutableIntStateOf(0) }
    var currentChunkAlpha  by remember(messageId) { mutableFloatStateOf(0f) }
    var isRevealed         by remember(messageId) { mutableStateOf(!shouldAnimate) }

    LaunchedEffect(messageId) {
        if (!shouldAnimate || isRevealed) return@LaunchedEffect
        when (val t = tier) {

            is MessageRevealTier.Typewriter -> {
                for (i in 1..text.length) {
                    if (isRevealed) break
                    revealedCharCount = i
                    delay(t.delayPerCharMs)
                }
                isRevealed = true
            }

            is MessageRevealTier.ChunkSweep -> {
                val steps = CHUNK_FADE_MS / FRAME_MS
                for (i in t.chunks.indices) {
                    if (isRevealed) break
                    revealedChunkIndex = i
                    currentChunkAlpha  = 0f
                    for (step in 1..steps) {
                        if (isRevealed) break
                        currentChunkAlpha = step.toFloat() / steps
                        delay(FRAME_MS)
                    }
                    if (!isRevealed) {
                        currentChunkAlpha = 1f
                        if (i < t.chunks.lastIndex) delay(CHUNK_PAUSE_MS)
                    }
                }
                isRevealed = true
            }
        }
    }

    Box(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .clip(ReceivedBubbleShape)
            .background(c.surfaceBubbleIn)
            .clickable { isRevealed = true }
            .padding(horizontal = BubblePaddingHorizontal, vertical = BubblePaddingVertical)
    ) {
        when {
            // ChunkSweep — Column of paragraphs fading in one by one.
            // Stays in this branch even after isRevealed (all alphas become 1f),
            // avoiding a layout switch that could cause a visual jump.
            shouldAnimate && tier is MessageRevealTier.ChunkSweep -> {
                Column {
                    tier.chunks.forEachIndexed { index, chunk ->
                        val chunkAlpha = when {
                            isRevealed || index < revealedChunkIndex -> 1f
                            index == revealedChunkIndex              -> currentChunkAlpha
                            else                                     -> 0f
                        }
                        Text(
                            text     = chunk,
                            style    = MessageBodyStyle.copy(color = c.textPrimary),
                            modifier = Modifier.alpha(chunkAlpha)
                        )
                    }
                }
            }

            // Typewriter — full text is always laid out (stable bubble height);
            // unrevealed characters are transparent so they don't shift the layout.
            shouldAnimate && !isRevealed && tier is MessageRevealTier.Typewriter -> {
                Text(
                    text  = buildAnnotatedString {
                        append(text.substring(0, revealedCharCount))
                        withStyle(SpanStyle(color = Color.Transparent)) {
                            append(text.substring(revealedCharCount))
                        }
                    },
                    style = MessageBodyStyle.copy(color = c.textPrimary)
                )
            }

            // Fully revealed or no animation
            else -> {
                Text(
                    text  = text,
                    style = MessageBodyStyle.copy(color = c.textPrimary)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
fun ReceivedChatBubblePreview() {
    ReceivedChatBubble(text = "Hey, how's it going?")
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ReceivedChatBubbleDarkPreview() {
    ReceivedChatBubble(text = "Hey, how's it going?")
}
