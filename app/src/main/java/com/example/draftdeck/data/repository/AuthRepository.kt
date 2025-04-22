package com.example.draftdeck.data.repository

import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.remote.api.AuthApi
import com.example.draftdeck.data.remote.api.LoginRequest
import com.example.draftdeck.data.remote.api.RegisterRequest
import com.example.draftdeck.data.remote.api.VerifyEmailRequest
import com.example.draftdeck.data.remote.api.ResendVerificationRequest
import com.example.draftdeck.data.remote.api.ResetPasswordRequest
import com.example.draftdeck.data.remote.api.VerifyPasswordResetCodeRequest
import com.example.draftdeck.data.remote.api.CompletePasswordResetRequest
import com.example.draftdeck.data.remote.dto.toUser
import com.example.draftdeck.domain.util.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

interface AuthRepository {
    suspend fun login(email: String, password: String): Flow<NetworkResult<User>>
    suspend fun register(email: String, password: String, firstName: String, lastName: String, role: String, studentId: String? = null): Flow<NetworkResult<User>>
    suspend fun logout(): Flow<NetworkResult<Unit>>
    suspend fun resetPassword(email: String): Flow<NetworkResult<Unit>>
    suspend fun verifyEmail(email: String, code: String): Flow<NetworkResult<User>>
    suspend fun resendVerification(email: String): Flow<NetworkResult<Unit>>
    suspend fun verifyPasswordResetCode(email: String, code: String): Flow<NetworkResult<Unit>>
    suspend fun completePasswordReset(email: String, newPassword: String, code: String): Flow<NetworkResult<Unit>>
    fun getCurrentUser(): Flow<User?>
    suspend fun clearSession()
}

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = LoginRequest(email, password)
            val response = authApi.login(request)

            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    sessionManager.saveAuthData(
                        accessToken = authResponse.access_token,
                        refreshToken = authResponse.refresh_token,
                        tokenType = authResponse.token_type,
                        expiresIn = authResponse.expires_in
                    )
                    sessionManager.saveUser(authResponse.user.toUser())
                    emit(NetworkResult.Success(authResponse.user.toUser()))
                } ?: emit(NetworkResult.Error(Exception("Empty response body")))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: String,
        studentId: String?
    ): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = RegisterRequest(email, password, firstName, lastName, role, studentId)
            val response = authApi.register(request)

            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    sessionManager.saveAuthData(
                        accessToken = authResponse.access_token,
                        refreshToken = authResponse.refresh_token,
                        tokenType = authResponse.token_type,
                        expiresIn = authResponse.expires_in
                    )
                    sessionManager.saveUser(authResponse.user.toUser())
                    emit(NetworkResult.Success(authResponse.user.toUser()))
                } ?: emit(NetworkResult.Error(Exception("Empty response body")))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun verifyEmail(email: String, code: String): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = VerifyEmailRequest(email, code)
            val response = authApi.verifyEmail(request)

            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    sessionManager.saveAuthData(
                        accessToken = authResponse.access_token,
                        refreshToken = authResponse.refresh_token,
                        tokenType = authResponse.token_type,
                        expiresIn = authResponse.expires_in
                    )
                    sessionManager.saveUser(authResponse.user.toUser())
                    emit(NetworkResult.Success(authResponse.user.toUser()))
                } ?: emit(NetworkResult.Error(Exception("Empty response body")))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun resendVerification(email: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = ResendVerificationRequest(email)
            val response = authApi.resendVerification(request)

            if (response.isSuccessful) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun logout(): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val response = authApi.logout()

            if (response.isSuccessful) {
                sessionManager.clearSession()
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        } finally {
            // Clear local session even if network call fails
            sessionManager.clearSession()
        }
    }

    override suspend fun resetPassword(email: String): Flow<NetworkResult<Unit>> = flow {
        // Emit Loading state to indicate an active request
        emit(NetworkResult.Loading)
        try {
            val request = ResetPasswordRequest(email)
            val response = authApi.resetPassword(request)
            
            if (response.isSuccessful) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun verifyPasswordResetCode(email: String, code: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = VerifyPasswordResetCodeRequest(email, code)
            val response = authApi.verifyPasswordResetCode(request)
            
            if (response.isSuccessful) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }
    
    override suspend fun completePasswordReset(email: String, newPassword: String, code: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = CompletePasswordResetRequest(email, newPassword, code)
            val response = authApi.completePasswordReset(request)
            
            if (response.isSuccessful) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override fun getCurrentUser(): Flow<User?> {
        return sessionManager.getUserFlow()
    }

    override suspend fun clearSession() {
        sessionManager.clearSession()
    }
}