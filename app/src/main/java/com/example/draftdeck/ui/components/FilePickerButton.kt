package com.example.draftdeck.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.draftdeck.ui.theme.DraftDeckTheme

@Composable
fun FilePickerButton(
    onFileSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "Select File",
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    mimeTypes: String = "application/*"
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                onFileSelected(it)
            }
        }
    )

    Button(
        onClick = { launcher.launch(mimeTypes) },
        modifier = modifier,
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = "Select File"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = buttonText)
        }
    }
}

@Preview
@Composable
fun FilePickerButtonPreview() {
    DraftDeckTheme {
        FilePickerButton(
            onFileSelected = {}
        )
    }
}