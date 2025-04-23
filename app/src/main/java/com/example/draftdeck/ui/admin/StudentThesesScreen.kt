package com.example.draftdeck.ui.admin

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.ui.components.DraftDeckAppBar
import com.example.draftdeck.ui.components.ErrorView
import com.example.draftdeck.ui.components.LoadingIndicator
import com.example.draftdeck.ui.thesis.components.ThesisCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentThesesScreen(
    studentId: String,
    viewModel: AdminViewModel,
    onBackClick: () -> Unit,
    onAssignAdvisor: (String) -> Unit,
    onNavigateToThesisDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val studentTheses by viewModel.studentThesesList.collectAsState()
    val students by viewModel.studentsList.collectAsState()
    val advisors by viewModel.advisorsList.collectAsState()
    val assignResult by viewModel.assignAdvisorResult.collectAsState()
    
    // For showing messages
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Track whether we're showing the assign advisor dialog
    var showAssignDialog by remember { mutableStateOf(false) }
    // Track the thesis ID for the thesis we're assigning an advisor to
    var currentThesisId by remember { mutableStateOf("") }
    // Track the selected advisor ID
    var selectedAdvisorId by remember { mutableStateOf("") }
    
    // Get the student info
    val student = if (students is NetworkResult.Success) {
        (students as NetworkResult.Success<List<User>>).data.find { it.id == studentId }
    } else null
    
    LaunchedEffect(studentId) {
        viewModel.loadStudentTheses(studentId)
        if (students !is NetworkResult.Success) {
            viewModel.loadStudents()
        }
        // Also load advisors in case we need to assign one
        if (advisors !is NetworkResult.Success) {
            viewModel.loadAdvisors()
        }
    }
    
    // Handle assignment results
    LaunchedEffect(assignResult) {
        when (assignResult) {
            is NetworkResult.Success -> {
                val thesis = (assignResult as NetworkResult.Success).data
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        "Successfully assigned ${thesis.advisorName} to thesis '${thesis.title}'"
                    )
                }
                // Reset the result state
                viewModel.resetAssignAdvisorResult()
            }
            is NetworkResult.Error -> {
                val error = (assignResult as NetworkResult.Error).exception
                Log.e("StudentThesesScreen", "Error assigning advisor: ${error.message}", error)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        "Failed to assign advisor: ${error.message ?: "Unknown error"}"
                    )
                }
                // Reset the result state
                viewModel.resetAssignAdvisorResult()
            }
            else -> {} // Do nothing for Loading or Idle
        }
    }
    
    Scaffold(
        topBar = {
            DraftDeckAppBar(
                title = "Theses - ${student?.firstName ?: ""} ${student?.lastName ?: ""}",
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
            when (val result = studentTheses) {
                is NetworkResult.Idle, is NetworkResult.Loading -> {
                    LoadingIndicator(fullScreen = true)
                }
                is NetworkResult.Success -> {
                    if (result.data.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No theses found for this student",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Show debugging info
                            Text(
                                text = "Student ID: $studentId",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            
                            Button(
                                onClick = { viewModel.loadStudentTheses(studentId) },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Refresh")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(result.data) { thesis ->
                                // Only show assign button for theses without an advisor
                                val showAssignButton = thesis.advisorId.isEmpty()
                                
                                ThesisCard(
                                    thesis = thesis,
                                    onClick = { onNavigateToThesisDetail(thesis.id) },
                                    onAssignButtonClick = if (showAssignButton) {
                                        {
                                            currentThesisId = thesis.id
                                            showAssignDialog = true
                                        }
                                    } else null,
                                    showAssignButton = showAssignButton,
                                    showStudentName = false
                                )
                            }
                        }
                    }
                }
                is NetworkResult.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        content = {
                            Text(
                                text = "Failed to load theses",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = result.exception.message ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Show debugging info
                            Text(
                                text = "Student ID: $studentId",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            
                            Button(
                                onClick = { viewModel.loadStudentTheses(studentId) },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Retry")
                            }
                        }
                    )
                }
            }
            
            // Show loading indicator when assigning an advisor
            if (assignResult is NetworkResult.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(message = "Assigning advisor...")
                }
            }
        }
    }
    
    // Show dialog to select an advisor when needed
    if (showAssignDialog) {
        val advisorsList = if (advisors is NetworkResult.Success) {
            (advisors as NetworkResult.Success<List<User>>).data
        } else {
            emptyList()
        }
        
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Assign Advisor") },
            text = {
                if (advisorsList.isEmpty()) {
                    if (advisors is NetworkResult.Loading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Loading advisors...")
                            LoadingIndicator()
                        }
                    } else {
                        Text("No advisors available")
                    }
                } else {
                    Column {
                        Text("Select an advisor to assign to this thesis:", 
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier.height(300.dp)
                        ) {
                            items(advisorsList) { advisor ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedAdvisorId == advisor.id,
                                        onClick = { selectedAdvisorId = advisor.id }
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Column {
                                        Text(
                                            text = "${advisor.firstName} ${advisor.lastName}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = advisor.email,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedAdvisorId.isNotEmpty()) {
                            Log.d("StudentThesesScreen", "Assigning advisor $selectedAdvisorId to thesis $currentThesisId")
                            viewModel.assignAdvisorToThesis(currentThesisId, selectedAdvisorId)
                            showAssignDialog = false
                            selectedAdvisorId = ""
                        }
                    },
                    enabled = selectedAdvisorId.isNotEmpty()
                ) {
                    Text("Assign")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showAssignDialog = false
                        selectedAdvisorId = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
} 