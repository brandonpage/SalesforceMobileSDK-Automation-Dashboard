package com.salesforce.salesforcemobilesdk_authmation_dashboard.manager

import platform.Foundation.NSUserDefaults

actual object SettingsManager {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun saveThemeMode(isDark: Boolean) {
        defaults.setBool(isDark, forKey = "is_dark_mode")
    }

    actual fun getThemeMode(): Boolean? {
        // NSUserDefaults boolForKey returns NO (false) if key doesn't exist, 
        // so we need a way to check existence or just accept default.
        // However, objectForKey returns null if not present.
        return if (defaults.objectForKey("is_dark_mode") != null) {
            defaults.boolForKey("is_dark_mode")
        } else {
            null
        }
    }
}
