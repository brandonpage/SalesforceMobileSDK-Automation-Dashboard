package com.salesforce.salesforcemobilesdk_authmation_dashboard.manager

import android.content.Context
import android.content.SharedPreferences

actual object TokenManager {
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("com.salesforce.authmation_dashboard", Context.MODE_PRIVATE)
    }

    actual fun saveToken(token: String) {
        sharedPreferences?.edit()?.putString("github_token", token.trim())?.apply()
    }

    actual fun getToken(): String? {
        return sharedPreferences?.getString("github_token", null)
    }
}
