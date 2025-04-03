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
    operator fun invoke(currentUser: User): Flow<NetworkResult<List<Thesis>>> {
        return when (currentUser.role) {
            Constants.ROLE_STUDENT -> thesisRepository.getThesesByStudentId(currentUser.id)
            Constants.ROLE_ADVISOR -> thesisRepository.getThesesByAdvisorId(currentUser.id)
            Constants.ROLE_ADMIN -> thesisRepository.getAllTheses()
            else -> thesisRepository.getAllTheses()
        }
    }
}