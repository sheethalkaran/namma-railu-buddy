package com.nammarailu.buddy.data.local

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {
    /**
     * Switch the app language at runtime.
     * Requires MainActivity to extend AppCompatActivity.
     * Android handles Activity recreation + persists locale across restarts automatically.
     */
    fun applyLocale(languageCode: String) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageCode)
        )
    }
}
