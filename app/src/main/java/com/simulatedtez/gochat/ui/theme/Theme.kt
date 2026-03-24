package com.simulatedtez.gochat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Slate900,
    secondary = Cyan600,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Slate800,
    tertiary = Violet600,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Slate900,
    error = Red500,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    outline = Slate300,
    outlineVariant = Slate200,
    inverseSurface = Slate800,
    inverseOnSurface = Slate50,
    inversePrimary = Blue400,
    surfaceTint = Blue600,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue400,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Blue100,
    secondary = Color(0xFF22D3EE),
    onSecondary = Slate900,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4C1D95),
    onTertiaryContainer = Color(0xFFEDE9FE),
    error = Color(0xFFF87171),
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate400,
    outline = Slate600,
    outlineVariant = Slate700,
    inverseSurface = Slate100,
    inverseOnSurface = Slate800,
    inversePrimary = Blue600,
    surfaceTint = Blue400,
)

@Composable
fun GoChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
