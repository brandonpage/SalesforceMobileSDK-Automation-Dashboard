package com.salesforce.salesforcemobilesdk_authmation_dashboard.manager

import java.io.File
import java.util.Properties

actual object SettingsManager {
    private val settingsFile = File(System.getProperty("user.home"), ".salesforce_automation_dashboard_settings")

    actual fun saveThemeMode(isDark: Boolean) {
        val props = Properties()
        if (settingsFile.exists()) {
            try {
                settingsFile.inputStream().use { props.load(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        props.setProperty("is_dark_mode", isDark.toString())
        try {
            settingsFile.outputStream().use { props.store(it, "Dashboard Settings") }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun getThemeMode(): Boolean? {
        if (!settingsFile.exists()) return null
        val props = Properties()
        return try {
            settingsFile.inputStream().use { props.load(it) }
            props.getProperty("is_dark_mode")?.toBoolean()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun saveWindowSize(width: Int, height: Int) {
        val props = Properties()
        if (settingsFile.exists()) {
            try {
                settingsFile.inputStream().use { props.load(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        props.setProperty("window_width", width.toString())
        props.setProperty("window_height", height.toString())
        try {
            settingsFile.outputStream().use { props.store(it, "Dashboard Settings") }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun getWindowWidth(): Int? {
        if (!settingsFile.exists()) return null
        val props = Properties()
        return try {
            settingsFile.inputStream().use { props.load(it) }
            props.getProperty("window_width")?.toIntOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun getWindowHeight(): Int? {
        if (!settingsFile.exists()) return null
        val props = Properties()
        return try {
            settingsFile.inputStream().use { props.load(it) }
            props.getProperty("window_height")?.toIntOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
