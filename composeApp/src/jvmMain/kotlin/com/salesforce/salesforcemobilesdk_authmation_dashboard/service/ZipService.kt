package com.salesforce.salesforcemobilesdk_authmation_dashboard.service

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class JvmZipService : ZipService {
    override suspend fun unzip(zipData: ByteArray): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipData)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (!entry.isDirectory) {
                    val buffer = ByteArray(entry.size.toInt().takeIf { it >= 0 } ?: 4096)
                    val output = java.io.ByteArrayOutputStream()
                    val data = ByteArray(4096)
                    var count: Int
                    while (zis.read(data).also { count = it } != -1) {
                        output.write(data, 0, count)
                    }
                    result[entry.name] = output.toByteArray()
                }
                zis.closeEntry()
            }
        }
        return result
    }
}

actual fun getZipService(): ZipService = JvmZipService()
