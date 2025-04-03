package com.example.draftdeck.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.domain.usecase.auth.GetCurrentUserUseCase
import com.example.draftdeck.domain.usecase.auth.LoginUseCase
import com.example.draftdeck.domain.usecase.auth.LogoutUseCase
import com.example.draftdeck.domain.usecase.auth.RegisterUseCase
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
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _loginState = MutableStateFlow<NetworkResult<User>>(NetworkResult.Loading)
    val loginState: StateFlow<NetworkResult<User>> = _loginState

    private val _registerState = MutableStateFlow<NetworkResult<User>>(NetworkResult.Loading)
    val registerState: StateFlow<NetworkResult<User>> = _registerState

    private val _logoutState = MutableStateFlow<NetworkResult<Unit>>(NetworkResult.Loading)
    val logoutState: StateFlow<NetworkResult<Unit>> = _logoutState

    val currentUser: StateFlow<User?> = getCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLoggedIn: Flow<Boolean> = currentUser.map { it != null }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            loginUseCase(email, password).collectLatest { result ->
                _loginState.value = result
            }
        }
    }

    fun register(email: String, password: String, name: String, surname: String, role: String) {
        viewModelScope.launch {
            registerUseCase(email, password, name, surname, role).collectLatest { result ->
                _registerState.value = result
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
}