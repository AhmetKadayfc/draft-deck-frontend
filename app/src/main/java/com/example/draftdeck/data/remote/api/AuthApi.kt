package com.example.draftdeck.data.remote.api

import com.example.draftdeck.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val first_name: String,
    val last_name: String,
    val role: String
)

data class VerifyEmailRequest(
    val email: String,
    val code: String
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<AuthResponse>

    @POST("auth/resend-verification")
    suspend fun resendVerification(@Body email: String): Response<Unit>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body email: String): Response<Unit>
}