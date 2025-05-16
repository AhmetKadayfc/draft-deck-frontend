package com.example.draftdeck.data.repository

import com.example.draftdeck.data.local.dao.UserDao
import com.example.draftdeck.data.local.entity.toUser
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.remote.api.UserApi
import com.example.draftdeck.data.remote.api.UpdateProfileRequest
import com.example.draftdeck.data.remote.dto.toUser
import com.example.draftdeck.data.local.entity.toUserEntity
import com.example.draftdeck.domain.util.NetworkConnectivityManager
import com.example.draftdeck.domain.util.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import android.util.Log

/**
 * Repository for managing user data and operations
 */
interface UserRepository {
    /**
     * Get the currently authenticated user
     */
    fun getCurrentUser(): Flow<User?>
    
    /**
     * Get a user by their ID
     */
    suspend fun getUserById(userId: String): Flow<NetworkResult<User>>
    
    /**
     * Get users by role (student, advisor, admin)
     */
    suspend fun getUsersByRole(role: String): List<User>
    
    /**
     * Assign an advisor to a student
     */
    suspend fun assignAdvisorToStudent(studentId: String, advisorId: String)
    
    /**
     * Update user profile information
     */
    suspend fun updateUserProfile(userId: String, firstName: String, lastName: String, phoneNumber: String?): User
}

/**
 * Implementation of UserRepository that combines local and remote data sources
 */
class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    override val networkConnectivityManager: NetworkConnectivityManager
) : UserRepository, BaseRepository {

    override val TAG = "UserRepoDebug"

    override fun getCurrentUser(): Flow<User?> {
        return sessionManager.getUserFlow()
    }
    
    override suspend fun getUserById(userId: String): Flow<NetworkResult<User>> {
        return getDataWithOfflineSupport(
            localDataSource = {
                try {
                    userDao.getUserById(userId).first()?.toUser()
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading user from local database: ${e.message}")
                    null
                }
            },
            remoteDataSource = {
                val response = userApi.getUserById(userId)
                if (response.isSuccessful) {
                    response.body()?.toUser() ?: throw IOException("Empty response body")
                } else {
                    throw HttpException(response)
                }
            },
            saveRemoteData = { user ->
                userDao.insertUser(user.toUserEntity())
            }
        )
    }
    
    override suspend fun getUsersByRole(role: String): List<User> {
        try {
            // Use the admin endpoint which is specifically for retrieving user lists
            val response = userApi.getAdminUsersList(role)
            if (response.isSuccessful) {
                return response.body()?.users?.map { it.toUser() } ?: emptyList()
            } else {
                throw HttpException(response)
            }
        } catch (e: Exception) {
            // Log the error for debugging
            Log.e("UserRepository", "Error getting users by role: ${e.message}", e)
            throw e
        }
    }
    
    override suspend fun assignAdvisorToStudent(studentId: String, advisorId: String) {
        val response = userApi.assignAdvisorToStudent(studentId, advisorId)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }
    
    override suspend fun updateUserProfile(userId: String, firstName: String, lastName: String, phoneNumber: String?): User {
        val request = UpdateProfileRequest(firstName, lastName, phoneNumber)
        val response = userApi.updateUserProfile(userId, request)
        if (response.isSuccessful) {
            val updatedUser = response.body()?.toUser() ?: throw IOException("Empty response body")
            // Update in local database
            userDao.insertUser(updatedUser.toUserEntity())
            // Update in session if this is the current user
            sessionManager.updateCurrentUserIfMatches(updatedUser)
            return updatedUser
        } else {
            throw HttpException(response)
        }
    }
} 