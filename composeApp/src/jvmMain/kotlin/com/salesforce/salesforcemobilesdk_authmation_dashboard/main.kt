package com.salesforce.salesforcemobilesdk_authmation_dashboard

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SalesforceMobileSDKAuthmationDashboard",
    ) {
        App()
    }
}