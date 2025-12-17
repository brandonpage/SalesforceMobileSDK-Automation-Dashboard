package com.salesforce.salesforcemobilesdk_authmation_dashboard.manager

expect object SettingsManager {
    fun saveThemeMode(isDark: Boolean)
    fun getThemeMode(): Boolean?
}
