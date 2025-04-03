package com.example.draftdeck.domain.usecase.feedback

import com.example.draftdeck.data.model.Feedback
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.repository.FeedbackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFeedbackForThesisUseCase @Inject constructor(
    private val feedbackRepository: FeedbackRepository
) {
    operator fun invoke(thesisId: String): Flow<NetworkResult<List<Feedback>>> {
        return feedbackRepository.getFeedbackForThesis(thesisId)
    }
}