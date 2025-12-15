package com.salesforce.salesforcemobilesdk_authmation_dashboard.service

class PlatformZipService : ZipService {
    override suspend fun unzip(zipData: ByteArray): Map<String, ByteArray> {
        // TODO: Implement for iOS
        return emptyMap()
    }
}

actual fun getZipService(): ZipService = PlatformZipService()
