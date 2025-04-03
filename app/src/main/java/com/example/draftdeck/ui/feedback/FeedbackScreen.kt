package com.example.draftdeck.ui.feedback

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.domain.util.Constants
import com.example.draftdeck.ui.components.DraftDeckAppBar
import com.example.draftdeck.ui.components.ErrorView
import com.example.draftdeck.ui.components.LoadingIndicator
import com.example.draftdeck.ui.feedback.components.FeedbackCard

@Composable
fun FeedbackScreen(
    thesisId: String,
    viewModel: FeedbackViewModel,
    onBackClick: () -> Unit,
    onNavigateToFeedbackDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val thesisDetails by viewModel.thesisDetails.collectAsState()
    val feedbackList by viewModel.feedbackList.collectAsState()
    val exportResult by viewModel.exportFeedbackResult.collectAsState()

    LaunchedEffect(thesisId) {
        viewModel.loadThesisDetails(thesisId)
        viewModel.loadFeedbackList(thesisId)
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
                title = when (val result = thesisDetails) {
                    is NetworkResult.Success -> "Feedback - ${result.data.title}"
                    else -> "Feedback"
                },
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            if (currentUser?.role == Constants.ROLE_ADVISOR) {
                ExtendedFloatingActionButton(
                    onClick = { /* Navigate to add feedback */ },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Feedback"
                        )
                    },
                    text = { Text("Add Feedback") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val result = feedbackList) {
                is NetworkResult.Loading -> {
                    LoadingIndicator(fullScreen = true)
                }
                is NetworkResult.Success -> {
                    if (result.data.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No feedback yet",
                                style = MaterialTheme.typography.headlineSmall
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (currentUser?.role == Constants.ROLE_ADVISOR) {
                                    "Click the button below to add feedback"
                                } else {
                                    "Your advisor has not added any feedback yet"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(result.data) { feedback ->
                                FeedbackCard(
                                    feedback = feedback,
                                    onClick = { onNavigateToFeedbackDetail(feedback.id) }
                                )
                            }
                        }
                    }
                }
                is NetworkResult.Error -> {
                    ErrorView(
                        message = "Failed to load feedback: ${result.exception.message}",
                        onRetry = { viewModel.loadFeedbackList(thesisId) }
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