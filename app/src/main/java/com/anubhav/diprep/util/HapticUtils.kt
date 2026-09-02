package com.anubhav.diprep.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * App-wide haptic feedback — zero crash risk.
 *
 * Rules (see CLAUDE.md "App-wide Haptic Feedback"):
 *  - ONLY Compose's [LocalHapticFeedback]. No android.os.Vibrator, no permission.
 *  - EVERY call is wrapped in try/catch and swallows all throwables. A haptic
 *    failure never crashes the app and never blocks the real click action —
 *    callers run their own logic separately from calling [SafeHaptic].
 *  - ONE centralized entry point: every screen calls [rememberSafeHaptic].
 *  - Master switch: when [LocalHapticEnabled] is false the utility does nothing;
 *    no haptic call fires at all.
 */

/** Set once at the app root (MainActivity) from UserProfile.hapticFeedbackEnabled. */
val LocalHapticEnabled = staticCompositionLocalOf { true }

class SafeHaptic internal constructor(
    private val haptic: HapticFeedback,
    private val enabled: Boolean
) {
    /** Standard tap feedback — used for every button, checkbox, toggle, chip, nav tap. */
    fun tap() = perform(HapticFeedbackType.LongPress)

    /**
     * Milestone / confetti moment. Kept on the same LongPress type for consistency;
     * a short double pulse reads as slightly stronger without a new pattern.
     */
    fun celebrate() {
        perform(HapticFeedbackType.LongPress)
        perform(HapticFeedbackType.LongPress)
    }

    private fun perform(type: HapticFeedbackType) {
        if (!enabled) return
        try {
            haptic.performHapticFeedback(type)
        } catch (_: Throwable) {
            // Silently ignore — a haptic failure must never surface to the user.
        }
    }
}

/**
 * The single helper every screen uses:
 *
 *   val haptic = rememberSafeHaptic()
 *   Button(onClick = { haptic.tap(); doTheThing() }) { ... }
 */
@Composable
fun rememberSafeHaptic(): SafeHaptic {
    val haptic = LocalHapticFeedback.current
    val enabled = LocalHapticEnabled.current
    return remember(haptic, enabled) { SafeHaptic(haptic, enabled) }
}
