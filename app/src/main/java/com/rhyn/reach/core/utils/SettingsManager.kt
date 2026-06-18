package com.rhyn.reach.core.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SettingsManager {
    private const val PREFS_NAME = "reach_settings"
    private const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_ONBOARDING = "has_seen_onboarding"

    private val _darkThemeFlow = MutableStateFlow<Boolean?>(null)
    val darkThemeFlow: StateFlow<Boolean?> = _darkThemeFlow

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun init(context: Context) {
        if (_darkThemeFlow.value == null) {
            _darkThemeFlow.value = getPrefs(context).getBoolean(KEY_DARK_THEME, false)
        }
    }

    fun isDarkTheme(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DARK_THEME, false)
    }

    fun setDarkTheme(context: Context, isDark: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DARK_THEME, isDark).apply()
        _darkThemeFlow.value = isDark
    }

    fun hasSeenOnboarding(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING, false)
    }

    fun setHasSeenOnboarding(context: Context, hasSeen: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING, hasSeen).apply()
    }
}

