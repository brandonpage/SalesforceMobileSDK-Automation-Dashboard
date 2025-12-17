package com.salesforce.salesforcemobilesdk_authmation_dashboard.manager

import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.DashboardState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

actual object StateManager {
    private val stateFile = File(System.getProperty("user.home"), ".salesforce_automation_dashboard_state.json")
    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
        encodeDefaults = true
    }

    actual fun saveState(state: DashboardState) {
        try {
            val jsonString = json.encodeToString(state)
            stateFile.writeText(jsonString)
        } catch (e: Exception) {
            println("Failed to save state: ${e.message}")
        }
    }

    actual fun loadState(): DashboardState? {
        return try {
            if (stateFile.exists()) {
                val jsonString = stateFile.readText()
                if (jsonString.isNotBlank()) {
                    json.decodeFromString<DashboardState>(jsonString)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            println("Failed to load state: ${e.message}")
            null
        }
    }
}
