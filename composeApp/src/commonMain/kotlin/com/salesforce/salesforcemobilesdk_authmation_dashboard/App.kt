package com.salesforce.salesforcemobilesdk_authmation_dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salesforce.salesforcemobilesdk_authmation_dashboard.manager.SettingsManager
import com.salesforce.salesforcemobilesdk_authmation_dashboard.manager.StateManager
import com.salesforce.salesforcemobilesdk_authmation_dashboard.manager.TokenManager
import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.CellData
import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.TableData
import com.salesforce.salesforcemobilesdk_authmation_dashboard.model.TestCase
import com.salesforce.salesforcemobilesdk_authmation_dashboard.network.GitHubClient
import com.salesforce.salesforcemobilesdk_authmation_dashboard.repository.ArtifactRepository
import com.salesforce.salesforcemobilesdk_authmation_dashboard.repository.TestResultsRepository
import com.salesforce.salesforcemobilesdk_authmation_dashboard.service.XmlParsingService
import com.salesforce.salesforcemobilesdk_authmation_dashboard.service.getZipService
import com.salesforce.salesforcemobilesdk_authmation_dashboard.theme.AppTheme
import com.salesforce.salesforcemobilesdk_authmation_dashboard.theme.FailureRed
import com.salesforce.salesforcemobilesdk_authmation_dashboard.theme.FailureRedDark
import com.salesforce.salesforcemobilesdk_authmation_dashboard.theme.SuccessGreen
import com.salesforce.salesforcemobilesdk_authmation_dashboard.theme.SuccessGreenDark
import com.salesforce.salesforcemobilesdk_authmation_dashboard.theme.WarningYellow
import com.salesforce.salesforcemobilesdk_authmation_dashboard.theme.WarningYellowDark
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val systemInDarkTheme = isSystemInDarkTheme()
    var useDarkTheme by remember {
        mutableStateOf(SettingsManager.getThemeMode() ?: systemInDarkTheme)
    }

    AppTheme(useDarkTheme = useDarkTheme) {
        val scope = rememberCoroutineScope()
        var githubToken by remember { mutableStateOf(TokenManager.getToken() ?: "") }
        var isTokenSaved by remember { mutableStateOf(TokenManager.getToken() != null) }
        var isLoading by remember { mutableStateOf(false) }
        var isAutoRefreshEnabled by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        // Load initial state from disk
        var dashboardState by remember { mutableStateOf(StateManager.loadState()) }

        var selectedCell by remember { mutableStateOf<CellData?>(null) }

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

        if (selectedCell != null) {
            FailureDetailsDialog(
                cellData = selectedCell!!,
                onDismiss = { selectedCell = null }
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

                            val newState =
                                testResultsRepository.loadDashboardData(githubToken, dashboardState)
                            dashboardState = newState
                            StateManager.saveState(newState)
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
                    val newState = testResultsRepository.loadDashboardData(githubToken)
                    dashboardState = newState
                    StateManager.saveState(newState)
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

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
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
                    Text(
                        "Mobile SDK Automation Dashboard",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    if (isTokenSaved) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Dark Mode", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = useDarkTheme,
                                onCheckedChange = {
                                    useDarkTheme = it
                                    SettingsManager.saveThemeMode(it)
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Auto-refresh (30s)", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isAutoRefreshEnabled,
                                onCheckedChange = { isAutoRefreshEnabled = it }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Button(onClick = {
                                TokenManager.clearToken()
                                StateManager.clearState()
                                isTokenSaved = false
                                githubToken = ""
                                dashboardState = null
                                errorMessage = null
                            }) {
                                Text("Remove Token")
                            }
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
                                isLoading = true
                                errorMessage = null
                                isTokenSaved = true
                            }
                        }
                    )
                } else {
                    if (isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Fetching latest Nightly run results...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else if (errorMessage != null) {
                        Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val newState = testResultsRepository.loadDashboardData(
                                        githubToken,
                                        dashboardState
                                    )
                                    dashboardState = newState
                                    StateManager.saveState(newState)
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
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val availableWidth = maxWidth
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    state.combinedResults?.let { table ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        ) {
                                            Text(
                                                text = "Unit Test Results",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            if (table.status == "in_progress" || table.status == "queued") {
                                                Spacer(modifier = Modifier.width(12.dp))
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "Run In Progress...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        DashboardGrid(
                                            table,
                                            isDarkTheme = useDarkTheme,
                                            availableWidth = availableWidth,
                                            onCellClick = { cell ->
                                                if (!cell.isSuccess) {
                                                    selectedCell = cell
                                                }
                                            })
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }

                                    Spacer(modifier = Modifier.height(50.dp)) // Bottom padding
                                }
                            }
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Please enter your GitHub Personal Access Token to continue.  No permissions are required.")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = token,
                onValueChange = onTokenChange,
                label = { Text("GitHub Token") },
                placeholder = { Text("github_pat_...") },
                supportingText = {
                    Column {
                        Text(
                            "Note: Token expiration must be set to 365 days or less.",
                            color = MaterialTheme.colorScheme.primary
                        )
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
    fun DashboardGrid(tableData: TableData, isDarkTheme: Boolean, availableWidth: androidx.compose.ui.unit.Dp, onCellClick: (CellData) -> Unit) {
        val headerColumnWidth = 150.dp
        val separatorWidth = 24.dp
        val numSeparators = tableData.columns.count { it == "SEPARATOR" }
        val numDataColumns = tableData.columns.size - numSeparators
        
        // Calculate cell width to fit available space
        val totalSeparatorWidth = separatorWidth * numSeparators
        val availableForCells = availableWidth - headerColumnWidth - totalSeparatorWidth
        val cellWidth = (availableForCells / numDataColumns).coerceAtLeast(100.dp)
        
        val cellHeight = 60.dp
        val headerColor = MaterialTheme.colorScheme.surfaceVariant
        val borderColor = MaterialTheme.colorScheme.outline
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
                        .border(1.dp, borderColor)
                        .padding(8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        "Library / Ver",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                tableData.columns.forEach { column ->
                    if (column == "SEPARATOR") {
                        Spacer(modifier = Modifier.width(24.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .width(cellWidth)
                                .height(cellHeight)
                                .background(headerColor)
                                .border(1.dp, borderColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = column,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                            .border(1.dp, borderColor)
                            .padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = library,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Result Cells
                    tableData.columns.forEach { column ->
                        if (column == "SEPARATOR") {
                            Spacer(modifier = Modifier.width(24.dp))
                        } else {
                            val cellData = tableData.results[library]?.get(column)
                            val isRunInProgress =
                                tableData.status == "in_progress" || tableData.status == "queued"

                            val backgroundColor = when {
                                cellData != null -> {
                                    if (cellData.isSuccess) {
                                        if (isDarkTheme) SuccessGreenDark else SuccessGreen
                                    } else {
                                        if (isDarkTheme) FailureRedDark else FailureRed
                                    }
                                }

                                // Common library doesn't exist on Android - always gray it out for Android columns
                                library == "Common" && column.contains("Android") -> MaterialTheme.colorScheme.surfaceVariant

                                isRunInProgress -> if (isDarkTheme) WarningYellowDark else WarningYellow
                                else -> MaterialTheme.colorScheme.surfaceVariant // Was LightGray
                            }

                            val contentColor = when {
                                cellData != null -> {
                                    if (isDarkTheme) Color.Black else Color.White
                                }

                                // Common library doesn't exist on Android - use regular text color
                                library == "Common" && column.contains("Android") -> MaterialTheme.colorScheme.onSurfaceVariant

                                isRunInProgress -> Color.Black
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .height(cellHeight)
                                    .background(backgroundColor)
                                    .border(1.dp, borderColor)
                                    .clickable(enabled = cellData != null && !cellData.isSuccess) {
                                        if (cellData != null) {
                                            onCellClick(cellData)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    cellData != null -> {
                                        if (cellData.isSuccess) {
                                            Text(
                                                "PASS",
                                                color = contentColor,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        } else {
                                            val failureCount =
                                                cellData.suites.sumOf { it.failures + it.errors }
                                            Text(
                                                "FAIL ($failureCount)",
                                                color = contentColor,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Common library doesn't exist on Android - always show dash
                                    library == "Common" && column.contains("Android") -> {
                                        Text("-", color = contentColor)
                                    }

                                    isRunInProgress -> {
                                        val infiniteTransition = rememberInfiniteTransition()
                                        val alpha by infiniteTransition.animateFloat(
                                            initialValue = 0.3f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1000),
                                                repeatMode = RepeatMode.Reverse
                                            )
                                        )
                                        Text(
                                            "In Progress",
                                            color = contentColor,
                                            fontStyle = FontStyle.Italic,
                                            modifier = Modifier.graphicsLayer(alpha = alpha)
                                        )
                                    }

                                    else -> Text("-", color = contentColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun FailureDetailsDialog(cellData: CellData, onDismiss: () -> Unit) {
        val failedTests = remember(cellData) {
            cellData.suites.flatMap { suite ->
                suite.testcases.filter { it.failure != null }
            }
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            },
            title = {
                Text("Failed Tests (${failedTests.size})")
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(failedTests) { test ->
                        FailedTestItem(test)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }

    @Composable
    fun FailedTestItem(test: TestCase) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                Text(
                    text = test.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = test.classname,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (test.failure?.message?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Message: ${test.failure.message}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (test.failure?.content?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stacktrace:",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = test.failure.content.trim(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }