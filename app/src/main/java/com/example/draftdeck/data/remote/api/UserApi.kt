package com.example.draftdeck.data.remote.api

import com.example.draftdeck.data.remote.dto.UserDto
import com.example.draftdeck.data.remote.dto.UsersResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class UpdateProfileRequest(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?
)

data class AssignAdvisorRequest(
    val advisorId: String
)

interface UserApi {
    @GET("users/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): Response<UserDto>

    @GET("users")
    suspend fun getUsersByRole(@Query("role") role: String): Response<UsersResponse>

    @GET("admin/users")
    suspend fun getAdminUsersList(@Query("role") role: String? = null): Response<UsersResponse>

    @PUT("users/{userId}")
    suspend fun updateUserProfile(
        @Path("userId") userId: String,
        @Body request: UpdateProfileRequest
    ): Response<UserDto>
    
    // Simplified method signature with direct parameters
    suspend fun updateUserProfile(
        @Path("userId") userId: String,
        firstName: String,
        lastName: String,
        phoneNumber: String?
    ): Response<UserDto> {
        return updateUserProfile(
            userId, 
            UpdateProfileRequest(firstName, lastName, phoneNumber)
        )
    }

    @POST("users/{studentId}/assign-advisor")
    suspend fun assignAdvisorToStudent(
        @Path("studentId") studentId: String,
        @Body request: AssignAdvisorRequest
    ): Response<Unit>
    
    @POST("admin/users/{studentId}/assign-advisor")
    suspend fun adminAssignAdvisorToStudent(
        @Path("studentId") studentId: String,
        @Body request: AssignAdvisorRequest
    ): Response<Unit>
    
    // Simplified method signature with direct parameter
    suspend fun assignAdvisorToStudent(
        @Path("studentId") studentId: String,
        advisorId: String
    ): Response<Unit> {
        return assignAdvisorToStudent(studentId, AssignAdvisorRequest(advisorId))
    }
    
    // Simplified method signature for admin endpoint
    suspend fun adminAssignAdvisorToStudent(
        @Path("studentId") studentId: String,
        advisorId: String
    ): Response<Unit> {
        return adminAssignAdvisorToStudent(studentId, AssignAdvisorRequest(advisorId))
    }

    @Multipart
    @PUT("users/{userId}/profile-picture")
    suspend fun updateProfilePicture(
        @Path("userId") userId: String,
        @Part file: MultipartBody.Part
    ): Response<UserDto>

    @PUT("users/{userId}/password")
    suspend fun updatePassword(
        @Path("userId") userId: String,
        @Body oldPassword: String,
        @Body newPassword: String
    ): Response<Unit>
}