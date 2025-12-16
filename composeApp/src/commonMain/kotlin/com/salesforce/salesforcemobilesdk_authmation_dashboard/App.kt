package com.salesforce.salesforcemobilesdk_authmation_dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.salesforce.salesforcemobilesdk_authmation_dashboard.manager.TokenManager
import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.DashboardState
import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.TableData
import com.salesforce.salesforcemobilesdk_authmation_dashboard.network.GitHubClient
import com.salesforce.salesforcemobilesdk_authmation_dashboard.repository.ArtifactRepository
import com.salesforce.salesforcemobilesdk_authmation_dashboard.repository.TestResultsRepository
import com.salesforce.salesforcemobilesdk_authmation_dashboard.service.XmlParsingService
import com.salesforce.salesforcemobilesdk_authmation_dashboard.service.getZipService

@Composable
@Preview
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        var githubToken by remember { mutableStateOf(TokenManager.getToken() ?: "") }
        var isTokenSaved by remember { mutableStateOf(TokenManager.getToken() != null) }
        var isLoading by remember { mutableStateOf(false) }
        var isAutoRefreshEnabled by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var dashboardState by remember { mutableStateOf<DashboardState?>(null) }

        val httpClient = remember {
            HttpClient {
                expectSuccess = false
                followRedirects = false
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                        isLenient = true
                    })
                }
            }
        }
        val testResultsRepository = remember {
            TestResultsRepository(
                ArtifactRepository(GitHubClient(httpClient), getZipService()),
                XmlParsingService()
            )
        }

        // Auto-refresh logic
        LaunchedEffect(isAutoRefreshEnabled) {
            if (isAutoRefreshEnabled) {
                while (isActive) {
                    // Don't refresh if already loading manually
                    if (!isLoading) {
                        try {
                            // Silent update if data already exists
                            if (dashboardState == null) isLoading = true
                            
                            val newState = testResultsRepository.loadDashboardData(githubToken, dashboardState)
                            dashboardState = newState
                            errorMessage = null
                        } catch (e: Exception) {
                            if (dashboardState == null) {
                                errorMessage = e.message
                            } else {
                                println("Auto-refresh failed: ${e.message}")
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                    delay(30_000) // 30 seconds
                }
            }
        }

        // Initial load if token exists
        LaunchedEffect(isTokenSaved) {
            if (isTokenSaved && dashboardState == null) {
                isLoading = true
                errorMessage = null
                try {
                    dashboardState = testResultsRepository.loadDashboardData(githubToken)
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Unknown error occurred"
                    e.printStackTrace()
                    // If auth fails, maybe we should let them re-enter token?
                    if (e.message?.contains("401") == true) {
                         isTokenSaved = false
                    }
                } finally {
                    isLoading = false
                }
            }
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .safeContentPadding()
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Text("SalesforceMobileSDK Automation Dashboard (v2)", style = MaterialTheme.typography.headlineMedium)
                
                if (isTokenSaved) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-refresh (30s)", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isAutoRefreshEnabled,
                            onCheckedChange = { isAutoRefreshEnabled = it }
                        )
                    }
                }
            }

            if (!isTokenSaved) {
                TokenEntryScreen(
                    token = githubToken,
                    onTokenChange = { githubToken = it },
                    onSave = {
                        if (githubToken.isNotBlank()) {
                            TokenManager.saveToken(githubToken)
                            isTokenSaved = true
                        }
                    }
                )
            } else {
                if (isLoading) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching latest Nightly run results...")
                } else if (errorMessage != null) {
                    Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                dashboardState = testResultsRepository.loadDashboardData(githubToken, dashboardState)
                            } catch (e: Exception) {
                                errorMessage = e.message
                            } finally {
                                isLoading = false
                            }
                        }
                    }) {
                        Text("Retry")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { isTokenSaved = false }) {
                        Text("Change Token")
                    }
                } else {
                    dashboardState?.let { state ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            state.androidResults?.let { table ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = "Android Results",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    if (table.status == "in_progress" || table.status == "queued") {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Run In Progress...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                DashboardGrid(table)
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            state.iosResults?.let { table ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = "iOS Results",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    if (table.status == "in_progress" || table.status == "queued") {
                                        Spacer(modifier = Modifier.width(12.dp))
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Run In Progress...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                DashboardGrid(table)
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        try {
                                            dashboardState = testResultsRepository.loadDashboardData(githubToken, dashboardState)
                                        } catch (e: Exception) {
                                            errorMessage = e.message
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Refresh")
                            }
                            
                            Spacer(modifier = Modifier.height(50.dp)) // Bottom padding
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TokenEntryScreen(
    token: String,
    onTokenChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Welcome! Please enter your GitHub Token to continue.")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChange,
            label = { Text("GitHub Token") },
            placeholder = { Text("ghp_...") },
            supportingText = { 
                Column {
                    Text("Requires 'repo' scope for private repos or actions access.")
                    Text("Note: Token expiration must be set to 366 days or less.", color = MaterialTheme.colorScheme.primary)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSave, enabled = token.isNotBlank()) {
            Text("Save & Continue")
        }
    }
}

@Composable
fun DashboardGrid(tableData: TableData) {
    val cellWidth = 80.dp
    val cellHeight = 40.dp
    val headerColor = MaterialTheme.colorScheme.surfaceVariant
    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .horizontalScroll(horizontalScrollState)
    ) {
        // Header Row (Columns)
        Row {
            // Corner cell
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(cellHeight)
                    .background(headerColor)
                    .border(1.dp, Color.LightGray)
                    .padding(8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("Library / Ver", fontWeight = FontWeight.Bold)
            }

            tableData.columns.forEach { column ->
                Box(
                    modifier = Modifier
                        .width(cellWidth)
                        .height(cellHeight)
                        .background(headerColor)
                        .border(1.dp, Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(column, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Data Rows
        tableData.libraries.forEach { library ->
            Row {
                // Library Name Column
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(cellHeight)
                        .background(headerColor)
                        .border(1.dp, Color.LightGray)
                        .padding(8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(library, style = MaterialTheme.typography.bodyMedium)
                }

                // Result Cells
                tableData.columns.forEach { column ->
                    val cellData = tableData.results[library]?.get(column)
                    val isRunInProgress = tableData.status == "in_progress" || tableData.status == "queued"
                    
                    val backgroundColor = when {
                        cellData != null -> if (cellData.isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336)
                        isRunInProgress -> Color(0xFFFFF59D) // Light Yellow
                        else -> Color.LightGray
                    }
                    
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .height(cellHeight)
                            .background(backgroundColor)
                            .border(1.dp, Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                         when {
                            cellData != null -> {
                                val text = if (cellData.isSuccess) "PASS" else "FAIL"
                                Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                            isRunInProgress -> {
                                Text("...", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            else -> Text("-", color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}