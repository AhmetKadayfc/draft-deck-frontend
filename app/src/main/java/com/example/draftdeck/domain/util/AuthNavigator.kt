package com.example.draftdeck.domain.util

import androidx.navigation.NavController
import com.example.draftdeck.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton class that handles authentication-related navigation events across the app
 */
@Singleton
class AuthNavigator @Inject constructor(
    private val sessionManager: SessionManager
) {
    // Flow that can be collected to respond to auth events
    private val _authEvents = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val authEvents: SharedFlow<AuthEvent> = _authEvents.asSharedFlow()

    /**
     * Called when a 401 unauthorized response is received from the API
     * This will clear the session and emit an event to redirect to the login screen
     */
    suspend fun onUnauthorized() {
        // First clear the session
        sessionManager.clearSession()
        
        // Emit an event for the main activity to handle
        _authEvents.emit(AuthEvent.Unauthorized)
    }

    /**
     * Navigate to the login screen when an unauthorized event is received
     */
    fun navigateToLogin(navController: NavController) {
        // Navigate to login and clear the back stack
        navController.navigate(Screen.Login.route) {
            // Clear all the back stack so user can't go back to protected screens
            popUpTo(Screen.Welcome.route) { inclusive = true }
        }
    }
}

/**
 * Authentication related events that can occur in the app
 */
sealed class AuthEvent {
    // Emitted when the user is unauthorized (received a 401 response)
    object Unauthorized : AuthEvent()
} 