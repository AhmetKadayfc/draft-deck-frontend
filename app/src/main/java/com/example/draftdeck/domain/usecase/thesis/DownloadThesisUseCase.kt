package com.example.draftdeck.domain.usecase.thesis

import android.content.Context
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.repository.ThesisRepository
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class DownloadThesisUseCase @Inject constructor(
    private val thesisRepository: ThesisRepository
) {
    suspend operator fun invoke(thesisId: String, context: Context): Flow<NetworkResult<File>> {
        return thesisRepository.downloadThesis(thesisId, context)
    }
}