package com.example.draftdeck.ui.feedback

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.draftdeck.data.model.Feedback
import com.example.draftdeck.data.model.InlineComment
import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.domain.usecase.auth.GetCurrentUserUseCase
import com.example.draftdeck.domain.usecase.feedback.AddFeedbackUseCase
import com.example.draftdeck.domain.usecase.feedback.ExportFeedbackUseCase
import com.example.draftdeck.domain.usecase.feedback.GetFeedbackForThesisUseCase
import com.example.draftdeck.domain.usecase.feedback.UpdateFeedbackUseCase
import com.example.draftdeck.domain.usecase.thesis.GetThesisDetailsUseCase
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
class FeedbackViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getThesisDetailsUseCase: GetThesisDetailsUseCase,
    private val getFeedbackForThesisUseCase: GetFeedbackForThesisUseCase,
    private val addFeedbackUseCase: AddFeedbackUseCase,
    private val updateFeedbackUseCase: UpdateFeedbackUseCase,
    private val exportFeedbackUseCase: ExportFeedbackUseCase
) : ViewModel() {

    val currentUser: StateFlow<User?> = getCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _thesisDetails = MutableStateFlow<NetworkResult<Thesis>>(NetworkResult.Loading)
    val thesisDetails: StateFlow<NetworkResult<Thesis>> = _thesisDetails

    private val _feedbackList = MutableStateFlow<NetworkResult<List<Feedback>>>(NetworkResult.Loading)
    val feedbackList: StateFlow<NetworkResult<List<Feedback>>> = _feedbackList

    private val _addFeedbackResult = MutableStateFlow<NetworkResult<Feedback>?>(null)
    val addFeedbackResult: StateFlow<NetworkResult<Feedback>?> = _addFeedbackResult

    private val _updateFeedbackResult = MutableStateFlow<NetworkResult<Feedback>?>(null)
    val updateFeedbackResult: StateFlow<NetworkResult<Feedback>?> = _updateFeedbackResult

    private val _exportFeedbackResult = MutableStateFlow<NetworkResult<File>?>(null)
    val exportFeedbackResult: StateFlow<NetworkResult<File>?> = _exportFeedbackResult

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

    fun addFeedback(
        thesisId: String,
        overallRemarks: String,
        inlineComments: List<InlineComment>
    ) {
        viewModelScope.launch {
            _addFeedbackResult.value = NetworkResult.Loading

            currentUser.value?.let { user ->
                addFeedbackUseCase(
                    thesisId = thesisId,
                    currentUser = user,
                    overallRemarks = overallRemarks,
                    inlineComments = inlineComments
                ).collectLatest { result ->
                    _addFeedbackResult.value = result
                }
            } ?: run {
                _addFeedbackResult.value = NetworkResult.Error(Exception("User not authenticated"))
            }
        }
    }

    fun updateFeedback(
        feedbackId: String,
        overallRemarks: String,
        inlineComments: List<InlineComment>
    ) {
        viewModelScope.launch {
            _updateFeedbackResult.value = NetworkResult.Loading

            updateFeedbackUseCase(
                feedbackId = feedbackId,
                overallRemarks = overallRemarks,
                inlineComments = inlineComments
            ).collectLatest { result ->
                _updateFeedbackResult.value = result
            }
        }
    }

    fun exportFeedbackAsPdf(feedbackId: String, context: Context) {
        viewModelScope.launch {
            _exportFeedbackResult.value = NetworkResult.Loading

            exportFeedbackUseCase(feedbackId, context).collectLatest { result ->
                _exportFeedbackResult.value = result
            }
        }
    }

    fun resetAddFeedbackResult() {
        _addFeedbackResult.value = null
    }

    fun resetUpdateFeedbackResult() {
        _updateFeedbackResult.value = null
    }

    fun resetExportFeedbackResult() {
        _exportFeedbackResult.value = null
    }
}