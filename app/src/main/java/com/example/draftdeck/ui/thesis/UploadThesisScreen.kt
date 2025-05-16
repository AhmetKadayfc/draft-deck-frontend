package com.example.draftdeck.ui.thesis

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.domain.util.Constants
import com.example.draftdeck.ui.components.DraftDeckAppBar
import com.example.draftdeck.ui.components.FilePickerButton
import com.example.draftdeck.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadThesisScreen(
    thesisId: String? = null,
    onBackClick: () -> Unit,
    onSuccessNavigate: () -> Unit,
    viewModel: ThesisViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val uploadResult by viewModel.uploadThesisResult.collectAsState()
    val updateResult by viewModel.updateThesisResult.collectAsState()
    
    val thesisDetails by viewModel.thesisDetails.collectAsState()
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var submissionType by remember { mutableStateOf(Constants.THESIS_TYPE_DRAFT_DISPLAY) } // Default to Draft
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    
    // Flag to determine if we're updating an existing thesis
    val isUpdate = thesisId != null
    
    // Check authentication
    LaunchedEffect(Unit) {
        if (!viewModel.checkAuthentication()) {
            // Should be handled by auth interceptor, but add this as a fallback
//            onBackClick()
        }
    }
    
    // If we're editing an existing thesis, load its details
    LaunchedEffect(thesisId) {
        if (isUpdate && thesisId != null) {
            viewModel.loadThesisDetails(thesisId)
        }
    }
    
    // When thesis details are loaded, update the form
    LaunchedEffect(thesisDetails) {
        if (isUpdate && thesisDetails is NetworkResult.Success) {
            val thesis = (thesisDetails as NetworkResult.Success<Thesis>).data
            title = thesis.title
            description = thesis.description
            
            // Convert API submission type to display format if needed
            submissionType = when(thesis.submissionType.lowercase()) {
                Constants.THESIS_TYPE_DRAFT -> Constants.THESIS_TYPE_DRAFT_DISPLAY
                Constants.THESIS_TYPE_FINAL -> Constants.THESIS_TYPE_FINAL_DISPLAY
                else -> thesis.submissionType
            }
        }
    }
    
    // Handle successful upload/update
    LaunchedEffect(uploadResult, updateResult) {
        if (uploadResult is NetworkResult.Success) {
            viewModel.resetUploadResult()
            onSuccessNavigate()
        } 
        else if (updateResult is NetworkResult.Success) {
            viewModel.resetUpdateResult()
            onSuccessNavigate()
        }
        // Do nothing for other states - don't navigate away
    }

    // File picker
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            // Get the file name from the Uri
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    selectedFileName = cursor.getString(nameIndex)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            DraftDeckAppBar(
                title = if (isUpdate) "Update Thesis" else "Upload Thesis",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Thesis Title") },
                    isError = title.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    isError = description.isBlank(),
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = submissionType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Submission Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(Constants.THESIS_TYPE_DRAFT_DISPLAY) },
                            onClick = {
                                submissionType = Constants.THESIS_TYPE_DRAFT_DISPLAY
                                isDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(Constants.THESIS_TYPE_FINAL_DISPLAY) },
                            onClick = {
                                submissionType = Constants.THESIS_TYPE_FINAL_DISPLAY
                                isDropdownExpanded = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilePickerButton(
                        onFileSelected = { uri ->
                            selectedFileUri = uri
                            // Get the file name from the Uri
                            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (cursor.moveToFirst() && nameIndex >= 0) {
                                    selectedFileName = cursor.getString(nameIndex)
                                }
                            }
                        },
                        buttonText = if (isUpdate) "Replace File" else "Select Thesis File",
                        modifier = Modifier.weight(1f),
                        mimeTypes = "*/*"
                    )

                    Spacer(modifier = Modifier.weight(0.2f))

                    if (selectedFileName != null) {
                        Text(
                            text = selectedFileName!!,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    } else if (isUpdate) {
                        Text(
                            text = "Original file will be kept",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (selectedFileName == null && !isUpdate) {
                    Text(
                        text = "Please select a PDF or DOCX file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (isUpdate) {
                            thesisId?.let {
                                viewModel.updateThesis(
                                    context = context,
                                    thesisId = it,
                                    title = title,
                                    description = description,
                                    submissionType = submissionType,
                                    fileUri = selectedFileUri
                                )
                            }
                        } else {
                            currentUser?.id?.let { studentId ->
                                selectedFileUri?.let { uri ->
                                    // For simplicity, we're using a hardcoded advisor ID here
                                    // In a real app, you would let the user select from a list of advisors
                                    val advisorId = "advisor123"

                                    viewModel.uploadThesis(
                                        context = context,
                                        title = title,
                                        description = description,
                                        fileUri = uri,
                                        submissionType = submissionType,
                                        advisorId = advisorId
                                    )
                                }
                            }
                        }
                    },
                    enabled = title.isNotBlank() && description.isNotBlank() &&
                            (isUpdate || selectedFileUri != null),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isUpdate) "Update Thesis" else "Upload Thesis")
                }
            }

            if ((isUpdate && updateResult is NetworkResult.Loading) ||
                (!isUpdate && uploadResult is NetworkResult.Loading)) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
            
            // Error display
            if ((isUpdate && updateResult is NetworkResult.Error) ||
                (!isUpdate && uploadResult is NetworkResult.Error)) {
                
                val error = if (isUpdate) 
                    (updateResult as NetworkResult.Error).exception
                else 
                    (uploadResult as NetworkResult.Error).exception
                
                val isAuthError = error.message?.contains("authentication", ignoreCase = true) == true ||
                    error.message?.contains("authenticated", ignoreCase = true) == true ||
                    error.message?.contains("401", ignoreCase = true) == true ||
                    error.message?.contains("403", ignoreCase = true) == true
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (isAuthError) "Authentication Error" else error.message ?: "An error occurred",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (isAuthError) 
                                   "There was a problem with your authentication. Try going back and returning to this screen."
                                   else "Please try again or contact support if the problem persists.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onBackClick
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}