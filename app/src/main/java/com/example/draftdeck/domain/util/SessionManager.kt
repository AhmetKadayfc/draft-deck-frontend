package com.example.draftdeck.domain.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.draftdeck.data.model.User
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFERENCES_NAME)

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    private val dataStore = context.dataStore

    companion object {
        private val AUTH_TOKEN = stringPreferencesKey(Constants.KEY_AUTH_TOKEN)
        private val USER = stringPreferencesKey(Constants.KEY_USER_PROFILE)
        private val TOKEN_TYPE = stringPreferencesKey(Constants.KEY_TOKEN_TYPE)
        private val REFRESH_TOKEN = stringPreferencesKey(Constants.KEY_REFRESH_TOKEN)
        private val TOKEN_EXPIRY = stringPreferencesKey(Constants.KEY_TOKEN_EXPIRY)
    }

    /**
     * Saves the authentication token to DataStore
     */
    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
        }
    }
    
    /**
     * Saves complete authentication data including token type and refresh token
     */
    suspend fun saveAuthData(
        accessToken: String, 
        refreshToken: String, 
        tokenType: String, 
        expiresIn: Int
    ) {
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000)
        
        dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = accessToken
            preferences[REFRESH_TOKEN] = refreshToken
            preferences[TOKEN_TYPE] = tokenType
            preferences[TOKEN_EXPIRY] = expiryTime.toString()
        }
    }

    /**
     * Returns the stored auth token as a Flow
     */
    fun getAuthToken(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN]
    }
    
    /**
     * Returns the full authorization header value (with token type)
     */
    fun getAuthHeader(): Flow<String?> = dataStore.data.map { preferences ->
        val token = preferences[AUTH_TOKEN]
        val type = preferences[TOKEN_TYPE] ?: "Bearer"
        
        if (!token.isNullOrBlank()) {
            "$type $token"
        } else {
            null
        }
    }
    
    /**
     * Checks if the token is expired
     */
    fun isTokenExpired(): Flow<Boolean> = dataStore.data.map { preferences ->
        val expiryTimeStr = preferences[TOKEN_EXPIRY]
        if (expiryTimeStr.isNullOrBlank()) {
            false // No expiry time set, assume not expired
        } else {
            try {
                val expiryTime = expiryTimeStr.toLong()
                System.currentTimeMillis() > expiryTime
            } catch (e: Exception) {
                false // Error parsing, assume not expired
            }
        }
    }
    
    /**
     * Returns the refresh token
     */
    fun getRefreshToken(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN]
    }

    /**
     * Saves user data to DataStore
     */
    suspend fun saveUser(user: User) {
        dataStore.edit { preferences ->
            preferences[USER] = gson.toJson(user)
        }
    }

    /**
     * Returns the current user as a Flow
     */
    fun getUserFlow(): Flow<User?> = dataStore.data.map { preferences ->
        val userJson = preferences[USER]
        userJson?.let {
            try {
                gson.fromJson(it, User::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Clears all session data
     */
    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN)
            preferences.remove(REFRESH_TOKEN)
            preferences.remove(TOKEN_TYPE)
            preferences.remove(TOKEN_EXPIRY)
            preferences.remove(USER)
        }
    }
}