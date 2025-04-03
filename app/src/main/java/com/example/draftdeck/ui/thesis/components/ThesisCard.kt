package com.example.draftdeck.ui.thesis.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.domain.util.Constants
import com.example.draftdeck.ui.theme.DraftDeckTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ThesisCard(
    thesis: Thesis,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showStudentName: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Thesis",
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = thesis.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = thesis.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThesisStatusBadge(status = thesis.status)

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "v${thesis.version} | ${thesis.submissionType}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = formatDate(thesis.lastUpdated),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (showStudentName) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Student: ${thesis.studentName}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ThesisStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status) {
        Constants.STATUS_PENDING -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        Constants.STATUS_REVIEWED -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        Constants.STATUS_APPROVED -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        Constants.STATUS_REJECTED -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        else -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    androidx.compose.material3.Surface(
        color = backgroundColor,
        contentColor = textColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = status.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            },
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}

@Preview
@Composable
fun ThesisCardPreview() {
    val sampleThesis = Thesis(
        id = "1",
        title = "Analysis of Machine Learning Algorithms",
        description = "This thesis explores various machine learning algorithms and their applications in solving real-world problems.",
        studentId = "s1",
        studentName = "John Doe",
        advisorId = "a1",
        advisorName = "Dr. Smith",
        submissionType = "Draft",
        fileUrl = "url",
        fileType = "pdf",
        version = 1,
        status = Constants.STATUS_PENDING,
        submissionDate = Date(),
        lastUpdated = Date()
    )

    DraftDeckTheme {
        ThesisCard(
            thesis = sampleThesis,
            onClick = {},
            showStudentName = true
        )
    }
}