package com.example.draftdeck.domain.usecase.feedback

import android.content.Context
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.repository.FeedbackRepository
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class ExportFeedbackUseCase @Inject constructor(
    private val feedbackRepository: FeedbackRepository
) {
    suspend operator fun invoke(feedbackId: String, context: Context): Flow<NetworkResult<File>> {
        return feedbackRepository.exportFeedbackAsPdf(feedbackId, context)
    }
}