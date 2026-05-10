# Received Message Reveal Animation — Feature Spec

## Overview

Incoming messages (received via the WebSocket, not loaded from history) animate into
view with a typewriter-style reveal effect. The animation is purely a UI illusion — the
full message string arrives from the socket as normal; the reveal happens entirely on
the client side inside `ReceivedChatBubble`.

The goal is delight without wasting the user's time. A length-adaptive strategy ensures
short messages feel snappy and long messages never drag.

---

## Scope

- **Applies to:** received messages only — emissions from the `newMessage` LiveData path.
- **Does not apply to:** messages loaded from `pagedMessages` (history). Those render
  fully revealed from the start.
- **Does not replay:** once a message has finished animating (or been interrupted), it
  stays fully revealed for the lifetime of the composition. It does not re-animate on
  recomposition or when the user scrolls the item off-screen and back.

---

## Triggering the Animation

The chat list renderer passes an `animateIn: Boolean` parameter down to
`ReceivedChatBubble`. The parent sets it:

- `true` — for messages that arrived via `newMessage` (socket delivery).
- `false` — for everything else (history load, sent messages shown in the same list).

`ReceivedChatBubble` treats `animateIn = false` as "start fully revealed; skip all
animation logic entirely."

---

## Length Tiers

Character count determines which animation strategy is used. Suggested thresholds
(tune after testing):

| Tier | Character range | Strategy |
|------|----------------|----------|
| Short | < 120 chars | Typewriter — fixed speed (~18 ms/char) |
| Medium | 120 – 600 chars | Typewriter — speed scaled to finish in ~2.5 s total |
| Long | > 600 chars | Chunk sweep — fade-in by paragraph or word-boundary chunk |

### Short tier
Fixed delay of ~18 ms per character. A 100-char message completes in ~1.8 s.
The base speed acts as a floor — no message in this tier ever animates slower than
18 ms/char regardless of how short it is.

### Medium tier
Total animation duration is capped at ~2.5 s. Character delay is derived as:
`delayPerChar = 2500ms / characterCount`. This keeps the effect alive without
dragging on longer messages in this range.

### Long tier
See *Chunk Sweep* section below.

---

## Typewriter Reveal (Short and Medium)

The bubble reserves its full height upfront (the complete text is rendered but
controlled-invisible below the reveal frontier). As the coroutine increments the
revealed character count, a gradient mask is redrawn.

### Gradient mechanics

The reveal uses a **vertical gradient overlay** drawn over the bubble background,
not over the text. The gradient blends:

- `bubbleColor` (fully opaque) at `startY` — the solid revealed region above.
- `chatBackgroundColor` (fully opaque) at `endY` — the unrevealed region below,
  matching the chat screen background so the bubble appears to dissolve into it.

Using `chatBackgroundColor` rather than `Color.Transparent` is critical. Transparent
would expose the bubble's full background rectangle behind the gradient and break the
illusion. The gradient must match the actual page background:
`SurfacePage` in light mode, `SurfacePageDark` in dark mode — both available via
`GoChatTheme.colors.surfacePage`.

### Gradient positioning

To place the gradient boundary at the correct pixel Y position (rather than a rough
character-fraction estimate), the `Text` composable captures its layout via
`onTextLayout`:

```kotlin
var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

Text(
    text = fullText,
    onTextLayout = { textLayoutResult = it }
)
```

At each animation tick, the last fully-revealed line index is derived from the current
character count using `textLayoutResult.getLineForOffset(revealedCharCount)`. Then
`textLayoutResult.getLineBottom(lineIndex)` gives the exact pixel Y for the gradient
`startY`. The gradient blend zone (`startY` → `endY`) spans approximately one line
height for a smooth fade rather than a hard edge.

---

## Chunk Sweep (Long tier — > 600 chars)

### Splitting into chunks

1. **Prefer paragraph breaks.** Split on `\n\n`. Each paragraph is one chunk.
2. **Fall back to word-boundary chunks.** If the message contains no paragraph breaks
   (or has paragraphs longer than ~300 chars), split into chunks of ~80–100 characters
   at the nearest word boundary (never cut mid-word).

### Rendering

- All chunks are laid out at full height from the start — **no layout shift**.
  Chunks below the current reveal point have `alpha = 0f`; the bubble's total height
  is fixed from frame one.
- Each chunk animates with a localised gradient sweep identical in style to the
  typewriter gradient, but scoped vertically to that chunk's bounds.
- Once a chunk's sweep completes, it snaps to fully opaque and the next chunk begins.
- **Timing per chunk:** ~300 ms fade-in, ~150 ms pause before the next chunk starts.

---

## Interruptibility

A single tap on the bubble (or any interaction that would scroll it) snaps the entire
message to fully revealed instantly, cancelling whatever coroutine is in progress.

Implementation: a `Boolean` flag `isRevealed` (starts `false`, flips to `true` on
interrupt or on animation completion) short-circuits the coroutine. The bubble's
`Modifier.clickable { isRevealed = true }` handles the tap case. The parent can also
flip `animateIn` to `false` if the message scrolls off-screen, though snapping on
recomposition (via the no-replay rule) effectively handles that case already.

