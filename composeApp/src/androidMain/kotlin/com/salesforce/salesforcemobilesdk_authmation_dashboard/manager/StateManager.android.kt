package com.salesforce.salesforcemobilesdk_authmation_dashboard.manager

import android.content.Context
import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.DashboardState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

actual object StateManager {
    private var context: Context? = null
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    fun init(ctx: Context) {
        context = ctx.applicationContext
    }

    actual fun saveState(state: DashboardState) {
        val ctx = context ?: return
        try {
            val jsonString = json.encodeToString(state)
            val file = File(ctx.filesDir, "dashboard_state.json")
            file.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun loadState(): DashboardState? {
        val ctx = context ?: return null
        return try {
            val file = File(ctx.filesDir, "dashboard_state.json")
            if (file.exists()) {
                val jsonString = file.readText()
                if (jsonString.isNotBlank()) {
                    json.decodeFromString<DashboardState>(jsonString)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual fun clearState() {
        val ctx = context ?: return
        try {
            val file = File(ctx.filesDir, "dashboard_state.json")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
