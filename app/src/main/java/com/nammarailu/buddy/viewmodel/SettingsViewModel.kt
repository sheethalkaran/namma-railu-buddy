package com.nammarailu.buddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nammarailu.buddy.data.local.AppPreferences
import com.nammarailu.buddy.data.local.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val language: String = "en",
    val notificationsEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(private val prefs: AppPreferences) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        prefs.isDarkMode,
        prefs.language,
        prefs.notificationsEnabled
    ) { dark, lang, notif -> SettingsUiState(dark, lang, notif) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setDarkMode(v: Boolean)      = viewModelScope.launch { prefs.setDarkMode(v) }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            prefs.setLanguage(lang)
            // Apply locale immediately — triggers Activity recreation automatically
            LocaleHelper.applyLocale(lang)
        }
    }

    fun setNotifications(v: Boolean) = viewModelScope.launch { prefs.setNotifications(v) }
}