---

## State Management Inside `ReceivedChatBubble`

All animation state lives inside the composable. No state leaks to the parent beyond
the `animateIn: Boolean` input parameter.

Key state:
- `revealedCharCount: Int` — driven by the animation coroutine (typewriter tiers).
- `revealedChunkIndex: Int` — driven by the animation coroutine (long tier).
- `isRevealed: Boolean` — `true` once fully revealed or interrupted; gates the
  coroutine and short-circuits all gradient drawing.
- `textLayoutResult: TextLayoutResult?` — captured via `onTextLayout`, used for
  pixel-accurate gradient positioning.

The `LaunchedEffect` that drives the coroutine is keyed on the message ID (or the
full text string if an ID is not available at the bubble level). This ensures the
effect does not re-trigger on recomposition.

---

## Dark Mode

The gradient's background blend color must always match the current chat screen
background:

```kotlin
val blendColor = GoChatTheme.colors.surfacePage
```

This is already available at bubble render time since `GoChatTheme.colors` is a
`@Composable` property.

---

## Known Edge Cases / Future Iterations

- **Gradient near the input bar.** If the bubble sits at the very bottom of the
  visible area, the blend color behind it may technically be the input bar surface
  rather than `surfacePage`. For the initial implementation, ignore this and use
  `surfacePage` consistently. Iterate after real-device testing.
- **Very short single-word messages.** At 18 ms/char these complete in well under a
  second. Consider a minimum animation duration of ~400 ms so the effect is at least
  perceptible.
- **Emoji and multi-codepoint characters.** Character count should operate on
  `String.length` (UTF-16 code units) for simplicity in the initial pass, since
  `TextLayoutResult` offsets are also in code units.
- **Messages arriving while the keyboard is open.** The bubble may be partially
  occluded. The animation still runs; interruptibility handles the case where the user
  dismisses the keyboard mid-animation.

---

## Implementation Parts

Discrete units of work needed to build this feature end-to-end.

| # | Part | Description |
|---|------|-------------|
| 1 | ~~`animateIn` parameter~~ | ~~Add `animateIn: Boolean` to `ReceivedChatBubble`. When `false`, render fully revealed with zero animation logic.~~ ✅ |
| 2 | ~~Tier classifier~~ | ~~Pure function: takes the message string, returns `Short` / `Medium` / `Long` based on character count.~~ ✅ |
| 3 | ~~State block~~ | ~~`revealedCharCount`, `revealedChunkIndex`, `isRevealed`, `textLayoutResult` — all scoped inside the composable.~~ ✅ |
| 4 | ~~`onTextLayout` capture~~ | ~~Wire `onTextLayout = { textLayoutResult = it }` on the `Text` composable so pixel positions are available to the gradient.~~ ✅ |
| 5 | ~~Short/Medium typewriter coroutine~~ | ~~`LaunchedEffect` keyed on message ID that increments `revealedCharCount` with tier-appropriate delay — 18 ms fixed for Short, `2500 / length` ms for Medium.~~ ✅ |
| 6 | ~~Gradient overlay~~ | ~~`Brush.verticalGradient` drawn over the bubble background. Uses `getLineForOffset(revealedCharCount)` → `getLineBottom(lineIndex)` to place the fade boundary at the correct pixel Y. Blends from `bubbleColor` down to `GoChatTheme.colors.surfacePage`.~~ ✅ |
| 7 | ~~Chunk splitter~~ | ~~Pure function: tries `\n\n` paragraph split first; falls back to word-boundary chunks of ~80–100 chars if no paragraph breaks (or if any paragraph exceeds ~300 chars).~~ ✅ |
| 8 | ~~Long-tier chunk sweep coroutine~~ | ~~`LaunchedEffect` that iterates `revealedChunkIndex`, fading each chunk in over ~300 ms (animated alpha), pausing ~150 ms before advancing. All chunk text is laid out at `alpha = 0f` from frame one to lock in the bubble height.~~ ✅ |
| 9 | ~~Interruptibility~~ | ~~`Modifier.clickable { isRevealed = true }` on the bubble. Both coroutines check `isRevealed` each iteration and exit early. Snaps everything to fully visible.~~ ✅ |
| 10 | ~~No-replay guard~~ | ~~`LaunchedEffect` key is the message ID (or full text if ID is unavailable). Ensures no re-animation on recomposition or scroll-off/scroll-back.~~ ✅ |
| 11 | ~~`animateIn` wiring in the chat list~~ | ~~In `ChatContent`, pass `animateIn = true` only for messages that arrived via `newMessage` LiveData — `false` for everything from `pagedMessages`. Requires a flag on the list item or a separate set tracking new-arrival IDs.~~ ✅ |

Parts 1–6 + 9–10 cover Short and Medium tiers end-to-end. Parts 7–8 add the Long tier on top. Part 11 is the parent-level wiring.
