package com.salesforce.salesforcemobilesdk_authmation_dashboard

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.salesforce.salesforcemobilesdk_authmation_dashboard.manager.SettingsManager

fun main() = application {
    val savedWidth = SettingsManager.getWindowWidth() ?: 1400
    val savedHeight = SettingsManager.getWindowHeight() ?: 900
    
    val windowState = rememberWindowState(
        size = DpSize(savedWidth.dp, savedHeight.dp)
    )
    
    Window(
        onCloseRequest = {
            SettingsManager.saveWindowSize(
                windowState.size.width.value.toInt(),
                windowState.size.height.value.toInt()
            )
            exitApplication()
        },
        title = "SalesforceMobileSDKAuthmationDashboard",
        state = windowState
    ) {
        App()
    }
}