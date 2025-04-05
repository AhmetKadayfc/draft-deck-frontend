package com.example.draftdeck.data.remote.api

import com.example.draftdeck.data.remote.dto.UserDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

data class UpdateProfileRequest(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String?
)

interface UserApi {
    @GET("users/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): Response<UserDto>

    @PUT("users/{userId}")
    suspend fun updateUserProfile(
        @Path("userId") userId: String,
        @Body request: UpdateProfileRequest
    ): Response<UserDto>

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