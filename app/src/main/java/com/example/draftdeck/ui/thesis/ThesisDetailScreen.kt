package com.example.draftdeck.ui.thesis

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.domain.util.Constants
import com.example.draftdeck.ui.components.DraftDeckAppBar
import com.example.draftdeck.ui.components.ErrorView
import com.example.draftdeck.ui.components.LoadingIndicator
import com.example.draftdeck.ui.theme.DraftDeckTheme
import com.example.draftdeck.ui.thesis.components.ThesisStatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ThesisDetailScreen(
    thesisId: String,
    viewModel: ThesisViewModel,
    onBackClick: () -> Unit,
    onNavigateToFeedback: (String) -> Unit,
    onNavigateToUpdateThesis: (String) -> Unit,
    onNavigateToAddFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val thesisDetails by viewModel.thesisDetails.collectAsState()
    val downloadResult by viewModel.downloadThesisResult.collectAsState()

    LaunchedEffect(thesisId) {
        viewModel.loadThesisDetails(thesisId)
    }

    LaunchedEffect(downloadResult) {
        if (downloadResult is NetworkResult.Success) {
            val file = (downloadResult as NetworkResult.Success<java.io.File>).data
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, when {
                    file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                    file.name.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    else -> "application/octet-stream"
                })
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Open with"))
            viewModel.resetDownloadResult()
        }
    }

    Scaffold(
        topBar = {
            DraftDeckAppBar(
                title = "Thesis Details",
                showBackButton = true,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { viewModel.downloadThesis(thesisId, context) }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Thesis"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val details = thesisDetails) {
                is NetworkResult.Idle -> {
                    // Initial state, do nothing
                }
                is NetworkResult.Loading -> {
                    LoadingIndicator(fullScreen = true)
                }
                is NetworkResult.Success -> {
                    val thesis = details.data
                    val isStudent = currentUser?.role == Constants.ROLE_STUDENT
                    val isAdvisor = currentUser?.role == Constants.ROLE_ADVISOR
                    val isAdmin = currentUser?.role == Constants.ROLE_ADMIN
                    val isOwner = currentUser?.id == thesis.studentId

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = thesis.title,
                                            style = MaterialTheme.typography.headlineSmall
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ThesisStatusBadge(status = thesis.status)

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = "v${thesis.version} | ${thesis.submissionType}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = thesis.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Divider()

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Student",
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Text(
                                            text = "Student",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        Text(
                                            text = thesis.studentName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Advisor",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Text(
                                            text = "Advisor",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        Text(
                                            text = thesis.advisorName,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Submission Date",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        Text(
                                            text = formatDate(thesis.submissionDate),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Last Updated",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        Text(
                                            text = formatDate(thesis.lastUpdated),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Actions based on user role
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Student actions
                            if (isStudent && isOwner) {
                                OutlinedButton(
                                    onClick = { onNavigateToUpdateThesis(thesis.id) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Edit")
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Button(
                                onClick = { onNavigateToFeedback(thesis.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Comment,
                                    contentDescription = "Feedback"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Feedback")
                            }
                        }

                        // Advisor actions
                        if (isAdvisor && !isAdmin) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { onNavigateToAddFeedback(thesis.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add Feedback")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Status management for advisor
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = { viewModel.updateThesisStatus(thesis.id, Constants.STATUS_APPROVED) },
                                    modifier = Modifier.weight(1f),
                                    enabled = thesis.status != Constants.STATUS_APPROVED
                                ) {
                                    Text("Approve")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = { viewModel.updateThesisStatus(thesis.id, Constants.STATUS_REJECTED) },
                                    modifier = Modifier.weight(1f),
                                    enabled = thesis.status != Constants.STATUS_REJECTED
                                ) {
                                    Text("Reject")
                                }
                            }
                        }
                    }
                }
                is NetworkResult.Error -> {
                    ErrorView(
                        message = "Failed to load thesis: ${details.exception.message}",
                        onRetry = { viewModel.loadThesisDetails(thesisId) }
                    )
                }
            }

            // Loading overlay for download
            if (downloadResult is NetworkResult.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LoadingIndicator()

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Downloading thesis...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}

@Preview
@Composable
fun ThesisDetailScreenPreview() {
    // This preview won't work properly due to the view model dependency
    // but it's useful for layout checks
    DraftDeckTheme {
        // ThesisDetailScreen(...)
    }
}