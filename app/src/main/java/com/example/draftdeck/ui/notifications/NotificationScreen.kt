package com.example.draftdeck.ui.notifications

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.draftdeck.data.model.Notification
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.ui.components.DraftDeckAppBar
import com.example.draftdeck.ui.components.ErrorView
import com.example.draftdeck.ui.components.LoadingIndicator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel,
    onBackClick: () -> Unit,
    onNavigateToRelatedItem: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    Scaffold(
        topBar = {
            DraftDeckAppBar(
                title = "Notifications",
                showBackButton = true,
                onBackClick = onBackClick,
                actions = {
                    TextButton(
                        onClick = { viewModel.markAllNotificationsAsRead() }
                    ) {
                        Text(
                            text = "Mark All Read",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val result = notifications) {
                is NetworkResult.Loading -> {
                    LoadingIndicator(fullScreen = true)
                }
                is NetworkResult.Success -> {
                    if (result.data.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "No Notifications",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "No notifications",
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "You're all caught up!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(result.data) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    onMarkAsRead = { viewModel.markNotificationAsRead(notification.id) },
                                    onItemClick = {
                                        notification.relatedItemId?.let { itemId ->
                                            onNavigateToRelatedItem(notification.type, itemId)
                                        }
                                        if (!notification.isRead) {
                                            viewModel.markNotificationAsRead(notification.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                is NetworkResult.Error -> {
                    ErrorView(
                        message = "Failed to load notifications: ${result.exception.message}",
                        onRetry = { viewModel.loadNotifications() }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onMarkAsRead: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .let {
                if (!notification.isRead) {
                    it.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                } else {
                    it
                }
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!notification.isRead) 2.dp else 1.dp
        ),
        onClick = onItemClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Notification indicator
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = formatNotificationDate(notification.createdDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notification.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!notification.isRead) {
                    TextButton(
                        onClick = onMarkAsRead
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Mark as Read"
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text("Mark as Read")
                    }
                }
            }
        }
    }
}

private fun formatNotificationDate(date: Date): String {
    val now = Date()
    val diffInMillis = now.time - date.time
    val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

    return when {
        diffInDays < 1 -> {
            val diffInHours = diffInMillis / (1000 * 60 * 60)
            if (diffInHours < 1) {
                val diffInMinutes = diffInMillis / (1000 * 60)
                if (diffInMinutes < 1) {
                    "Just now"
                } else {
                    "$diffInMinutes min ago"
                }
            } else {
                "$diffInHours hours ago"
            }
        }
        diffInDays < 2 -> "Yesterday"
        diffInDays < 7 -> "$diffInDays days ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            formatter.format(date)
        }
    }
}