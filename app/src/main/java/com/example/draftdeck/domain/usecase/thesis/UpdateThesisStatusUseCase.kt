package com.example.draftdeck.domain.usecase.thesis

import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.repository.ThesisRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UpdateThesisStatusUseCase @Inject constructor(
    private val thesisRepository: ThesisRepository
) {
    suspend operator fun invoke(thesisId: String, newStatus: String): Flow<NetworkResult<Thesis>> {
        return thesisRepository.updateThesisStatus(thesisId, newStatus)
    }
}