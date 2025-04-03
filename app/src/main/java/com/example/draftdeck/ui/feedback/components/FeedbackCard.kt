package com.example.draftdeck.ui.feedback.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Person
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
import com.example.draftdeck.data.model.CommentPosition
import com.example.draftdeck.data.model.Feedback
import com.example.draftdeck.data.model.InlineComment
import com.example.draftdeck.ui.theme.DraftDeckTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeedbackCard(
    feedback: Feedback,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
                    imageVector = Icons.Default.Person,
                    contentDescription = "Advisor",
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = feedback.advisorName,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = formatDate(feedback.createdDate),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = feedback.overallRemarks,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Comment,
                    contentDescription = "Comments",
                    tint = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${feedback.inlineComments.size} inline comments",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: InlineComment,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page ${comment.pageNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = comment.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (comment.type) {
                        "Suggestion" -> MaterialTheme.colorScheme.secondary
                        "Correction" -> MaterialTheme.colorScheme.error
                        "Question" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}

@Preview
@Composable
fun FeedbackCardPreview() {
    val sampleFeedback = Feedback(
        id = "1",
        thesisId = "thesis1",
        advisorId = "advisor1",
        advisorName = "Dr. Smith",
        overallRemarks = "This thesis shows good understanding of the topic but needs improvements in methodology and literature review.",
        inlineComments = listOf(
            InlineComment(
                id = "c1",
                pageNumber = 1,
                position = CommentPosition(x = 100f, y = 150f),
                content = "The introduction needs to be more specific about research objectives.",
                type = "Suggestion"
            ),
            InlineComment(
                id = "c2",
                pageNumber = 2,
                position = CommentPosition(x = 200f, y = 250f),
                content = "Check this citation format.",
                type = "Correction"
            )
        ),
        status = "Completed",
        createdDate = Date()
    )

    DraftDeckTheme {
        FeedbackCard(
            feedback = sampleFeedback,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun CommentItemPreview() {
    val sampleComment = InlineComment(
        id = "c1",
        pageNumber = 1,
        position = CommentPosition(x = 100f, y = 150f),
        content = "The introduction needs to be more specific about research objectives.",
        type = "Suggestion"
    )

    DraftDeckTheme {
        CommentItem(
            comment = sampleComment
        )
    }
}