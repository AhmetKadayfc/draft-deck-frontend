package com.example.draftdeck.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.domain.usecase.auth.GetCurrentUserUseCase
import com.example.draftdeck.domain.usecase.auth.LoginUseCase
import com.example.draftdeck.domain.usecase.auth.LogoutUseCase
import com.example.draftdeck.domain.usecase.auth.RegisterUseCase
import com.example.draftdeck.domain.usecase.auth.ResendVerificationUseCase
import com.example.draftdeck.domain.usecase.auth.VerifyEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val verifyEmailUseCase: VerifyEmailUseCase,
    private val resendVerificationUseCase: ResendVerificationUseCase
) : ViewModel() {

    private val _loginState = MutableStateFlow<NetworkResult<User>>(NetworkResult.Idle)
    val loginState: StateFlow<NetworkResult<User>> = _loginState

    private val _registerState = MutableStateFlow<NetworkResult<User>>(NetworkResult.Idle)
    val registerState: StateFlow<NetworkResult<User>> = _registerState

    private val _logoutState = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.Idle)
    val logoutState: StateFlow<NetworkResult<Unit>> = _logoutState
    
    private val _verifyEmailState = MutableStateFlow<NetworkResult<User>>(NetworkResult.Idle)
    val verifyEmailState: StateFlow<NetworkResult<User>> = _verifyEmailState
    
    private val _resendVerificationState = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.Idle)
    val resendVerificationState: StateFlow<NetworkResult<Unit>> = _resendVerificationState
    
    // Password reset states
    private val _requestPasswordResetState = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.Idle)
    val requestPasswordResetState: StateFlow<NetworkResult<Unit>> = _requestPasswordResetState
    
    private val _verifyPasswordResetCodeState = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.Idle)
    val verifyPasswordResetCodeState: StateFlow<NetworkResult<Unit>> = _verifyPasswordResetCodeState
    
    private val _resetPasswordState = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.Idle)
    val resetPasswordState: StateFlow<NetworkResult<Unit>> = _resetPasswordState

    val currentUser: StateFlow<User?> = getCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLoggedIn: Flow<Boolean> = currentUser.map { it != null }
    
    // Store the email for verification purposes
    private val _currentEmail = MutableStateFlow<String?>(null)
    val currentEmail: StateFlow<String?> = _currentEmail
    
    // Store the verification code after it's verified
    private val _verificationCode = MutableStateFlow<String?>(null)
    val verificationCode: StateFlow<String?> = _verificationCode
    
    // Method to set the current email directly
    fun setCurrentEmail(email: String) {
        _currentEmail.value = email
    }
    
    // Method to set the verification code
    fun setVerificationCode(code: String) {
        _verificationCode.value = code
    }

    fun login(email: String, password: String) {
        _currentEmail.value = email
        viewModelScope.launch {
            loginUseCase(email, password).collectLatest { result ->
                _loginState.value = result
            }
        }
    }

    fun register(email: String, password: String, firstName: String, lastName: String, role: String) {
        _currentEmail.value = email
        viewModelScope.launch {
            registerUseCase(email, password, firstName, lastName, role).collectLatest { result ->
                _registerState.value = result
            }
        }
    }
    
    fun register(email: String, password: String, firstName: String, lastName: String, role: String, studentId: String?) {
        _currentEmail.value = email
        viewModelScope.launch {
            registerUseCase(email, password, firstName, lastName, role, studentId).collectLatest { result ->
                _registerState.value = result
            }
        }
    }
    
    fun verifyEmail(code: String) {
        val email = _currentEmail.value ?: return
        viewModelScope.launch {
            verifyEmailUseCase(email, code).collectLatest { result ->
                _verifyEmailState.value = result
            }
        }
    }
    
    fun resendVerification() {
        val email = _currentEmail.value ?: return
        viewModelScope.launch {
            resendVerificationUseCase(email).collectLatest { result ->
                _resendVerificationState.value = result
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase().collectLatest { result ->
                _logoutState.value = result
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = NetworkResult.Idle
    }

    fun resetRegisterState() {
        _registerState.value = NetworkResult.Idle
    }

    fun resetLogoutState() {
        _logoutState.value = NetworkResult.Idle
    }
    
    fun resetVerifyEmailState() {
        _verifyEmailState.value = NetworkResult.Idle
    }
    
    fun resetResendVerificationState() {
        _resendVerificationState.value = NetworkResult.Idle
    }

    fun requestPasswordReset(email: String) {
        _currentEmail.value = email
        viewModelScope.launch {
            val repository = loginUseCase.getRepository() // Assuming you have access to repository via use case
            repository.resetPassword(email).collectLatest { result ->
                _requestPasswordResetState.value = result
            }
        }
    }
    
    fun verifyPasswordResetCode(code: String) {
        val email = _currentEmail.value ?: return
        viewModelScope.launch {
            val repository = loginUseCase.getRepository()
            repository.verifyPasswordResetCode(email, code).collectLatest { result ->
                _verifyPasswordResetCodeState.value = result
                if (result is NetworkResult.Success) {
                    // Save the code for the next step
                    setVerificationCode(code)
                }
            }
        }
    }
    
    fun resetPassword(newPassword: String) {
        val email = _currentEmail.value ?: return
        val code = _verificationCode.value ?: return
        viewModelScope.launch {
            val repository = loginUseCase.getRepository()
            repository.completePasswordReset(email, newPassword, code).collectLatest { result ->
                _resetPasswordState.value = result
            }
        }
    }
    
    // Reset state methods for password reset flows
    fun resetRequestPasswordResetState() {
        _requestPasswordResetState.value = NetworkResult.Idle
    }
    
    fun resetVerifyPasswordResetCodeState() {
        _verifyPasswordResetCodeState.value = NetworkResult.Idle
    }
    
    fun resetPasswordResetState() {
        _resetPasswordState.value = NetworkResult.Idle
    }
    
    /**
     * Check if the user is authenticated and token is valid
     * This can be called when entering screens that require authentication
     * @return true if authenticated, false otherwise
     */
    fun checkAuthState(): Boolean {
        // Simply check if we have a current user
        val isAuthenticated = currentUser.value != null
        
        if (!isAuthenticated) {
            // Log or notify that user is not authenticated
            // In a real app, you might want to auto-redirect to login
        }
        
        return isAuthenticated
    }
}