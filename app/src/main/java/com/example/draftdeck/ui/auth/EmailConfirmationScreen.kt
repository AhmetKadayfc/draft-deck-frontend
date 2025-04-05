package com.example.draftdeck.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.ui.components.DraftDeckAppBar
import com.example.draftdeck.ui.theme.DraftDeckTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailConfirmationScreen(
    viewModel: AuthViewModel,
    onConfirmSuccess: () -> Unit,
    email: String? = null,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Create focus requesters for each input field
    val focusRequesters = remember {
        List(5) { FocusRequester() }
    }
    
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Set the email in the ViewModel immediately to avoid state issues
    if (email != null && email.isNotEmpty()) {
        viewModel.setCurrentEmail(email)
    }
    
    // Also set it in a LaunchedEffect for safety
    LaunchedEffect(email) {
        if (email != null && email.isNotEmpty()) {
            viewModel.setCurrentEmail(email)
        }
    }
    
    val verifyEmailState by viewModel.verifyEmailState.collectAsState()
    val resendVerificationState by viewModel.resendVerificationState.collectAsState()
    val currentEmail by viewModel.currentEmail.collectAsState()

    // Start countdown for resend option
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        canResend = true
    }

    // Request focus to the first input field
    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
    
    // Handle verification result
    LaunchedEffect(verifyEmailState) {
        when (verifyEmailState) {
            is NetworkResult.Loading -> {
                // Only set loading to true if we've actually submitted the code
                if (code.length == 5) {
                    isLoading = true
                }
            }
            is NetworkResult.Success -> {
                isLoading = false
                // Show success message before navigating
                snackbarHostState.showSnackbar("Email verified successfully!")
                // Navigate to login screen
                onConfirmSuccess()
                viewModel.resetVerifyEmailState()
            }
            is NetworkResult.Error -> {
                isLoading = false
                val errorMessage = (verifyEmailState as NetworkResult.Error).exception.message ?: "Verification failed"
                snackbarHostState.showSnackbar(errorMessage)
                viewModel.resetVerifyEmailState()
            }
        }
    }
    
    // Handle resend verification result
    LaunchedEffect(resendVerificationState) {
        when (resendVerificationState) {
            is NetworkResult.Loading -> {
                // Only set loading true if actively resending
                if (canResend) {
                    isLoading = true
                }
            }
            is NetworkResult.Success -> {
                isLoading = false
                snackbarHostState.showSnackbar("Verification code resent")
                // Reset the countdown
                countdown = 60
                canResend = false
                viewModel.resetResendVerificationState()
            }
            is NetworkResult.Error -> {
                isLoading = false
                val errorMessage = (resendVerificationState as NetworkResult.Error).exception.message ?: "Failed to resend code"
                snackbarHostState.showSnackbar(errorMessage)
                viewModel.resetResendVerificationState()
            }
        }
    }

    // Initialize to not loading on compose
    LaunchedEffect(Unit) {
        isLoading = false
        viewModel.resetVerifyEmailState()
        viewModel.resetResendVerificationState()
    }

    Scaffold(
        topBar = {
            DraftDeckAppBar(
                title = "Email Confirmation",
                showBackButton = false
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Email Confirmation",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "We've sent a 5-digit code to ${currentEmail ?: "your email address"}. Please enter it below to verify your account.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Enter Verification Code",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { index ->
                    val char = code.getOrNull(index)?.toString() ?: ""

                    BasicTextField(
                        value = char,
                        onValueChange = { newValue ->
                            if (newValue.length <= 1) {
                                // Update the digit at the current position
                                code = if (newValue.isEmpty()) {
                                    // Move focus to previous field when deleting
                                    if (index > 0) {
                                        scope.launch {
                                            focusRequesters[index - 1].requestFocus()
                                        }
                                    }
                                    code.replaceRange(maxOf(0, index), minOf(index + 1, code.length), "")
                                } else {
                                    val currentCode = if (index >= code.length) code.padEnd(index, ' ') else code
                                    val updatedCode = currentCode.replaceRange(index, minOf(index + 1, currentCode.length), newValue)
                                    
                                    // Move focus to next field when digit is entered
                                    if (index < 4) {
                                        scope.launch {
                                            focusRequesters[index + 1].requestFocus()
                                        }
                                    } else {
                                        // Clear focus when the last digit is entered
                                        focusManager.clearFocus()
                                    }
                                    
                                    updatedCode
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = if (index == 4) ImeAction.Done else ImeAction.Next
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                            .focusRequester(focusRequesters[index])
                    )

                    if (index < 4) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Calculate button enabled state based on code length
            val isEmailValid = currentEmail != null || email != null
            val isCodeComplete = code.length == 5
            val buttonEnabled = isCodeComplete && !isLoading && isEmailValid
            
            // Debug logging for state
            LaunchedEffect(code) {
                if (isCodeComplete) {
                    // Reset state if the user re-enters a complete code
                    if (verifyEmailState is NetworkResult.Error) {
                        viewModel.resetVerifyEmailState()
                    }
                }
            }

            Button(
                onClick = {
                    // Verify the email with the entered code
                    viewModel.verifyEmail(code)
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                enabled = buttonEnabled
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Confirm")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (canResend) {
                Text(
                    text = "Didn't receive the code? Resend",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(enabled = canResend && !isLoading) {
                            viewModel.resendVerification()
                        }
                )
            } else {
                Text(
                    text = "Resend code in $countdown seconds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmailConfirmationScreenPreview() {
    // This preview won't work properly due to the view model dependency
    // but it's useful for layout checks
    DraftDeckTheme {
        // EmailConfirmationScreen(viewModel = viewModel, onConfirmSuccess = {})
    }
}