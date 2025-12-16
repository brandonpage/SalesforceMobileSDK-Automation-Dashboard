package com.salesforce.salesforcemobilesdk_authmation_dashboard.model

data class DashboardState(
    val androidResults: TableData? = null,
    val iosResults: TableData? = null
)

data class TableData(
    val title: String,
    val libraries: List<String>,
    val columns: List<String>,
    val results: Map<String, Map<String, CellData?>>,
    val status: String? = null,
    val id: Long? = null
)

data class CellData(
    val isSuccess: Boolean,
    val suites: List<TestSuite>
)
