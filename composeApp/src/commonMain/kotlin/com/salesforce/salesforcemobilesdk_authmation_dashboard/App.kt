package com.salesforce.salesforcemobilesdk_authmation_dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRedirect
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.salesforce.salesforcemobilesdk_authmation_dashboard.network.GitHubClient
import com.salesforce.salesforcemobilesdk_authmation_dashboard.repository.ArtifactRepository
import com.salesforce.salesforcemobilesdk_authmation_dashboard.repository.TestResultsRepository
import com.salesforce.salesforcemobilesdk_authmation_dashboard.service.XmlParsingService
import com.salesforce.salesforcemobilesdk_authmation_dashboard.service.getZipService
import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.TestSuite

@Composable
@Preview
fun App() {
    MaterialTheme {
        var artifactUrl by remember { mutableStateOf("https://github.com/brandonpage/SalesforceMobileSDK-Android/actions/runs/20244467789/artifacts/4876662269") }
        var githubToken by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var hasSearched by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var testResults by remember { mutableStateOf<List<TestSuite>>(emptyList()) }
        val scope = rememberCoroutineScope()

        val httpClient = remember { 
            HttpClient {
                // Manual redirect handling in GitHubClient to strip Auth header
                expectSuccess = false
                followRedirects = false
            } 
        }
        val testResultsRepository = remember {
            TestResultsRepository(
                ArtifactRepository(GitHubClient(httpClient), getZipService()),
                XmlParsingService()
            )
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .safeContentPadding()
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("GitHub Artifact Downloader", style = MaterialTheme.typography.headlineMedium)
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = artifactUrl,
                onValueChange = { artifactUrl = it },
                label = { Text("Artifact URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = githubToken,
                onValueChange = { githubToken = it },
                label = { Text("GitHub Token (Required for Private Repos)") },
                placeholder = { Text("ghp_...") },
                supportingText = { Text("Requires 'repo' or 'workflow' scope") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        hasSearched = true
                        errorMessage = null
                        testResults = emptyList()
                        try {
                            testResults = testResultsRepository.loadTestResults(artifactUrl, githubToken.takeIf { it.isNotBlank() })
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Unknown error occurred"
                            e.printStackTrace()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && artifactUrl.isNotBlank()
            ) {
                Text(if (isLoading) "Downloading & Parsing..." else "Fetch Results")
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            
            if (!isLoading && hasSearched && testResults.isEmpty() && errorMessage == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("No test results found.", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(testResults) { suite ->
                    TestSuiteItem(suite)
                }
            }
        }
    }
}

@Composable
fun TestSuiteItem(suite: TestSuite) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
            .padding(8.dp)
    ) {
        Text(text = suite.name, style = MaterialTheme.typography.titleMedium)
        Row {
            Text("Tests: ${suite.tests}", modifier = Modifier.padding(end = 8.dp))
            Text("Failures: ${suite.failures}", color = if (suite.failures > 0) Color.Red else Color.Unspecified, modifier = Modifier.padding(end = 8.dp))
            Text("Errors: ${suite.errors}", color = if (suite.errors > 0) Color.Red else Color.Unspecified)
        }
        if (suite.failures > 0 || suite.errors > 0) {
            suite.testcases.filter { it.failure != null }.forEach { failedCase ->
                Text("Failed: ${failedCase.name}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("Message: ${failedCase.failure?.message}", style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
    }
}