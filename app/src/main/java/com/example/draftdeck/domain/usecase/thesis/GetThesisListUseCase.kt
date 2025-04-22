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
    /**
     * Get thesis list with filters that match the Flask API's expected parameters
     * @param currentUser User making the request (role handled server-side)
     * @param status Filter by thesis status
     * @param type Filter by thesis type (matches "type" parameter in Flask API)
     * @param query Search query for thesis title/description
     */
    operator fun invoke(currentUser: User, status: String? = null, type: String? = null, query: String? = null): Flow<NetworkResult<List<Thesis>>> {
        // The repository will send these parameters to the API endpoint
        return thesisRepository.getTheses(status, type, query)
    }
}