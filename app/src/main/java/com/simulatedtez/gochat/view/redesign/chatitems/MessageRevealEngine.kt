package com.simulatedtez.gochat.view.redesign.chatitems

private const val SHORT_TIER_MAX_CHARS = 120
private const val LONG_TIER_MIN_CHARS = 600
private const val SHORT_DELAY_MS = 18L
private const val MEDIUM_TOTAL_DURATION_MS = 2500L
private const val MAX_PARAGRAPH_CHARS = 300
private const val WORD_CHUNK_TARGET_CHARS = 90

/**
 * Typewriter: reveal one character at a time with [delayPerCharMs] between each step.
 * ChunkSweep: fade in pre-split [chunks] sequentially (used for long messages).
 */
sealed class MessageRevealTier {
    data class Typewriter(val delayPerCharMs: Long) : MessageRevealTier()
    data class ChunkSweep(val chunks: List<String>) : MessageRevealTier()
}

/**
 * Classifies [text] into a reveal tier and bakes in all timing/chunk parameters.
 * Compose only needs to consume the result — no further calculation required there.
 */
fun classifyRevealTier(text: String): MessageRevealTier {
    val length = text.length
    return when {
        length < SHORT_TIER_MAX_CHARS -> {
            MessageRevealTier.Typewriter(delayPerCharMs = SHORT_DELAY_MS)
        }
        length <= LONG_TIER_MIN_CHARS -> {
            val delay = (MEDIUM_TOTAL_DURATION_MS / length).coerceAtLeast(1L)
            MessageRevealTier.Typewriter(delayPerCharMs = delay)
        }
        else -> {
            MessageRevealTier.ChunkSweep(chunks = splitIntoChunks(text))
        }
    }
}

private fun splitIntoChunks(text: String): List<String> {
    val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
    if (paragraphs.size > 1 && paragraphs.all { it.length <= MAX_PARAGRAPH_CHARS }) {
        return paragraphs
    }
    return splitByWordBoundary(text)
}

private fun splitByWordBoundary(text: String): List<String> {
    val chunks = mutableListOf<String>()
    var start = 0
    while (start < text.length) {
        val tentativeEnd = (start + WORD_CHUNK_TARGET_CHARS).coerceAtMost(text.length)
        if (tentativeEnd == text.length) {
            chunks.add(text.substring(start).trim())
            break
        }
        // Walk back to the nearest space to avoid cutting mid-word
        var boundary = tentativeEnd
        while (boundary > start && text[boundary - 1] != ' ') {
            boundary--
        }
        if (boundary == start) boundary = tentativeEnd // no space found; hard cut
        chunks.add(text.substring(start, boundary).trim())
        start = boundary
    }
    return chunks.filter { it.isNotEmpty() }
}
