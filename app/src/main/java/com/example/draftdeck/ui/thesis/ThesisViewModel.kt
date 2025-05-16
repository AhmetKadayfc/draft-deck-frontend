package com.example.draftdeck.ui.thesis

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.draftdeck.data.model.Feedback
import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.domain.usecase.auth.GetCurrentUserUseCase
import com.example.draftdeck.domain.usecase.feedback.GetFeedbackForThesisUseCase
import com.example.draftdeck.domain.usecase.thesis.DownloadThesisUseCase
import com.example.draftdeck.domain.usecase.thesis.GetThesisDetailsUseCase
import com.example.draftdeck.domain.usecase.thesis.UpdateThesisStatusUseCase
import com.example.draftdeck.domain.usecase.thesis.UpdateThesisUseCase
import com.example.draftdeck.domain.usecase.thesis.UploadThesisUseCase
import com.example.draftdeck.domain.util.Constants
import com.example.draftdeck.domain.util.NetworkConnectivityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class ThesisViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getThesisDetailsUseCase: GetThesisDetailsUseCase,
    private val uploadThesisUseCase: UploadThesisUseCase,
    private val updateThesisUseCase: UpdateThesisUseCase,
    private val updateThesisStatusUseCase: UpdateThesisStatusUseCase,
    private val downloadThesisUseCase: DownloadThesisUseCase,
    private val getFeedbackForThesisUseCase: GetFeedbackForThesisUseCase,
    private val networkConnectivityManager: NetworkConnectivityManager
) : ViewModel() {

    val currentUser: StateFlow<User?> = getCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _thesisDetails = MutableStateFlow<NetworkResult<Thesis>>(NetworkResult.Idle)
    val thesisDetails: StateFlow<NetworkResult<Thesis>> = _thesisDetails

    private val _feedbackList = MutableStateFlow<NetworkResult<List<Feedback>>>(NetworkResult.Idle)
    val feedbackList: StateFlow<NetworkResult<List<Feedback>>> = _feedbackList

    private val _uploadThesisResult = MutableStateFlow<NetworkResult<Thesis>?>(null)
    val uploadThesisResult: StateFlow<NetworkResult<Thesis>?> = _uploadThesisResult

    private val _updateThesisResult = MutableStateFlow<NetworkResult<Thesis>?>(null)
    val updateThesisResult: StateFlow<NetworkResult<Thesis>?> = _updateThesisResult

    private val _downloadThesisResult = MutableStateFlow<NetworkResult<File>?>(null)
    val downloadThesisResult: StateFlow<NetworkResult<File>?> = _downloadThesisResult

    // Add network connectivity state
    private val _isOnline = MutableStateFlow(networkConnectivityManager.isNetworkAvailable())
    val isOnline: StateFlow<Boolean> = _isOnline

    init {
        // Observe network connectivity changes
        viewModelScope.launch {
            networkConnectivityManager.observeNetworkConnectivity().collect { isConnected ->
                _isOnline.value = isConnected
                Log.d("ThesisViewModel", "Network connectivity changed. Online: $isConnected")
            }
        }
    }

    fun loadThesisDetails(thesisId: String) {
        viewModelScope.launch {
            _thesisDetails.value = NetworkResult.Loading
            try {
                getThesisDetailsUseCase(thesisId).collect { result ->
                    if (result is NetworkResult.Error && !_isOnline.value) {
                        // Add offline information to error message when we're offline
                        val offlineMessage = if (result.exception.message?.contains("No internet connection") == true) {
                            result.exception.message ?: "No internet connection"
                        } else {
                            "You're offline. Some data may be out of date."
                        }
                        _thesisDetails.value = NetworkResult.Error(Exception(offlineMessage))
                    } else {
                        _thesisDetails.value = result
                    }
                }
            } catch (e: Exception) {
                // Handle cancellation gracefully
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("ThesisViewModel", "loadThesisDetails job was cancelled")
                } else {
                    _thesisDetails.value = NetworkResult.Error(e)
                    Log.e("ThesisViewModel", "Error loading thesis details", e)
                }
            }
        }
    }

    fun loadFeedbackList(thesisId: String) {
        viewModelScope.launch {
            _feedbackList.value = NetworkResult.Loading
            try {
                getFeedbackForThesisUseCase(thesisId).collect { result ->
                    _feedbackList.value = result
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("ThesisViewModel", "loadFeedbackList job was cancelled")
                } else {
                    _feedbackList.value = NetworkResult.Error(e)
                    Log.e("ThesisViewModel", "Error loading feedback list", e)
                }
            }
        }
    }

    fun uploadThesis(
        context: Context,
        title: String,
        description: String,
        fileUri: Uri,
        submissionType: String,
        advisorId: String
    ) {
        viewModelScope.launch {
            _uploadThesisResult.value = NetworkResult.Loading

            // Convert display-friendly submission type to API-compatible value
            val apiSubmissionType = when (submissionType) {
                Constants.THESIS_TYPE_DRAFT_DISPLAY -> Constants.THESIS_TYPE_DRAFT
                Constants.THESIS_TYPE_FINAL_DISPLAY -> Constants.THESIS_TYPE_FINAL
                else -> submissionType.lowercase() // Fallback
            }

            try {
                currentUser.value?.let { user ->
                    uploadThesisUseCase(
                        context = context,
                        title = title,
                        description = description,
                        fileUri = fileUri,
                        submissionType = apiSubmissionType,
                        currentUser = user,
                        advisorId = advisorId
                    ).collect { result ->
                        _uploadThesisResult.value = result
                    }
                } ?: run {
                    _uploadThesisResult.value = NetworkResult.Error(Exception("User not authenticated"))
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("ThesisViewModel", "uploadThesis job was cancelled")
                } else {
                    _uploadThesisResult.value = NetworkResult.Error(e)
                    Log.e("ThesisViewModel", "Error uploading thesis", e)
                }
            }
        }
    }

    fun updateThesis(
        context: Context,
        thesisId: String,
        title: String,
        description: String,
        submissionType: String,
        fileUri: Uri?
    ) {
        viewModelScope.launch {
            _updateThesisResult.value = NetworkResult.Loading

            // Convert display-friendly submission type to API-compatible value
            val apiSubmissionType = when (submissionType) {
                Constants.THESIS_TYPE_DRAFT_DISPLAY -> Constants.THESIS_TYPE_DRAFT
                Constants.THESIS_TYPE_FINAL_DISPLAY -> Constants.THESIS_TYPE_FINAL
                else -> submissionType.lowercase() // Fallback
            }

            try {
                updateThesisUseCase(
                    context = context,
                    thesisId = thesisId,
                    title = title,
                    description = description,
                    submissionType = apiSubmissionType,
                    fileUri = fileUri
                ).collect { result ->
                    _updateThesisResult.value = result
                    
                    // Show success message
                    if (result is NetworkResult.Success) {
                        android.widget.Toast.makeText(
                            context,
                            "Thesis updated successfully",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("ThesisViewModel", "updateThesis job was cancelled")
                } else {
                    _updateThesisResult.value = NetworkResult.Error(e)
                    Log.e("ThesisViewModel", "Error updating thesis", e)
                    
                    // Show error message
                    android.widget.Toast.makeText(
                        context,
                        "Error updating thesis",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun updateThesisStatus(thesisId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                updateThesisStatusUseCase(thesisId, newStatus).collect { result ->
                    if (result is NetworkResult.Success) {
                        _thesisDetails.value = result
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("ThesisViewModel", "updateThesisStatus job was cancelled")
                } else {
                    Log.e("ThesisViewModel", "Error updating thesis status", e)
                }
            }
        }
    }

    fun downloadThesis(thesisId: String, context: Context) {
        viewModelScope.launch {
            _downloadThesisResult.value = NetworkResult.Loading

            try {
                downloadThesisUseCase(thesisId, context).collect { result ->
                    _downloadThesisResult.value = result
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.d("ThesisViewModel", "downloadThesis job was cancelled")
                } else {
                    _downloadThesisResult.value = NetworkResult.Error(e)
                    Log.e("ThesisViewModel", "Error downloading thesis", e)
                }
            }
        }
    }

    fun resetUploadResult() {
        _uploadThesisResult.value = null
    }

    fun resetUpdateResult() {
        _updateThesisResult.value = null
    }

    fun resetDownloadResult() {
        _downloadThesisResult.value = null
    }

    /**
     * Check if the user is properly authenticated before performing thesis operations
     * @return true if authenticated, false otherwise
     */
    fun checkAuthentication(): Boolean {
        val isAuthenticated = currentUser.value != null
        if (!isAuthenticated) {
            // Log for debugging
            Log.w("ThesisViewModel", "User is not authenticated")
        }
        return isAuthenticated
    }

    /**
     * A more robust check that can be called before critical operations
     * that require the current user
     */
    fun validateSession(): User? {
        return currentUser.value
    }

    /**
     * Checks if device is currently online. This can be used in UI 
     * to display connectivity status or disable features that require connectivity.
     */
    fun isDeviceOnline(): Boolean {
        return networkConnectivityManager.isNetworkAvailable()
    }
}