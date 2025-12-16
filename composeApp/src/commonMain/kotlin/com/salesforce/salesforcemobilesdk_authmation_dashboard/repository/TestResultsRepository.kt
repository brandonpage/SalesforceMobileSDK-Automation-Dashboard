package com.salesforce.salesforcemobilesdk_authmation_dashboard.repository

import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.CellData
import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.DashboardState
import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.TableData
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

    suspend fun loadDashboardData(token: String?, currentState: DashboardState? = null): DashboardState {
        println("Starting loadDashboardData (v3 - Multi-platform)")
        
        val androidResults = loadAndroidResults(token, currentState?.androidResults)
        val iosResults = loadIosResults(token, currentState?.iosResults)
        
        return DashboardState(
            androidResults = androidResults,
            iosResults = iosResults
        )
    }

    private suspend fun loadAndroidResults(token: String?, currentTable: TableData?): TableData {
        val owner = "brandonpage"
        val repo = "SalesforceMobileSDK-Android"
        val targetLibraries = listOf(
            "SalesforceAnalytics",
            "SalesforceSDK",
            "SmartStore",
            "MobileSync",
            "SalesforceHybrid",
            "SalesforceReact"
        )
        val apiLevels = (28..36).toList().map { it.toString() }
        
        val (runId, runStatus, artifacts) = getNightlyRunAndArtifacts(owner, repo, token)
        
        // Cache Check: If ID matches and we already have completed results, return cached
        if (currentTable != null && currentTable.id == runId && currentTable.status == "completed" && runStatus == "completed") {
             println("Cache Hit for Android: Run $runId is already loaded and completed.")
             return currentTable
        }
        
        // If ID matches and status is same (e.g. still in_progress), we can also skip unless we want to poll for progress
        // But usually in_progress means no results yet.
        if (currentTable != null && currentTable.id == runId && currentTable.status == runStatus) {
             println("Cache Hit for Android: Run $runId status unchanged ($runStatus).")
             return currentTable
        }

        val resultsMap = createEmptyResultsMap(targetLibraries, apiLevels)
        
        artifacts.forEach { artifact ->
            val libraryName = targetLibraries.find { artifact.name.equals("test-results-$it", ignoreCase = true) }
            if (libraryName != null) {
                try {
                    val files = artifactRepository.downloadAndExtractArtifact(owner, repo, artifact.id, token)
                    files.filterKeys { it.endsWith(".xml") }.forEach { (filename, content) ->
                        val apiLevel = parseApiLevelFromFilename(filename)?.toString()
                        if (apiLevel != null && apiLevels.contains(apiLevel)) {
                            val suites = xmlParsingService.parseTestResults(content)
                            val isSuccess = suites.all { it.failures == 0 && it.errors == 0 }
                            updateResultsMap(resultsMap, libraryName, apiLevel, isSuccess, suites)
                        }
                    }
                } catch (e: Exception) {
                    println("Failed to process artifact ${artifact.name}: ${e.message}")
                }
            }
        }
        
        return TableData("Android", targetLibraries, apiLevels, resultsMap, runStatus, runId)
    }

    private suspend fun loadIosResults(token: String?, currentTable: TableData?): TableData {
        val owner = "brandonpage"
        val repo = "SalesforceMobileSDK-iOS"
        val targetLibraries = listOf(
            "SalesforceAnalytics",
            "SalesforceSDKCommon",
            "SalesforceSDKCore",
            "SmartStore",
            "MobileSync"
        )
        val versions = listOf("17", "18", "26")
        
        val (runId, runStatus, artifacts) = getNightlyRunAndArtifacts(owner, repo, token)

        // Cache Check
        if (currentTable != null && currentTable.id == runId && currentTable.status == "completed" && runStatus == "completed") {
             println("Cache Hit for iOS: Run $runId is already loaded and completed.")
             return currentTable
        }

        if (currentTable != null && currentTable.id == runId && currentTable.status == runStatus) {
             println("Cache Hit for iOS: Run $runId status unchanged ($runStatus).")
             return currentTable
        }

        val resultsMap = createEmptyResultsMap(targetLibraries, versions)
        
        artifacts.forEach { artifact ->
            val regex = Regex("test-results-(.+)-ios\\^(.+)")
            val match = regex.find(artifact.name)
            if (match != null) {
                val (libName, version) = match.destructured
                val normalizedLib = targetLibraries.find { it.equals(libName, ignoreCase = true) }
                
                if (normalizedLib != null && versions.contains(version)) {
                    try {
                        val files = artifactRepository.downloadAndExtractArtifact(owner, repo, artifact.id, token)
                        files.filterKeys { it.endsWith(".xml") }.forEach { (_, content) ->
                            val suites = xmlParsingService.parseTestResults(content)
                            val isSuccess = suites.all { it.failures == 0 && it.errors == 0 }
                            updateResultsMap(resultsMap, normalizedLib, version, isSuccess, suites)
                        }
                    } catch (e: Exception) {
                         println("Failed to process artifact ${artifact.name}: ${e.message}")
                    }
                }
            }
        }
        
        return TableData("iOS", targetLibraries, versions, resultsMap, runStatus, runId)
    }

    private suspend fun getNightlyRunAndArtifacts(owner: String, repo: String, token: String?): Triple<Long?, String?, List<com.salesforce.salesforcemobilesdk_authmation_dashboard.network.model.Artifact>> {
        try {
            println("Fetching workflow runs for $owner/$repo...")
            val runs = artifactRepository.getWorkflowRuns(owner, repo, token)
            // Allow in_progress and queued runs to be candidates
            val candidateRuns = runs.filter { it.name.equals("Nightly Tests", ignoreCase = true) }
                .take(5)
            
            for (run in candidateRuns) {
                val artifacts = artifactRepository.getArtifacts(owner, repo, run.id, token)
                
                // If it's running, return it immediately regardless of artifacts (might be partial)
                if (run.status == "in_progress" || run.status == "queued") {
                     println("Found active run for $owner/$repo: ${run.id} (${run.status})")
                     return Triple(run.id, run.status, artifacts)
                }

                // If completed, only return if it has artifacts
                if (run.status == "completed" && artifacts.isNotEmpty()) {
                    println("Found valid completed run for $owner/$repo: ${run.id}")
                    return Triple(run.id, run.status, artifacts)
                }
            }
        } catch (e: Exception) {
            println("Error fetching artifacts for $owner/$repo: ${e.message}")
        }
        println("No valid Nightly Tests run found for $owner/$repo")
        return Triple(null, null, emptyList())
    }

    private fun createEmptyResultsMap(libraries: List<String>, columns: List<String>): MutableMap<String, MutableMap<String, CellData?>> {
        val map = mutableMapOf<String, MutableMap<String, CellData?>>()
        libraries.forEach { lib ->
            map[lib] = mutableMapOf()
            columns.forEach { col -> map[lib]!![col] = null }
        }
        return map
    }

    private fun updateResultsMap(map: MutableMap<String, MutableMap<String, CellData?>>, lib: String, col: String, isSuccess: Boolean, suites: List<TestSuite>) {
        val current = map[lib]!![col]
        if (current == null) {
            map[lib]!![col] = CellData(isSuccess, suites)
        } else {
            val newSuccess = current.isSuccess && isSuccess
            val newSuites = current.suites + suites
            map[lib]!![col] = CellData(newSuccess, newSuites)
        }
    }

    private fun parseApiLevelFromFilename(filename: String): Int? {
        // Robust regex to find API level (28-36) in filename
        // Matches:
        // 1. "api" prefix: api28, api-28, api_28, API28
        // 2. File ending in number: /28.xml, -28.xml, _28.xml, .28.xml, or just 28.xml (start of string)
        val regex = Regex("(?i)(?:api)[-_]?([2-3][0-9])|(?:^|[-_./])([2-3][0-9])\\.xml")
        
        val match = regex.find(filename)
        return (match?.groups?.get(1)?.value ?: match?.groups?.get(2)?.value)?.toIntOrNull()
    }
}

