package com.anubhav.diprep.util

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList

/**
 * Per-app language switching.
 *
 * Uses the framework [LocaleManager] (API 33+, always available at this app's
 * minSdk 35). CLAUDE.md suggested AppCompatDelegate.setApplicationLocales();
 * LocaleManager is the same mechanism AppCompat delegates to on modern Android,
 * without pulling in the androidx.appcompat dependency. Setting the locale here
 * triggers an automatic activity recreation, so the UI updates with no manual
 * app restart.
 */
object LocaleHelper {

    const val LANG_ENGLISH = "en"
    const val LANG_HINDI = "hi"

    fun currentTag(context: Context): String {
        val lm = context.getSystemService(LocaleManager::class.java) ?: return LANG_ENGLISH
        val locales = lm.applicationLocales
        if (locales.isEmpty) return LANG_ENGLISH
        return if (locales[0].language == LANG_HINDI) LANG_HINDI else LANG_ENGLISH
    }

    fun setLanguage(context: Context, tag: String) {
        val lm = context.getSystemService(LocaleManager::class.java) ?: return
        val normalized = if (tag == LANG_HINDI) LANG_HINDI else LANG_ENGLISH
        lm.applicationLocales = LocaleList.forLanguageTags(normalized)
    }
}
