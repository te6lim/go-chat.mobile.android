package com.simulatedtez.gochat.view.redesign

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simulatedtez.gochat.R

// ── Brand Colors ──────────────────────────────────────────────────────────────

val PrimaryBlue      = Color(0xFF0284C7)  // Primary CTA buttons, sent bubbles, links
val PrimaryBlueDark  = Color(0xFF0EA5E9)  // Primary in dark mode
val TealAccent       = Color(0xFF0D9488)  // Username highlights, online dot, secondary actions
val TealSoft         = Color(0xFFCCFBF1)  // Teal avatar backgrounds, mentions, reaction chips

// ── Light Mode Surfaces ───────────────────────────────────────────────────────

val SurfacePage      = Color(0xFFF8FAFC)  // App-level background
val SurfaceCard      = Color(0xFFFFFFFF)  // Sheets, modals, cards
val SurfaceBubbleIn  = Color(0xFFF1F5F9)  // Incoming message bubble
val SurfaceBorder    = Color(0xFFE2E8F0)  // Dividers, input outlines

// ── Dark Mode Surfaces ────────────────────────────────────────────────────────

val SurfacePageDark     = Color(0xFF0F172A)
val SurfaceCardDark     = Color(0xFF1E293B)
val SurfaceBubbleInDark = Color(0xFF334155)
val SurfaceBorderDark   = Color(0xFF475569)

// ── Text Colors ───────────────────────────────────────────────────────────────

val TextPrimary   = Color(0xFF0F172A)  // Message body, main labels
val TextSecondary = Color(0xFF475569)  // Timestamps, subtitles, helper text
val TextMuted     = Color(0xFF94A3B8)  // Placeholders, disabled states
val TextInverse   = Color(0xFFFFFFFF)  // Text inside blue/teal buttons or bubbles

// Dark mode text
val TextPrimaryDark   = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark     = Color(0xFF475569)

// ── Semantic / Status Colors ──────────────────────────────────────────────────

val ColorOnline  = Color(0xFF10B981)  // Online indicator, delivered confirmation
val ColorAway    = Color(0xFFF59E0B)  // Away status, soft warnings
val ColorError   = Color(0xFFEF4444)  // Delete, failed message, error states
val ColorMention = Color(0xFF8B5CF6)  // @ mention highlight, unread badge

// ── Message Bubble Colors ─────────────────────────────────────────────────────

val BubbleSentLight     = PrimaryBlue        // #0284C7
val BubbleSentDark      = PrimaryBlueDark    // #0EA5E9
val BubbleReceivedLight = SurfaceBubbleIn    // #F1F5F9
val BubbleReceivedDark  = SurfaceBubbleInDark // #334155

// ── Corner Radii ──────────────────────────────────────────────────────────────

val RadiusBubble     = 16.dp  // Message bubble — friendly, rounded
val RadiusBubbleTail = 4.dp   // Tail corner — directional pointer
val RadiusAvatar     = 999.dp // Full circle (use with clip)
val RadiusInput      = 12.dp  // Input fields
val RadiusButton     = 12.dp  // Primary buttons (matches input)
val RadiusPill       = 999.dp // Status badges, reaction chips
val RadiusSheet      = 16.dp  // Top corners of bottom sheet / modal

// ── Typography ────────────────────────────────────────────────────────────────
// Both are variable fonts — a single file covers all weights via the wght axis.
// Listing each weight we use tells Compose how to map FontWeight values to the
// variable axis; the runtime picks the exact intermediate value automatically.

@OptIn(ExperimentalTextApi::class)
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800)))
)

@OptIn(ExperimentalTextApi::class)
val Inter = FontFamily(
    Font(R.font.inter, FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter, FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter, FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)))
)

val FontHeading = PlusJakartaSans  // Display, headings, usernames, group names
val FontBody    = Inter            // Messages, timestamps, buttons, input fields

// Type scale — sizes in sp, line heights derived from spec multipliers

val ScreenTitleStyle = TextStyle(
    fontFamily = FontHeading,
    fontSize = 22.sp,
    fontWeight = FontWeight.Bold,        // 700
    lineHeight = 26.sp,                  // 22 × 1.2
    letterSpacing = (-0.02 * 22).sp
)

val SectionHeaderStyle = TextStyle(
    fontFamily = FontHeading,
    fontSize = 18.sp,
    fontWeight = FontWeight.SemiBold,    // 600
    lineHeight = 23.sp,                  // 18 × 1.3
    letterSpacing = (-0.02 * 18).sp
)

val UsernameStyle = TextStyle(
    fontFamily = FontHeading,
    fontSize = 14.sp,
    fontWeight = FontWeight.SemiBold,    // 600
    lineHeight = 20.sp                   // 14 × 1.4
)

val MessageBodyStyle = TextStyle(
    fontFamily = FontBody,
    fontSize = 15.sp,
    fontWeight = FontWeight.Normal,      // 400
    lineHeight = 24.sp                   // 15 × 1.6
)

val UiLabelStyle = TextStyle(
    fontFamily = FontBody,
    fontSize = 13.sp,
    fontWeight = FontWeight.Medium,      // 500
    lineHeight = 18.sp                   // 13 × 1.4
)

val TimestampStyle = TextStyle(
    fontFamily = FontBody,
    fontSize = 11.sp,
    fontWeight = FontWeight.Normal,      // 400
    lineHeight = 13.sp                   // 11 × 1.2
)

val CaptionStyle = TextStyle(
    fontFamily = FontBody,
    fontSize = 11.sp,
    fontWeight = FontWeight.Normal,      // 400
    lineHeight = 14.sp                   // 11 × 1.3
)

// ── Theme ──────────────────────────────────────────────────────────────────────
// Holds the colors that differ between light and dark mode.
// Constant tokens (TealAccent, ColorOnline, ColorAway, ColorError, TextInverse)
// are the same in both modes and can be referenced directly.

data class GoChatColors(
    val surfacePage: Color,
    val surfaceCard: Color,
    val surfaceBubbleIn: Color,
    val surfaceBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primaryBlue: Color,
)

private val LightGoChatColors = GoChatColors(
    surfacePage     = SurfacePage,
    surfaceCard     = SurfaceCard,
    surfaceBubbleIn = SurfaceBubbleIn,
    surfaceBorder   = SurfaceBorder,
    textPrimary     = TextPrimary,
    textSecondary   = TextSecondary,
    textMuted       = TextMuted,
    primaryBlue     = PrimaryBlue,
)

private val DarkGoChatColors = GoChatColors(
    surfacePage     = SurfacePageDark,
    surfaceCard     = SurfaceCardDark,
    surfaceBubbleIn = SurfaceBubbleInDark,
    surfaceBorder   = SurfaceBorderDark,
    textPrimary     = TextPrimaryDark,
    textSecondary   = TextSecondaryDark,
    textMuted       = TextMutedDark,
    primaryBlue     = PrimaryBlueDark,
)

object GoChatTheme {
    val colors: GoChatColors
        @Composable get() = if (isSystemInDarkTheme()) DarkGoChatColors else LightGoChatColors
}
