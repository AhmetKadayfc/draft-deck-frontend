package com.example.draftdeck.domain.usecase.auth

import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<User?> {
        return authRepository.getCurrentUser()
    }
}