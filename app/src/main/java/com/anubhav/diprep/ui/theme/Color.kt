package com.anubhav.diprep.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Dark Premium — the single, final design direction for the whole app.
 * Full dark theme with a warm gold accent. Flat surfaces differentiated
 * by 1dp borders only: no gradients, no shadows.
 */

// Core surfaces
val DarkBackground = Color(0xFF141110)
val DarkSurface = Color(0xFF1E1A17)
val DarkBorder = Color(0xFF2A2422)
val DarkDivider = Color(0xFF211C19)

// Accent + text
val Gold = Color(0xFFE8B869)
val TextPrimary = Color(0xFFF0ECE6)
val TextMuted = Color(0xFF8A8380)
val OnAccent = Color(0xFF141110)

// Semantic performance colors
val SuccessGreen = Color(0xFF5DCAA5)
val WarningAmber = Color(0xFFE8B869)
val DangerCoral = Color(0xFFE8896A)

// Weak-topic alert card
val WeakAlertBg = Color(0xFF251815)
val WeakAlertBorder = Color(0xFF4A2E26)

// Disabled / progress track
val TrackDisabled = Color(0xFF3A332E)

// Bottom nav unselected item
val NavUnselected = Color(0xFF6B645F)

/*
 * Legacy "Sleek" aliases — kept so screens that still reference these names
 * render in the Dark Premium palette without a mechanical rename. New code
 * should use MaterialTheme.colorScheme or the semantic names above.
 */
val SleekPrimary = Gold
val OnSleekPrimary = OnAccent
val SleekPrimaryContainer = DarkSurface
val OnSleekPrimaryContainer = TextPrimary

val SleekSecondary = TextMuted
val OnSleekSecondary = OnAccent
val SleekSecondaryContainer = DarkSurface
val OnSleekSecondaryContainer = TextPrimary

val SleekTertiary = Gold
val OnSleekTertiary = OnAccent
val SleekTertiaryContainer = DarkSurface
val OnSleekTertiaryContainer = TextPrimary

val SleekBackground = DarkBackground
val OnSleekBackground = TextPrimary

val SleekSurface = DarkSurface
val OnSleekSurface = TextPrimary
val SleekSurfaceVariant = DarkBorder
val OnSleekSurfaceVariant = TextMuted

val SleekSurfaceContainer = DarkSurface
val SleekSurfaceContainerHigh = DarkBorder
val SleekSurfaceContainerHighest = TrackDisabled
val SleekSurfaceContainerLowest = DarkBackground
val SleekSurfaceContainerLow = DarkBackground

val SleekOutline = DarkBorder
val SleekOutlineVariant = DarkDivider
val SleekBorderLight = DarkBorder

val SleekError = DangerCoral
val OnSleekError = OnAccent
val SleekErrorContainer = WeakAlertBg
val OnSleekErrorContainer = TextPrimary

val SleekInverseSurface = TextPrimary
val SleekInverseOnSurface = DarkBackground
val SleekInversePrimary = Gold

// Semantic indicators (legacy names) — tinted backgrounds sit on the dark ground.
val SleekSuccess = SuccessGreen
val SleekSuccessLight = SuccessGreen
val SleekSuccessBg = Color(0xFF17251F)

val SleekWarning = WarningAmber
val SleekWarningLight = WarningAmber
val SleekWarningBg = Color(0xFF261E12)

val SleekCritical = DangerCoral
val SleekCriticalLight = DangerCoral
val SleekCriticalBg = WeakAlertBg
