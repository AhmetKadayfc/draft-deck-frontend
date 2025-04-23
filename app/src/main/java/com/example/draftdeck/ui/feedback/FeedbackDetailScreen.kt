package com.example.draftdeck.ui.feedback

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.draftdeck.data.model.Feedback
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.domain.util.Constants
import com.example.draftdeck.ui.components.DraftDeckAppBar
import com.example.draftdeck.ui.components.ErrorView
import com.example.draftdeck.ui.components.LoadingIndicator
import com.example.draftdeck.ui.feedback.components.CommentItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeedbackDetailScreen(
    feedbackId: String,
    viewModel: FeedbackViewModel,
    onBackClick: () -> Unit,
    onNavigateToUpdateFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val exportResult by viewModel.exportFeedbackResult.collectAsState()

    // For this screen, we would need to implement a function to get a single feedback by ID
    // Since we didn't create that in the ViewModel, we'll simulate it by using the feedback list
    // and filtering by ID

    val feedbackList by viewModel.feedbackList.collectAsState()
    val feedback = when (feedbackList) {
        is NetworkResult.Success -> {
            (feedbackList as NetworkResult.Success<List<Feedback>>).data.find { it.id == feedbackId }
        }
        else -> null
    }

    LaunchedEffect(exportResult) {
        if (exportResult is NetworkResult.Success) {
            val file = (exportResult as NetworkResult.Success<java.io.File>).data
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Open with"))
            viewModel.resetExportFeedbackResult()
        }
    }

    Scaffold(
        topBar = {
            DraftDeckAppBar(
                title = "Feedback Details",
                showBackButton = true,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { viewModel.exportFeedbackAsPdf(feedbackId, context) }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export as PDF"
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
            when (feedbackList) {
                is NetworkResult.Idle -> {
                    // Initial state, do nothing
                }
                is NetworkResult.Loading -> {
                    LoadingIndicator(fullScreen = true)
                }
                is NetworkResult.Success -> {
                    if (feedback == null) {
                        ErrorView(
                            message = "Feedback not found",
                            onRetry = { /* Reload feedback */ }
                        )
                    } else {
                        val isAdvisor = currentUser?.id == feedback.advisorId
                        val isAdmin = currentUser?.role == Constants.ROLE_ADMIN

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
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Advisor",
                                            tint = MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column {
                                            Text(
                                                text = "Advisor",
                                                style = MaterialTheme.typography.bodySmall
                                            )

                                            Text(
                                                text = feedback.advisorName,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }

                                        Spacer(modifier = Modifier.weight(1f))

                                        Text(
                                            text = formatDate(feedback.createdDate),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Overall Remarks",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = feedback.overallRemarks,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Inline Comments (${feedback.inlineComments.size})",
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (feedback.inlineComments.isEmpty()) {
                                Text(
                                    text = "No inline comments provided",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                feedback.inlineComments
                                    .sortedBy { it.pageNumber }
                                    .forEach { comment ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        CommentItem(comment = comment)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Actions based on user role
                            if (isAdvisor && !isAdmin) {
                                Button(
                                    onClick = { onNavigateToUpdateFeedback(feedbackId) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Feedback"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Edit Feedback")
                                }
                            }
                        }
                    }
                }
                is NetworkResult.Error -> {
                    ErrorView(
                        message = "Failed to load feedback: ${(feedback as NetworkResult.Error).exception.message}",
                        onRetry = { /* Reload feedback */ }
                    )
                }
            }

            // Loading overlay for export
            if (exportResult is NetworkResult.Loading) {
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
                            text = "Exporting feedback as PDF...",
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