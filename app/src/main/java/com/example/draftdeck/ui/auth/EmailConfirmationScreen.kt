package com.example.draftdeck.ui.auth

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.draftdeck.ui.components.DraftDeckAppBar
import com.example.draftdeck.ui.theme.DraftDeckTheme
import kotlinx.coroutines.delay

@Composable
fun EmailConfirmationScreen(
    viewModel: AuthViewModel,
    onConfirmSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        canResend = true
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            DraftDeckAppBar(
                title = "Email Confirmation",
                showBackButton = false
            )
        }
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
                text = "Email confirm",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "We've sent a 5-digit code to your email address. Please enter it below to verify your account.",
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
                                    code.replaceRange(maxOf(0, index), minOf(index + 1, code.length), "")
                                } else {
                                    val currentCode = if (index >= code.length) code.padEnd(index, ' ') else code
                                    currentCode.replaceRange(index, minOf(index + 1, currentCode.length), newValue)
                                }

                                // Move focus if needed
                                if (newValue.isNotEmpty() && index < 4) {
                                    // Move to next field
                                } else if (newValue.isEmpty() && index > 0) {
                                    // Move to previous field
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
                            .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
                    )

                    if (index < 4) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // Here we would normally verify the code
                    // For now, just simulate success
                    onConfirmSuccess()
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                enabled = code.length == 5
            ) {
                Text("Confirm")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (canResend) "Didn't receive the code? Resend" else "Resend code in $countdown seconds",
                style = MaterialTheme.typography.bodyMedium,
                color = if (canResend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
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