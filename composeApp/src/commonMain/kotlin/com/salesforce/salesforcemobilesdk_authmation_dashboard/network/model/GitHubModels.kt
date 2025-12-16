package com.salesforce.salesforcemobilesdk_authmation_dashboard.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowRunsResponse(
    @SerialName("workflow_runs") val workflowRuns: List<WorkflowRun>,
    @SerialName("total_count") val totalCount: Int
)

@Serializable
data class WorkflowRun(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String?,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ArtifactsResponse(
    val artifacts: List<Artifact>,
    @SerialName("total_count") val totalCount: Int
)

@Serializable
data class Artifact(
    val id: Long,
    val name: String,
    @SerialName("archive_download_url") val archiveDownloadUrl: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("size_in_bytes") val sizeInBytes: Long
)
