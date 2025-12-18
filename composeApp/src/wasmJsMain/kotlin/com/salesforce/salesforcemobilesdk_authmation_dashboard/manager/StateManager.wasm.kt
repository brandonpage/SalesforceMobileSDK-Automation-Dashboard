package com.salesforce.salesforcemobilesdk_authmation_dashboard.manager

import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.DashboardState

actual object StateManager {
    actual fun saveState(state: DashboardState) {
        // TODO: Implement Wasm state persistence
        println("StateManager.saveState not implemented for Wasm")
    }

    actual fun loadState(): DashboardState? {
        // TODO: Implement Wasm state persistence
        println("StateManager.loadState not implemented for Wasm")
        return null
    }

    actual fun clearState() {
        // TODO: Implement Wasm state clearing
        println("StateManager.clearState not implemented for Wasm")
    }
}
