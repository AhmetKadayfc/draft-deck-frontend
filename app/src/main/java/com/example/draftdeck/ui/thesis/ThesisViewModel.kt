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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ThesisViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getThesisDetailsUseCase: GetThesisDetailsUseCase,
    private val uploadThesisUseCase: UploadThesisUseCase,
    private val updateThesisUseCase: UpdateThesisUseCase,
    private val updateThesisStatusUseCase: UpdateThesisStatusUseCase,
    private val downloadThesisUseCase: DownloadThesisUseCase,
    private val getFeedbackForThesisUseCase: GetFeedbackForThesisUseCase
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

    fun loadThesisDetails(thesisId: String) {
        viewModelScope.launch {
            getThesisDetailsUseCase(thesisId).collectLatest { result ->
                _thesisDetails.value = result
            }
        }
    }

    fun loadFeedbackList(thesisId: String) {
        viewModelScope.launch {
            getFeedbackForThesisUseCase(thesisId).collectLatest { result ->
                _feedbackList.value = result
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

            currentUser.value?.let { user ->
                uploadThesisUseCase(
                    context = context,
                    title = title,
                    description = description,
                    fileUri = fileUri,
                    submissionType = apiSubmissionType,
                    currentUser = user,
                    advisorId = advisorId
                ).collectLatest { result ->
                    _uploadThesisResult.value = result
                }
            } ?: run {
                _uploadThesisResult.value = NetworkResult.Error(Exception("User not authenticated"))
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

            updateThesisUseCase(
                context = context,
                thesisId = thesisId,
                title = title,
                description = description,
                submissionType = apiSubmissionType,
                fileUri = fileUri
            ).collectLatest { result ->
                _updateThesisResult.value = result
            }
        }
    }

    fun updateThesisStatus(thesisId: String, newStatus: String) {
        viewModelScope.launch {
            updateThesisStatusUseCase(thesisId, newStatus).collectLatest { result ->
                if (result is NetworkResult.Success) {
                    _thesisDetails.value = result
                }
            }
        }
    }

    fun downloadThesis(thesisId: String, context: Context) {
        viewModelScope.launch {
            _downloadThesisResult.value = NetworkResult.Loading

            downloadThesisUseCase(thesisId, context).collectLatest { result ->
                _downloadThesisResult.value = result
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
     * Logs the authentication state for debugging
     * @return true if authenticated, false otherwise
     */
    fun checkAuthentication(): Boolean {
        val isAuthenticated = currentUser.value != null
        if (!isAuthenticated) {
            // Log for debugging
            android.util.Log.w("ThesisViewModel", "User is not authenticated")
        } else {
            android.util.Log.d("ThesisViewModel", "User is authenticated: ${currentUser.value?.email}")
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
}