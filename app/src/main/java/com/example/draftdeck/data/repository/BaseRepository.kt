package com.example.draftdeck.data.repository

import android.util.Log
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.domain.util.NetworkConnectivityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Base repository interface that provides methods for handling
 * online/offline data access patterns.
 */
interface BaseRepository {
    val networkConnectivityManager: NetworkConnectivityManager
    val TAG: String

    /**
     * Fetches data with offline-first strategy.
     * 1. First tries to load data from local database
     * 2. If online, fetches fresh data from API
     * 3. If offline, returns only local data with appropriate messaging
     *
     * @param localDataSource Function to fetch data from local database
     * @param remoteDataSource Function to fetch data from remote API
     * @param saveRemoteData Function to save remote data to local database
     */
    fun <T> getDataWithOfflineSupport(
        localDataSource: suspend () -> T?,
        remoteDataSource: suspend () -> T,
        saveRemoteData: suspend (T) -> Unit
    ): Flow<NetworkResult<T>> = flow {
        // Emit loading state
        emit(NetworkResult.Loading)

        try {
            // First try to load data from local database
            val localData = localDataSource()
            
            // If local data exists, emit it first
            if (localData != null) {
                Log.d(TAG, "Emitting data from local database")
                emit(NetworkResult.Success(localData))
            }

            // Check network connectivity
            if (networkConnectivityManager.isNetworkAvailable()) {
                try {
                    // Fetch fresh data from API
                    Log.d(TAG, "Fetching data from remote API")
                    val remoteData = remoteDataSource()
                    
                    // Save to local database
                    Log.d(TAG, "Saving remote data to local database")
                    saveRemoteData(remoteData)
                    
                    // Emit remote data
                    emit(NetworkResult.Success(remoteData))
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching data from remote API: ${e.message}")
                    
                    // If we have local data, we've already emitted it
                    // If we don't have local data and remote fetch failed, emit error
                    if (localData == null) {
                        emit(NetworkResult.Error(e))
                    }
                }
            } else {
                Log.d(TAG, "Device is offline, using local data only")
                // If we don't have local data and we're offline, emit error
                if (localData == null) {
                    emit(NetworkResult.Error(Exception("No internet connection and no local data available")))
                }
                // Otherwise we've already emitted the local data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in repository: ${e.message}")
            emit(NetworkResult.Error(e))
        }
    }
} 