package com.example.draftdeck.di

import android.util.Log
import com.example.draftdeck.data.remote.api.AuthApi
import com.example.draftdeck.data.remote.api.FeedbackApi
import com.example.draftdeck.data.remote.api.ThesisApi
import com.example.draftdeck.data.remote.api.UserApi
import com.example.draftdeck.domain.util.Constants
import com.example.draftdeck.domain.util.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TAG = "NetworkDebug"

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionManager: SessionManager): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            Log.d(TAG, "Intercepting request for: ${originalRequest.url}")
            
            // Skip authentication for login and register requests
            val skipAuth = originalRequest.url.encodedPath.contains("/auth/login") || 
                           originalRequest.url.encodedPath.contains("/auth/register") ||
                           originalRequest.url.encodedPath.contains("/auth/password-reset")
            
            if (skipAuth) {
                Log.d(TAG, "Skipping auth for endpoint: ${originalRequest.url}")
                return@Interceptor chain.proceed(originalRequest)
            }
            
            // Get the full auth header with token type
            val authHeader = runBlocking { 
                val header = sessionManager.getAuthHeader().first()
                Log.d(TAG, "Auth header: ${if (header.isNullOrBlank()) "NULL/EMPTY" else "AVAILABLE"}")
                header
            }

            val request = if (!authHeader.isNullOrBlank()) {
                Log.d(TAG, "Adding auth header to request: ${originalRequest.url}")
                originalRequest.newBuilder()
                    .header("Authorization", authHeader)
                    .build()
            } else {
                Log.w(TAG, "No auth header available for request: ${originalRequest.url}")
                originalRequest
            }

            // Log the full request to help debugging
            Log.d(TAG, "Sending request: ${request.method} ${request.url}")
            request.headers.forEach { (name, value) ->
                val displayValue = if (name.equals("Authorization", ignoreCase = true)) "Bearer ***" else value
                Log.d(TAG, "Header: $name = $displayValue")
            }

            val response = chain.proceed(request)
            
            // Log response code
            Log.d(TAG, "Received response: ${response.code} for ${request.url}")
            
            // Check if we got a 401 Unauthorized response
            if (response.code == 401) {
                Log.e(TAG, "Authentication failed (401) for request: ${originalRequest.url}")
                // In a production app, you might implement token refresh here
                
                // For now, we'll just log the error
                // Consider implementing refresh token logic here in the future
            }
            
            response
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val networkDebugInterceptor = Interceptor { chain ->
            val request = chain.request()
            Log.d(TAG, "OkHttp: Executing ${request.method} ${request.url}")
            
            val response = chain.proceed(request)
            Log.d(TAG, "OkHttp: ${request.method} ${request.url} responded ${response.code}")
            
            response
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(networkDebugInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // Log the base URL to verify it's correct
        Log.d(TAG, "Creating Retrofit with base URL: ${Constants.BASE_URL}")
        
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        Log.d(TAG, "Creating AuthApi instance")
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideThesisApi(retrofit: Retrofit): ThesisApi {
        Log.d(TAG, "Creating ThesisApi instance")
        return retrofit.create(ThesisApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFeedbackApi(retrofit: Retrofit): FeedbackApi {
        Log.d(TAG, "Creating FeedbackApi instance")
        return retrofit.create(FeedbackApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi {
        Log.d(TAG, "Creating UserApi instance")
        return retrofit.create(UserApi::class.java)
    }
}