package com.salesforce.salesforcemobilesdk_authmation_dashboard

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()