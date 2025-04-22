package com.example.draftdeck.ui.dashboard

import android.util.Log
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

/**
 * ViewModel for the Dashboard screen that handles thesis listing with filtering options.
 * Uses role-based access control from the API rather than client-side filtering.
 */
@HiltViewModel
open class DashboardViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getThesisListUseCase: GetThesisListUseCase
) : ViewModel() {

    private val TAG = "DashboardDebug"

    // Current authenticated user
    open val currentUser: StateFlow<User?> = getCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // List of theses with their loading/error states
    private val _thesisList = MutableStateFlow<NetworkResult<List<Thesis>>>(NetworkResult.Loading)
    open val thesisList: StateFlow<NetworkResult<List<Thesis>>> = _thesisList

    // Parameters for filtering that will be sent to the API
    private var currentStatus: String? = null
    private var currentType: String? = null
    private var currentQuery: String? = null

    /**
     * Loads the thesis list from the API with current filters applied
     */
    open fun loadThesisList() {
        Log.d(TAG, "Loading thesis list with filters - status: $currentStatus, type: $currentType, query: $currentQuery")
        viewModelScope.launch {
            Log.d(TAG, "Collecting current user")
            currentUser.collectLatest { user ->
                if (user != null) {
                    Log.d(TAG, "Current user found: ID=${user.id}, Role=${user.role}")
                    getThesisListUseCase(user, currentStatus, currentType, currentQuery).collectLatest { result ->
                        Log.d(TAG, "Thesis list result received: $result")
                        when (result) {
                            is NetworkResult.Success -> 
                                Log.d(TAG, "Received ${result.data.size} theses")
                            is NetworkResult.Error -> 
                                Log.e(TAG, "Error loading theses: ${result.exception.message}")
                            is NetworkResult.Loading -> 
                                Log.d(TAG, "Thesis list is loading")
                            is NetworkResult.Idle ->
                                Log.d(TAG, "Thesis list is idle")
                        }
                        _thesisList.value = result
                    }
                } else {
                    Log.e(TAG, "No current user found!")
                }
            }
        }
    }

    /**
     * Consolidated method to apply all filters at once and trigger a single API call
     * This prevents multiple rapid API calls when multiple filters change simultaneously
     */
    open fun applyFilters(status: String? = null, type: String? = null, query: String? = null) {
        Log.d(TAG, "Setting all filters at once - status: ${status ?: "ALL"}, type: ${type ?: "ALL"}, query: ${query ?: "NONE"}")
        
        // Only make an API call if the filters have actually changed
        val filtersChanged = currentStatus != status || currentType != type || currentQuery != query
        
        if (filtersChanged) {
            currentStatus = status
            currentType = type
            currentQuery = query
            
            // Single API call for all filter changes
            refreshThesisList()
        } else {
            Log.d(TAG, "Filters unchanged, skipping refresh")
        }
    }

    /**
     * Filter theses by status (pending, reviewed, approved)
     * @deprecated Use applyFilters instead to prevent multiple API calls
     */
    @Deprecated("Use applyFilters instead to prevent multiple API calls")
    open fun filterThesisByStatus(status: String?) {
        Log.d(TAG, "Setting status filter: ${status ?: "ALL"}")
        this.currentStatus = status
        refreshThesisList()
    }

    /**
     * Filter theses by submission type (draft, final)
     * @deprecated Use applyFilters instead to prevent multiple API calls
     */
    @Deprecated("Use applyFilters instead to prevent multiple API calls")
    open fun filterThesisByType(type: String?) {
        Log.d(TAG, "Setting type filter: ${type ?: "ALL"}")
        this.currentType = type
        refreshThesisList()
    }

    /**
     * Search theses by query text (searches title and description)
     * @deprecated Use applyFilters instead to prevent multiple API calls
     */
    @Deprecated("Use applyFilters instead to prevent multiple API calls")
    open fun searchTheses(query: String?) {
        Log.d(TAG, "Setting search query: ${query ?: "NONE"}")
        this.currentQuery = if (query.isNullOrBlank()) null else query
        refreshThesisList()
    }

    /**
     * Force refresh the thesis list
     */
    open fun refreshThesisList() {
        Log.d(TAG, "Manually refreshing thesis list")
        loadThesisList()
    }
}