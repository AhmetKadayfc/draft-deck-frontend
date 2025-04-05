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

    private val _loginState = MutableStateFlow<NetworkResult<User>>(NetworkResult.Loading)
    val loginState: StateFlow<NetworkResult<User>> = _loginState

    private val _registerState = MutableStateFlow<NetworkResult<User>>(NetworkResult.Loading)
    val registerState: StateFlow<NetworkResult<User>> = _registerState

    private val _logoutState = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.Loading)
    val logoutState: StateFlow<NetworkResult<Unit>> = _logoutState
    
    private val _verifyEmailState = MutableStateFlow<NetworkResult<User>>(NetworkResult.Loading)
    val verifyEmailState: StateFlow<NetworkResult<User>> = _verifyEmailState
    
    private val _resendVerificationState = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.Loading)
    val resendVerificationState: StateFlow<NetworkResult<Unit>> = _resendVerificationState

    val currentUser: StateFlow<User?> = getCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLoggedIn: Flow<Boolean> = currentUser.map { it != null }
    
    // Store the email for verification purposes
    private val _currentEmail = MutableStateFlow<String?>(null)
    val currentEmail: StateFlow<String?> = _currentEmail
    
    // Method to set the current email directly
    fun setCurrentEmail(email: String) {
        _currentEmail.value = email
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
        _loginState.value = NetworkResult.Loading
    }

    fun resetRegisterState() {
        _registerState.value = NetworkResult.Loading
    }

    fun resetLogoutState() {
        _logoutState.value = NetworkResult.Loading
    }
    
    fun resetVerifyEmailState() {
        _verifyEmailState.value = NetworkResult.Loading
    }
    
    fun resetResendVerificationState() {
        _resendVerificationState.value = NetworkResult.Loading
    }
}