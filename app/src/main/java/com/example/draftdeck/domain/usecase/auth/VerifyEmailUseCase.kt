package com.example.draftdeck.domain.usecase.auth

import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class VerifyEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, code: String): Flow<NetworkResult<User>> {
        return authRepository.verifyEmail(email, code)
    }
} 