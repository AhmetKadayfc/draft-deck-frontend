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
import androidx.compose.ui.unit.dp
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.remote.handle
import com.example.draftdeck.data.remote.isLoading
import com.example.draftdeck.ui.components.DraftDeckAppBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PasswordResetCodeVerificationScreen(
    viewModel: AuthViewModel,
    onVerifySuccess: (String) -> Unit,
    onNavigateBack: () -> Unit,
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
    
    val verifyPasswordResetCodeState by viewModel.verifyPasswordResetCodeState.collectAsState()
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
    LaunchedEffect(verifyPasswordResetCodeState) {
        verifyPasswordResetCodeState.handle(
            onLoading = {
                // Only set loading to true if we've actually submitted the code
                if (code.length == 5) {
                    isLoading = true
                }
            },
            onSuccess = {
                isLoading = false
                // Show success message before navigating
                snackbarHostState.showSnackbar("Code verified successfully!")
                // Navigate to password reset screen with the verified code
                onVerifySuccess(code)
                viewModel.resetVerifyPasswordResetCodeState()
            },
            onError = { exception ->
                isLoading = false
                val errorMessage = exception.message ?: "Verification failed"
                snackbarHostState.showSnackbar(errorMessage)
                viewModel.resetVerifyPasswordResetCodeState()
            }
        )
    }
    
    // Remove the unnecessary initialization
    LaunchedEffect(Unit) {
        viewModel.resetVerifyPasswordResetCodeState()
    }

    Scaffold(
        topBar = {
            DraftDeckAppBar(
                title = "Verify Code",
                showBackButton = true,
                onBackClick = onNavigateBack
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
                text = "Password Reset Verification",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "We've sent a 5-digit code to ${currentEmail ?: "your email address"}. Please enter it below to reset your password.",
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
                // Create 5 single-digit input fields for the verification code
                for (i in 0 until 5) {
                    val isFilled = code.length > i
                    val char = if (isFilled) code[i].toString() else ""
                    val borderColor = if (isFilled) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.outline

                    // Code input box
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.5.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = char,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty()) {
                                    // Delete behavior - remove the last digit
                                    if (code.isNotEmpty()) {
                                        code = code.substring(0, code.length - 1)
                                        if (i > 0) {
                                            focusRequesters[i - 1].requestFocus()
                                        }
                                    }
                                } else {
                                    // Only accept digits
                                    val lastChar = newValue.last()
                                    if (lastChar.isDigit()) {
                                        if (i < code.length) {
                                            // Replace the existing digit at this position
                                            code = code.substring(0, i) + lastChar + code.substring(i + 1)
                                        } else {
                                            // Add a new digit
                                            code += lastChar
                                        }
                                        
                                        // Auto-advance to next field
                                        if (i < 4) {
                                            focusRequesters[i + 1].requestFocus()
                                        } else {
                                            // Last field, hide keyboard
                                            focusManager.clearFocus()
                                        }
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = if (i == 4) ImeAction.Done else ImeAction.Next
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequesters[i])
                        )
                    }
                    
                    if (i < 4) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val isCodeComplete = code.length == 5
            val buttonEnabled = isCodeComplete && !isLoading

            Button(
                onClick = {
                    // Verify the code
                    viewModel.verifyPasswordResetCode(code)
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
                    Text("Verify")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (canResend) {
                Text(
                    text = "Didn't receive the code? Resend",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(enabled = canResend && !isLoading) {
                            viewModel.requestPasswordReset(currentEmail ?: email ?: "")
                            countdown = 60
                            canResend = false
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