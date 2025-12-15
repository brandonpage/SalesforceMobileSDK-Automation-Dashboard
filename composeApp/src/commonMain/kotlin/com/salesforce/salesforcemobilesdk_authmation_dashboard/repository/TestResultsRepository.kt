package com.salesforce.salesforcemobilesdk_authmation_dashboard.repository

import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.TestSuite
import com.salesforce.salesforcemobilesdk_authmation_dashboard.service.XmlParsingService

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TestResultsRepository(
    private val artifactRepository: ArtifactRepository,
    private val xmlParsingService: XmlParsingService
) {
    suspend fun loadTestResults(url: String, token: String? = null): List<TestSuite> {
        val files = artifactRepository.downloadAndExtractArtifact(url, token)
        
        return withContext(Dispatchers.Default) {
            files.filterKeys { it.endsWith(".xml") }
                .flatMap { (filename, content) ->
                    xmlParsingService.parseTestResults(content).map { suite ->
                        suite.copy(name = "$filename - ${suite.name}")
                    }
                }
        }
    }
}
