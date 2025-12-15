package com.salesforce.salesforcemobilesdk_authmation_dashboard

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform