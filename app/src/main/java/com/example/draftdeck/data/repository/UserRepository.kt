package com.example.draftdeck.data.repository

import com.example.draftdeck.data.local.dao.UserDao
import com.example.draftdeck.data.local.entity.UserEntity
import com.example.draftdeck.data.local.entity.toUser
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.remote.api.UserApi
import com.example.draftdeck.data.remote.api.UpdateProfileRequest
import com.example.draftdeck.data.remote.dto.toUser
import com.example.draftdeck.data.local.entity.toUserEntity
import com.example.draftdeck.domain.util.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
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
    suspend fun getUserById(userId: String): Flow<User?>
    
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
    private val sessionManager: SessionManager
) : UserRepository {

    override fun getCurrentUser(): Flow<User?> {
        return sessionManager.getUserFlow()
    }
    
    override suspend fun getUserById(userId: String): Flow<User?> = flow {
        // First try to get from local database
        try {
            // Emit local data first
            userDao.getUserById(userId).collect { userEntity ->
                if (userEntity != null) {
                    emit(userEntity.toUser())
                }
            }
        } catch (e: Exception) {
            // Continue to API call if local database fails
        }
        
        // Then try to get from API
        try {
            val response = userApi.getUserById(userId)
            if (response.isSuccessful) {
                response.body()?.let { userDto ->
                    val user = userDto.toUser()
                    // Save to local database
                    userDao.insertUser(user.toUserEntity())
                    emit(user)
                }
            }
        } catch (e: Exception) {
            // If API fails, we already emitted the local user if available
            emit(null)
        }
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