package com.salesforce.salesforcemobilesdk_authmation_dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import com.salesforce.salesforcemobilesdk_authmation_dashboard.manager.SettingsManager
import com.salesforce.salesforcemobilesdk_authmation_dashboard.manager.StateManager
import com.salesforce.salesforcemobilesdk_authmation_dashboard.manager.TokenManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        TokenManager.init(applicationContext)
        StateManager.init(applicationContext)
        SettingsManager.init(applicationContext)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}