package com.example.draftdeck.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.remote.handle
import com.example.draftdeck.data.remote.isLoading
import com.example.draftdeck.ui.components.DraftDeckAppBar
import com.example.draftdeck.ui.components.LoadingIndicator

@Composable
fun ResetPasswordScreen(
    viewModel: AuthViewModel,
    onResetSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    email: String? = null,
    code: String? = null,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // Set the email in the ViewModel immediately to avoid state issues
    if (email != null && email.isNotEmpty()) {
        viewModel.setCurrentEmail(email)
    }
    
    // Set the verification code in the ViewModel immediately
    if (code != null && code.isNotEmpty()) {
        viewModel.setVerificationCode(code)
    }
    
    // Also set them in a LaunchedEffect for safety
    LaunchedEffect(email, code) {
        if (email != null && email.isNotEmpty()) {
            viewModel.setCurrentEmail(email)
        }
        if (code != null && code.isNotEmpty()) {
            viewModel.setVerificationCode(code)
        }
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val resetPasswordState by viewModel.resetPasswordState.collectAsState()
    val currentEmail by viewModel.currentEmail.collectAsState()
    val verificationCode by viewModel.verificationCode.collectAsState()
    
    LaunchedEffect(resetPasswordState) {
        resetPasswordState.handle(
            onLoading = {
                isLoading = true
            },
            onSuccess = {
                isLoading = false
                snackbarHostState.showSnackbar("Password reset successfully")
                viewModel.resetResetPasswordState()
                onResetSuccess()
            },
            onError = { exception ->
                isLoading = false
                val error = exception.message ?: "Failed to reset password"
                isError = true
                errorMessage = error
                snackbarHostState.showSnackbar(error)
                viewModel.resetResetPasswordState()
            }
        )
    }
    
    Scaffold(
        topBar = {
            DraftDeckAppBar(
                title = "Reset Password",
                showBackButton = true,
                onBackClick = onNavigateBack
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
                    text = "Create New Password",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Enter and confirm your new password",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; isError = false },
                    label = { Text("New Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Password"
                        )
                    },
                    isError = isError,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; isError = false },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Confirm Password"
                        )
                    },
                    isError = isError,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Debug text to show email that will be used
                Text(
                    text = "Using email: ${currentEmail ?: "None"} | Code: ${if (verificationCode != null) "✓" else "✗"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        when {
                            password.isBlank() || confirmPassword.isBlank() -> {
                                isError = true
                                errorMessage = "Please fill in all fields"
                            }
                            password != confirmPassword -> {
                                isError = true
                                errorMessage = "Passwords do not match"
                            }
                            password.length < 6 -> {
                                isError = true
                                errorMessage = "Password must be at least 6 characters"
                            }
                            currentEmail == null -> {
                                isError = true
                                errorMessage = "Email not found. Please start from the forgot password screen"
                            }
                            verificationCode == null -> {
                                isError = true
                                errorMessage = "Verification code not found. Please complete the verification step first"
                            }
                            else -> {
                                viewModel.resetPassword(password)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("Reset Password")
                }
            }
            
            if (isLoading) {
                LoadingIndicator(fullScreen = true)
            }
        }
    }
} 