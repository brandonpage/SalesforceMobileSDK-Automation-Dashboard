package com.salesforce.salesforcemobilesdk_authmation_dashboard.repository

import com.salesforce.salesforcemobilesdk_authmation_dashboard.network.GitHubClient
import com.salesforce.salesforcemobilesdk_authmation_dashboard.service.ZipService

import com.salesforce.salesforcemobilesdk_authmation_dashboard.network.model.Artifact
import com.salesforce.salesforcemobilesdk_authmation_dashboard.network.model.WorkflowRun

class ArtifactRepository(
    private val gitHubClient: GitHubClient,
    private val zipService: ZipService
) {
    suspend fun getWorkflowRuns(owner: String, repo: String, token: String?): List<WorkflowRun> {
        return gitHubClient.getWorkflowRuns(owner, repo, token)
    }

    suspend fun getArtifacts(owner: String, repo: String, runId: Long, token: String?): List<Artifact> {
        return gitHubClient.getArtifacts(owner, repo, runId, token)
    }

    suspend fun downloadAndExtractArtifact(owner: String, repo: String, artifactId: Long, token: String?): Map<String, String> {
        val zipBytes = gitHubClient.downloadArtifact(owner, repo, artifactId.toString(), token)
        val extractedFiles = zipService.unzip(zipBytes)
        
        return extractedFiles.mapValues { entry ->
            entry.value.decodeToString()
        }
    }

    suspend fun downloadAndExtractArtifact(url: String, token: String? = null): Map<String, String> {
        val cleanUrl = url.trim()
        val (owner, repo, artifactId) = parseArtifactUrl(cleanUrl)
        println("Parsed URL - Owner: $owner, Repo: $repo, ArtifactId: $artifactId")
        
        val zipBytes = gitHubClient.downloadArtifact(owner, repo, artifactId, token)
        val extractedFiles = zipService.unzip(zipBytes)
        
        return extractedFiles.mapValues { entry ->
            entry.value.decodeToString()
        }
    }

    private fun parseArtifactUrl(url: String): Triple<String, String, String> {
        val cleanUrl = url.split("?")[0] // Remove query parameters

        // Pattern 1: Browser URL (Runs)
        // https://github.com/owner/repo/actions/runs/runId/artifacts/artifactId
        Regex("github\\.com/([^/]+)/([^/]+)/actions/runs/\\d+/artifacts/(\\d+)").find(cleanUrl)?.let {
            val (owner, repo, artifactId) = it.destructured
            return Triple(owner, repo, artifactId)
        }

        // Pattern 2: Browser URL (Suites)
        // https://github.com/owner/repo/suites/suiteId/artifacts/artifactId
        Regex("github\\.com/([^/]+)/([^/]+)/suites/\\d+/artifacts/(\\d+)").find(cleanUrl)?.let {
            val (owner, repo, artifactId) = it.destructured
            return Triple(owner, repo, artifactId)
        }

        // Pattern 3: API URL
        // https://api.github.com/repos/owner/repo/actions/artifacts/artifactId
        Regex("api\\.github\\.com/repos/([^/]+)/([^/]+)/actions/artifacts/(\\d+)").find(cleanUrl)?.let {
            val (owner, repo, artifactId) = it.destructured
            return Triple(owner, repo, artifactId)
        }

        throw IllegalArgumentException("Invalid GitHub Artifact URL. Could not parse owner, repo, and artifact ID from: $url")
    }
}
