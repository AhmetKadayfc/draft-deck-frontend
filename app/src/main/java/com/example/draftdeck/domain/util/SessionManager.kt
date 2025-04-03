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
    }

    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
        }
    }

    fun getAuthToken(): Flow<String?> = dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN]
    }

    suspend fun saveUser(user: User) {
        dataStore.edit { preferences ->
            preferences[USER] = gson.toJson(user)
        }
    }

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

    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN)
            preferences.remove(USER)
        }
    }
}