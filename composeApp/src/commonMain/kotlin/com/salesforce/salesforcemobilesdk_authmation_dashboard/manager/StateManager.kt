package com.salesforce.salesforcemobilesdk_authmation_dashboard.manager

import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.DashboardState

expect object StateManager {
    fun saveState(state: DashboardState)
    fun loadState(): DashboardState?
}
