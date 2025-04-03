package com.example.draftdeck.data.remote

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val exception: Exception) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}