package com.salesforce.salesforcemobilesdk_authmation_dashboard.service

import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.TestSuite
import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.TestSuites
import nl.adaptivity.xmlutil.serialization.XML

class XmlParsingService {
    private val xml = XML {
        unknownChildHandler = nl.adaptivity.xmlutil.serialization.UnknownChildHandler { _, _, _, _, _ -> emptyList() }
    }

    fun parseTestResults(content: String): List<TestSuite> {
        return try {
            // Try parsing as TestSuites (root element <testsuites>)
            val testSuites = xml.decodeFromString(TestSuites.serializer(), content)
            testSuites.testsuites
        } catch (e: Exception) {
            try {
                // Try parsing as single TestSuite (root element <testsuite>)
                val testSuite = xml.decodeFromString(TestSuite.serializer(), content)
                listOf(testSuite)
            } catch (e2: Exception) {
                println("Failed to parse XML: ${e2.message}")
                emptyList()
            }
        }
    }
}
