package com.example.draftdeck.ui.feedback

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.draftdeck.data.model.CommentPosition
import com.example.draftdeck.data.model.InlineComment
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.ui.components.DraftDeckAppBar
import com.example.draftdeck.ui.components.LoadingIndicator
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeedbackScreen(
    thesisId: String,
    viewModel: FeedbackViewModel,
    onBackClick: () -> Unit,
    onFeedbackSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    feedbackId: String? = null,
    isUpdate: Boolean = false
) {
    var overallRemarks by remember { mutableStateOf("") }
    val inlineComments = remember { mutableStateListOf<InlineComment>() }

    // For adding new inline comments
    var showAddCommentDialog by remember { mutableStateOf(false) }
    var commentContent by remember { mutableStateOf("") }
    var commentPage by remember { mutableStateOf("1") }
    var commentType by remember { mutableStateOf("Suggestion") }
    var isCommentTypeDropdownExpanded by remember { mutableStateOf(false) }

    val commentTypes = listOf("Suggestion", "Correction", "Question")

    val addFeedbackResult by viewModel.addFeedbackResult.collectAsState()
    val updateFeedbackResult by viewModel.updateFeedbackResult.collectAsState()

    LaunchedEffect(thesisId) {
        viewModel.loadThesisDetails(thesisId)
    }

    // Handle feedback submission success
    LaunchedEffect(addFeedbackResult, updateFeedbackResult) {
        val result = if (isUpdate) updateFeedbackResult else addFeedbackResult

        if (result is NetworkResult.Success) {
            if (isUpdate) {
                viewModel.resetUpdateFeedbackResult()
            } else {
                viewModel.resetAddFeedbackResult()
            }
            onFeedbackSuccess()
        }
    }

    Scaffold(
        topBar = {
            DraftDeckAppBar(
                title = if (isUpdate) "Update Feedback" else "Add Feedback",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Thesis Information
                when (val thesisResult = viewModel.thesisDetails.collectAsState().value) {
                    is NetworkResult.Idle -> {
                        // Initial state, nothing to display yet
                    }
                    is NetworkResult.Success -> {
                        val thesis = thesisResult.data
                        Text(
                            text = thesis.title,
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "by ${thesis.studentName}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is NetworkResult.Loading -> {
                        Text(
                            text = "Loading thesis details...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is NetworkResult.Error -> {
                        Text(
                            text = "Error loading thesis: ${thesisResult.exception.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Overall Remarks
                OutlinedTextField(
                    value = overallRemarks,
                    onValueChange = { overallRemarks = it },
                    label = { Text("Overall Remarks") },
                    placeholder = { Text("Provide general feedback about the thesis") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Inline Comments
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Inline Comments",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Button(
                            onClick = { showAddCommentDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Comment"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Comment")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (inlineComments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No inline comments added yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(inlineComments) { comment ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row {
                                            Text(
                                                text = "Page ${comment.pageNumber}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = comment.type,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier
                                                    .background(
                                                        color = when (comment.type) {
                                                            "Suggestion" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                                            "Correction" -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                                            "Question" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                                        },
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = comment.content,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    IconButton(
                                        onClick = { inlineComments.remove(comment) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Comment",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Comment Dialog
                if (showAddCommentDialog) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { showAddCommentDialog = false }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Add Comment",
                                style = MaterialTheme.typography.headlineSmall
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = commentPage,
                                onValueChange = { commentPage = it },
                                label = { Text("Page Number") },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            ExposedDropdownMenuBox(
                                expanded = isCommentTypeDropdownExpanded,
                                onExpandedChange = { isCommentTypeDropdownExpanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = commentType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Comment Type") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCommentTypeDropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )

                                ExposedDropdownMenu(
                                    expanded = isCommentTypeDropdownExpanded,
                                    onDismissRequest = { isCommentTypeDropdownExpanded = false }
                                ) {
                                    commentTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                commentType = type
                                                isCommentTypeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = commentContent,
                                onValueChange = { commentContent = it },
                                label = { Text("Comment") },
                                placeholder = { Text("Write your comment here") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { showAddCommentDialog = false }
                                ) {
                                    Text("Cancel")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        if (commentContent.isNotBlank() && commentPage.isNotBlank()) {
                                            val pageNum = commentPage.toIntOrNull() ?: 1
                                            inlineComments.add(
                                                InlineComment(
                                                    id = UUID.randomUUID().toString(),
                                                    pageNumber = pageNum,
                                                    position = CommentPosition(x = 0f, y = 0f), // Default position
                                                    content = commentContent,
                                                    type = commentType
                                                )
                                            )
                                            commentContent = ""
                                            commentPage = "1"
                                            commentType = "Suggestion"
                                            showAddCommentDialog = false
                                        }
                                    },
                                    enabled = commentContent.isNotBlank() && commentPage.isNotBlank()
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (isUpdate && feedbackId != null) {
                            viewModel.updateFeedback(
                                feedbackId = feedbackId,
                                overallRemarks = overallRemarks,
                                inlineComments = inlineComments
                            )
                        } else {
                            viewModel.addFeedback(
                                thesisId = thesisId,
                                overallRemarks = overallRemarks,
                                inlineComments = inlineComments
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = overallRemarks.isNotBlank()
                ) {
                    Text(if (isUpdate) "Update Feedback" else "Submit Feedback")
                }
            }

            // Loading States
            if ((isUpdate && updateFeedbackResult is NetworkResult.Loading) ||
                (!isUpdate && addFeedbackResult is NetworkResult.Loading)) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        }
    }
}