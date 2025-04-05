package com.example.draftdeck.ui.auth

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.ui.components.DraftDeckAppBar
import com.example.draftdeck.ui.components.ErrorView
import com.example.draftdeck.ui.components.LoadingIndicator
import com.example.draftdeck.ui.theme.DraftDeckTheme

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToEmailVerification: (String) -> Unit,
    email: String? = null,
    fromVerification: Boolean = false,
    modifier: Modifier = Modifier
) {
    var emailState by remember { 
        mutableStateOf(
            when {
                email == null || email.isEmpty() || email == "{email}" -> ""
                else -> email
            }
        ) 
    }
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf(fromVerification) }

    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        when (loginState) {
            is NetworkResult.Success -> {
                onLoginSuccess()
                viewModel.resetLoginState()
            }
            is NetworkResult.Error -> {
                val error = (loginState as NetworkResult.Error).exception.message ?: "Login failed"
                
                if (error.contains("403", ignoreCase = true)) {
                    val emailToVerify = emailState.takeIf { it.isNotEmpty() } ?: email
                    
                    if (!emailToVerify.isNullOrEmpty()) {
                        onNavigateToEmailVerification(emailToVerify)
                        viewModel.resetLoginState()
                        return@LaunchedEffect
                    }
                }
                
                isError = true
                errorMessage = error
                showSuccessMessage = false
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            DraftDeckAppBar(
                title = "Login",
                showBackButton = true,
                onBackClick = onNavigateToRegister
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Draft Deck",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                if (showSuccessMessage) {
                    Text(
                        text = "Your email has been successfully verified! Please log in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = emailState,
                    onValueChange = { emailState = it; isError = false },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = "Email"
                        )
                    },
                    isError = isError,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; isError = false },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
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

                if (isError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (emailState.isBlank() || password.isBlank()) {
                            isError = true
                            errorMessage = "Please fill in all fields"
                        } else {
                            viewModel.login(emailState, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Login")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Password forgotten?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { /* Handle password reset */ }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Don't have an account? Register",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            if (loginState is NetworkResult.Loading) {
                LoadingIndicator(fullScreen = true)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    // This preview won't work properly due to the view model dependency
    // but it's useful for layout checks
    DraftDeckTheme {
        // LoginScreen(viewModel = viewModel, onLoginSuccess = {}, onNavigateToRegister = {})
    }
}