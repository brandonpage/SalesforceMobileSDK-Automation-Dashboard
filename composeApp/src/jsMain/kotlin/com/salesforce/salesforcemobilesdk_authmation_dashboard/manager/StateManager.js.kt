package com.salesforce.salesforcemobilesdk_authmation_dashboard.manager

import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.DashboardState

actual object StateManager {
    actual fun saveState(state: DashboardState) {
        // TODO: Implement JS state persistence (e.g. localStorage)
        println("StateManager.saveState not implemented for JS")
    }

    actual fun loadState(): DashboardState? {
        // TODO: Implement JS state persistence
        println("StateManager.loadState not implemented for JS")
        return null
    }

    actual fun clearState() {
        // TODO: Implement JS state clearing
        println("StateManager.clearState not implemented for JS")
    }
}
