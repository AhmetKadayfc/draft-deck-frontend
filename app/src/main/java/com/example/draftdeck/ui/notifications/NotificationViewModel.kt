package com.example.draftdeck.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.draftdeck.data.model.Notification
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    val currentUser: StateFlow<User?> = getCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _notifications = MutableStateFlow<NetworkResult<List<Notification>>>(NetworkResult.Loading)
    val notifications: StateFlow<NetworkResult<List<Notification>>> = _notifications

    private val _markAsReadResult = MutableStateFlow<NetworkResult<Unit>?>(null)
    val markAsReadResult: StateFlow<NetworkResult<Unit>?> = _markAsReadResult

    fun loadNotifications() {
        viewModelScope.launch {
            _notifications.value = NetworkResult.Loading

            // Simulate API call with dummy data
            try {
                currentUser.value?.id?.let { userId ->
                    // Here we would normally call an API to get notifications
                    // For now, we'll just return some dummy data

                    val dummyNotifications = listOf(
                        Notification(
                            id = "1",
                            userId = userId,
                            title = "New Feedback",
                            content = "Dr. Smith has provided feedback on your thesis 'Analysis of Machine Learning Algorithms'",
                            type = "Feedback",
                            relatedItemId = "feedback123",
                            isRead = false,
                            createdDate = Date(System.currentTimeMillis() - 86400000) // 1 day ago
                        ),
                        Notification(
                            id = "2",
                            userId = userId,
                            title = "Thesis Status Updated",
                            content = "Your thesis 'Analysis of Machine Learning Algorithms' has been reviewed",
                            type = "StatusUpdate",
                            relatedItemId = "thesis123",
                            isRead = true,
                            createdDate = Date(System.currentTimeMillis() - 172800000) // 2 days ago
                        ),
                        Notification(
                            id = "3",
                            userId = userId,
                            title = "Submission Deadline Reminder",
                            content = "The final submission deadline for your thesis is approaching. Please ensure all revisions are complete.",
                            type = "Reminder",
                            relatedItemId = null,
                            isRead = false,
                            createdDate = Date(System.currentTimeMillis() - 259200000) // 3 days ago
                        )
                    )

                    _notifications.value = NetworkResult.Success(dummyNotifications)
                } ?: run {
                    _notifications.value = NetworkResult.Error(Exception("User not authenticated"))
                }
            } catch (e: Exception) {
                _notifications.value = NetworkResult.Error(e)
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            _markAsReadResult.value = NetworkResult.Loading

            try {
                // In a real app, we'd call an API to mark the notification as read
                // For now, we'll just update our local state

                val currentNotifications = _notifications.value
                if (currentNotifications is NetworkResult.Success) {
                    val updatedNotifications = currentNotifications.data.map { notification ->
                        if (notification.id == notificationId) {
                            notification.copy(isRead = true)
                        } else {
                            notification
                        }
                    }

                    _notifications.value = NetworkResult.Success(updatedNotifications)
                    _markAsReadResult.value = NetworkResult.Success(Unit)
                }
            } catch (e: Exception) {
                _markAsReadResult.value = NetworkResult.Error(e)
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            _markAsReadResult.value = NetworkResult.Loading

            try {
                // In a real app, we'd call an API to mark all notifications as read
                // For now, we'll just update our local state

                val currentNotifications = _notifications.value
                if (currentNotifications is NetworkResult.Success) {
                    val updatedNotifications = currentNotifications.data.map { notification ->
                        notification.copy(isRead = true)
                    }

                    _notifications.value = NetworkResult.Success(updatedNotifications)
                    _markAsReadResult.value = NetworkResult.Success(Unit)
                }
            } catch (e: Exception) {
                _markAsReadResult.value = NetworkResult.Error(e)
            }
        }
    }

    fun resetMarkAsReadResult() {
        _markAsReadResult.value = null
    }
}