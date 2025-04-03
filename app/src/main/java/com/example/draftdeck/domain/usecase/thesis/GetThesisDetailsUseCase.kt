package com.example.draftdeck.domain.usecase.thesis

import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.repository.ThesisRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThesisDetailsUseCase @Inject constructor(
    private val thesisRepository: ThesisRepository
) {
    operator fun invoke(thesisId: String): Flow<NetworkResult<Thesis>> {
        return thesisRepository.getThesisById(thesisId)
    }
}