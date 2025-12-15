package com.salesforce.salesforcemobilesdk_authmation_dashboard.service

interface ZipService {
    suspend fun unzip(zipData: ByteArray): Map<String, ByteArray>
}

expect fun getZipService(): ZipService
