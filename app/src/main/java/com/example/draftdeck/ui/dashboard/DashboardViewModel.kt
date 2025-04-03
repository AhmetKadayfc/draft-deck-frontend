package com.example.draftdeck.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.domain.usecase.auth.GetCurrentUserUseCase
import com.example.draftdeck.domain.usecase.thesis.GetThesisListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class DashboardViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getThesisListUseCase: GetThesisListUseCase
) : ViewModel() {

    open val currentUser: StateFlow<User?> = getCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _thesisList = MutableStateFlow<NetworkResult<List<Thesis>>>(NetworkResult.Loading)
    open val thesisList: StateFlow<NetworkResult<List<Thesis>>> = _thesisList

    open fun loadThesisList() {
        viewModelScope.launch {
            currentUser.collectLatest { user ->
                user?.let {
                    getThesisListUseCase(it).collectLatest { result ->
                        _thesisList.value = result
                    }
                }
            }
        }
    }

    open fun filterThesisByStatus(status: String?) {
        val currentThesisList = _thesisList.value
        if (currentThesisList is NetworkResult.Success) {
            val filteredList = if (status == null) {
                currentThesisList.data
            } else {
                currentThesisList.data.filter { it.status == status }
            }
            _thesisList.value = NetworkResult.Success(filteredList)
        }
    }

    open fun refreshThesisList() {
        loadThesisList()
    }
}