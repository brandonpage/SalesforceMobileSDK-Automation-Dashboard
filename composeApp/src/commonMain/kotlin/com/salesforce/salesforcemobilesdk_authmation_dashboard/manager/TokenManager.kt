package com.salesforce.salesforcemobilesdk_authmation_dashboard.manager

expect object TokenManager {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()
}
