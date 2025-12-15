package com.salesforce.salesforcemobilesdk_authmation_dashboard.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class GitHubClient(private val httpClient: HttpClient) {

    suspend fun downloadArtifact(owner: String, repo: String, artifactId: String, token: String? = null): ByteArray {
        val url = "https://api.github.com/repos/$owner/$repo/actions/artifacts/$artifactId/zip"
        val cleanToken = token?.trim()?.removePrefix("Bearer ")?.trim()
        
        println("Downloading artifact from: $url")
        println("Token provided: ${if (cleanToken.isNullOrBlank()) "NO" else "YES (Length: ${cleanToken.length})"}")
        
        // Initial request to GitHub API
        val response = httpClient.get(url) {
            cleanToken?.takeIf { it.isNotBlank() }?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
            header(HttpHeaders.Accept, "application/vnd.github+json")
            header(HttpHeaders.UserAgent, "SalesforceMobileSDK-Automation-Dashboard")
            header("X-GitHub-Api-Version", "2022-11-28")
            
            // We want to handle redirects manually to strip the Auth header
            expectSuccess = false
        }

        println("Initial response status: ${response.status} (${response.status.value})")

        if (response.status == HttpStatusCode.Unauthorized) {
            val errorBody = response.readBytes().decodeToString()
            println("401 Error Body: $errorBody")
            throw Exception("Authentication failed (401). Token present: ${!cleanToken.isNullOrBlank()}. GitHub says: $errorBody")
        } else if (response.status == HttpStatusCode.NotFound) {
             val errorBody = response.readBytes().decodeToString()
             throw Exception("Artifact not found (404) at $url.\nToken present: ${!cleanToken.isNullOrBlank()}.\nResponse: $errorBody\nEnsure your token has 'repo' (private) or 'public_repo' scope.")
        }

        when (response.status.value) {
            in 300..399 -> {
                val location = response.headers[HttpHeaders.Location]
                    ?: throw Exception("Redirect response missing Location header")

                println("Redirecting to: $location")

                // Follow redirect WITHOUT the Authorization header
                val redirectResponse = httpClient.get(location) {
                    // No Auth header here
                    expectSuccess = false
                }

                println("Redirect response status: ${redirectResponse.status}")

                if (redirectResponse.status.value !in 200..299) {
                    throw Exception("Failed to download artifact from redirect: ${redirectResponse.status}")
                }
                return redirectResponse.readBytes()
            }
            in 200..299 -> {
                return response.readBytes()
            }
            else -> {
                throw Exception(
                    "Failed to download artifact: ${response.status} - ${
                        response.readRawBytes().decodeToString()
                    }"
                )
            }
        }
    }
}
