package com.example.draftdeck.data.remote

sealed class NetworkResult<out T> {
    object Idle : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val exception: Exception) : NetworkResult<Nothing>()
}

/**
 * Handle network result states with a builder pattern
 */
inline fun <T> NetworkResult<T>.handle(
    onIdle: () -> Unit = {},
    onLoading: () -> Unit = {},
    onSuccess: (T) -> Unit = {},
    onError: (Exception) -> Unit = {}
) {
    when (this) {
        is NetworkResult.Idle -> onIdle()
        is NetworkResult.Loading -> onLoading()
        is NetworkResult.Success -> onSuccess(data)
        is NetworkResult.Error -> onError(exception)
    }
}

/**
 * Check if the network result is in a loading state
 */
val NetworkResult<*>.isLoading: Boolean
    get() = this is NetworkResult.Loading