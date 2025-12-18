package com.salesforce.salesforcemobilesdk_authmation_dashboard.manager

import java.io.File
import java.util.Properties

actual object TokenManager {
    private val tokenFile = File(System.getProperty("user.home"), ".salesforce_automation_dashboard_token")

    actual fun saveToken(token: String) {
        tokenFile.writeText(token.trim())
    }

    actual fun getToken(): String? {
        return if (tokenFile.exists()) {
            tokenFile.readText().trim().takeIf { it.isNotEmpty() }
        } else {
            null
        }
    }

    actual fun clearToken() {
        if (tokenFile.exists()) {
            tokenFile.delete()
        }
    }
}
