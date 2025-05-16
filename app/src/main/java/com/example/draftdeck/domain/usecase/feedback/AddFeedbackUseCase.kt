package com.example.draftdeck.domain.usecase.feedback

import com.example.draftdeck.data.model.Feedback
import com.example.draftdeck.data.model.InlineComment
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.repository.FeedbackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AddFeedbackUseCase @Inject constructor(
    private val feedbackRepository: FeedbackRepository
) {
    suspend operator fun invoke(
        thesisId: String,
        currentUser: User,
        overallRemarks: String,
        inlineComments: List<InlineComment>
    ): Flow<NetworkResult<Feedback>> {
        return feedbackRepository.createFeedback(
            thesisId = thesisId,
            overallRemarks = overallRemarks,
            inlineComments = inlineComments
        )
    }
}