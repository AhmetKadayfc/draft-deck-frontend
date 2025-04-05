package com.example.draftdeck.ui.util

import com.example.draftdeck.data.remote.NetworkResult

/**
 * Extension function to add an Idle case handler to existing when expressions
 * This helps handle the new Idle state for screens that haven't been updated yet
 */
inline fun <T, R> NetworkResult<T>.handleWhenWithIdle(
    onIdle: () -> R,
    onLoading: () -> R,
    onSuccess: (T) -> R,
    onError: (Exception) -> R
): R {
    return when (this) {
        is NetworkResult.Idle -> onIdle()
        is NetworkResult.Loading -> onLoading()
        is NetworkResult.Success -> onSuccess(data)
        is NetworkResult.Error -> onError(exception)
    }
} 