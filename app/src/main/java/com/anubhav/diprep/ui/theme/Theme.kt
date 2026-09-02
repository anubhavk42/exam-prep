package com.anubhav.diprep.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightPremiumColorScheme = lightColorScheme(
    primary = Gold,
    onPrimary = OnAccent,
    primaryContainer = Color(0xFFF5EDD5),
    onPrimaryContainer = Color(0xFF1A1715),

    secondary = Gold,
    onSecondary = OnAccent,
    secondaryContainer = Color(0xFFF5EDD5),
    onSecondaryContainer = Color(0xFF1A1715),

    tertiary = Gold,
    onTertiary = OnAccent,
    tertiaryContainer = Color(0xFFF5EDD5),
    onTertiaryContainer = Color(0xFF1A1715),

    background = Color(0xFFFAF8F5),
    onBackground = Color(0xFF1A1715),

    surface = Color(0xFFF0EDE8),
    onSurface = Color(0xFF1A1715),
    surfaceVariant = Color(0xFFE0DDD8),
    onSurfaceVariant = Color(0xFF6B645F),
    surfaceContainerLowest = Color(0xFFFAF8F5),
    surfaceContainerLow = Color(0xFFF5F2ED),
    surfaceContainer = Color(0xFFF0EDE8),
    surfaceContainerHigh = Color(0xFFE8E4DF),
    surfaceContainerHighest = Color(0xFFD4CFC9),

    outline = Color(0xFFD4CFC9),
    outlineVariant = Color(0xFFE0DDD8),

    error = DangerCoral,
    onError = Color(0xFFFAF8F5),
    errorContainer = Color(0xFFFFE5DC),
    onErrorContainer = Color(0xFF1A1715),

    inverseSurface = Color(0xFF1A1715),
    inverseOnSurface = Color(0xFFFAF8F5),
    inversePrimary = Gold,
)

/*
 * Dark Premium is the primary design direction. Light mode is available
 * via the themeMode preference ("LIGHT" / "DARK" / "SYSTEM").
 */
private val DarkPremiumColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = OnAccent,
    primaryContainer = DarkSurface,
    onPrimaryContainer = TextPrimary,

    secondary = Gold,
    onSecondary = OnAccent,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = TextPrimary,

    tertiary = Gold,
    onTertiary = OnAccent,
    tertiaryContainer = DarkSurface,
    onTertiaryContainer = TextPrimary,

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkBorder,
    onSurfaceVariant = TextMuted,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkBackground,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkBorder,
    surfaceContainerHighest = TrackDisabled,

    outline = DarkBorder,
    outlineVariant = DarkDivider,

    error = DangerCoral,
    onError = OnAccent,
    errorContainer = WeakAlertBg,
    onErrorContainer = TextPrimary,

    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = Gold,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkPremiumColorScheme else LightPremiumColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
