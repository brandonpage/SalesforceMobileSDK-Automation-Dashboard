package com.salesforce.salesforcemobilesdk_authmation_dashboard.manager

import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.DashboardState

actual object StateManager {
    actual fun saveState(state: DashboardState) {
        // TODO: Implement iOS state persistence (e.g. NSUserDefaults)
        println("StateManager.saveState not implemented for iOS")
    }

    actual fun loadState(): DashboardState? {
        // TODO: Implement iOS state persistence
        println("StateManager.loadState not implemented for iOS")
        return null
    }
}
