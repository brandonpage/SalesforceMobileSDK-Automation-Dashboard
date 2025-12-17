package com.salesforce.salesforcemobilesdk_authmation_dashboard.manager

import android.content.Context
import android.content.SharedPreferences

actual object SettingsManager {
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("com.salesforce.authmation_dashboard_settings", Context.MODE_PRIVATE)
    }

    actual fun saveThemeMode(isDark: Boolean) {
        sharedPreferences?.edit()?.putBoolean("is_dark_mode", isDark)?.apply()
    }

    actual fun getThemeMode(): Boolean? {
        return if (sharedPreferences?.contains("is_dark_mode") == true) {
            sharedPreferences?.getBoolean("is_dark_mode", false)
        } else {
            null
        }
    }

    actual fun saveWindowSize(width: Int, height: Int) {
        // Not applicable for Android
    }

    actual fun getWindowWidth(): Int? = null

    actual fun getWindowHeight(): Int? = null
}
