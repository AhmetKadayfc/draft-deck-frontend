package com.example.draftdeck.data.repository

import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.remote.api.AuthApi
import com.example.draftdeck.data.remote.api.LoginRequest
import com.example.draftdeck.data.remote.api.RegisterRequest
import com.example.draftdeck.data.remote.dto.toUser
import com.example.draftdeck.domain.util.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

interface AuthRepository {
    suspend fun login(email: String, password: String): Flow<NetworkResult<User>>
    suspend fun register(email: String, password: String, name: String, surname: String, role: String): Flow<NetworkResult<User>>
    suspend fun logout(): Flow<NetworkResult<Unit>>
    suspend fun resetPassword(email: String): Flow<NetworkResult<Unit>>
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
                    sessionManager.saveAuthToken(authResponse.token)
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
        name: String,
        surname: String,
        role: String
    ): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading)
        try {
            val request = RegisterRequest(email, password, name, surname, role)
            val response = authApi.register(request)

            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    sessionManager.saveAuthToken(authResponse.token)
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
        emit(NetworkResult.Loading)
        try {
            val response = authApi.resetPassword(email)

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