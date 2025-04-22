package com.example.draftdeck.domain.usecase.thesis

import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.repository.ThesisRepository
import com.example.draftdeck.domain.util.Constants
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThesisListUseCase @Inject constructor(
    private val thesisRepository: ThesisRepository
) {
    operator fun invoke(currentUser: User, status: String? = null, type: String? = null, query: String? = null): Flow<NetworkResult<List<Thesis>>> {
        // Use the new getTheses method which handles role-specific logic server-side
        return thesisRepository.getTheses(status, type, query)
    }
}