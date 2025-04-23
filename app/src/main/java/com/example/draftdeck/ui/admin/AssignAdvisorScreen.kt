package com.example.draftdeck.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.ui.components.DraftDeckAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignAdvisorScreen(
    thesisId: String,
    viewModel: AdminViewModel,
    onBackClick: () -> Unit,
    onAssignSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val assignResult by viewModel.assignAdvisorResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(assignResult) {
        if (assignResult is NetworkResult.Success) {
            onAssignSuccess()
            viewModel.resetAssignAdvisorResult()
        } else if (assignResult is NetworkResult.Error) {
            snackbarHostState.showSnackbar(
                "Failed to assign thesis: ${(assignResult as NetworkResult.Error).exception.message}"
            )
            viewModel.resetAssignAdvisorResult()
        }
    }

    Scaffold(
        topBar = {
            DraftDeckAppBar(
                title = "Assign Thesis to Yourself",
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "You (${currentUser?.firstName} ${currentUser?.lastName}) will be assigned as the advisor for this thesis.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        // The current user's ID will be used by the API
                        currentUser?.id?.let { advisorId ->
                            viewModel.assignAdvisorToThesis(thesisId, advisorId)
                        }
                    },
                    enabled = currentUser != null && assignResult !is NetworkResult.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (assignResult is NetworkResult.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Assign Thesis to Me")
                }
            }
        }
    }
}

@Composable
fun AdvisorSelectionItem(
    advisor: User,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${advisor.firstName} ${advisor.lastName}",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = advisor.email,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Assigned Theses: ${advisor.thesisCount}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
} 