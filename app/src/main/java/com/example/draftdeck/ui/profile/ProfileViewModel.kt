package com.example.draftdeck.ui.profile

import android.net.Uri
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.remote.api.UpdateProfileRequest
import com.example.draftdeck.domain.usecase.auth.GetCurrentUserUseCase
import com.example.draftdeck.data.repository.AuthRepository
import com.example.draftdeck.data.remote.api.UserApi
import com.example.draftdeck.data.remote.dto.toUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val userApi: UserApi,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = getCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _updateProfileResult = MutableStateFlow<NetworkResult<User>?>(null)
    val updateProfileResult: StateFlow<NetworkResult<User>?> = _updateProfileResult

    private val _updatePasswordResult = MutableStateFlow<NetworkResult<Unit>?>(null)
    val updatePasswordResult: StateFlow<NetworkResult<Unit>?> = _updatePasswordResult

    private val _updateProfilePictureResult = MutableStateFlow<NetworkResult<User>?>(null)
    val updateProfilePictureResult: StateFlow<NetworkResult<User>?> = _updateProfilePictureResult

    fun updateProfile(firstName: String, lastName: String, phoneNumber: String?) {
        viewModelScope.launch {
            _updateProfileResult.value = NetworkResult.Loading

            try {
                currentUser.value?.id?.let { userId ->
                    val request = UpdateProfileRequest(
                        firstName = firstName,
                        lastName= lastName,
                        phoneNumber = phoneNumber
                    )

                    val response = userApi.updateUserProfile(userId, request)

                    if (response.isSuccessful) {
                        response.body()?.toUser()?.let { updatedUser ->
                            _updateProfileResult.value = NetworkResult.Success(updatedUser)
                        } ?: run {
                            _updateProfileResult.value = NetworkResult.Error(Exception("Empty response body"))
                        }
                    } else {
                        _updateProfileResult.value = NetworkResult.Error(Exception("Failed to update profile: ${response.code()}"))
                    }
                } ?: run {
                    _updateProfileResult.value = NetworkResult.Error(Exception("User not authenticated"))
                }
            } catch (e: Exception) {
                _updateProfileResult.value = NetworkResult.Error(e)
            }
        }
    }

    fun updatePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _updatePasswordResult.value = NetworkResult.Loading

            try {
                currentUser.value?.id?.let { userId ->
                    val response = userApi.updatePassword(userId, oldPassword, newPassword)

                    if (response.isSuccessful) {
                        _updatePasswordResult.value = NetworkResult.Success(Unit)
                    } else {
                        _updatePasswordResult.value = NetworkResult.Error(Exception("Failed to update password: ${response.code()}"))
                    }
                } ?: run {
                    _updatePasswordResult.value = NetworkResult.Error(Exception("User not authenticated"))
                }
            } catch (e: Exception) {
                _updatePasswordResult.value = NetworkResult.Error(e)
            }
        }
    }

    fun updateProfilePicture(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _updateProfilePictureResult.value = NetworkResult.Loading

            try {
                currentUser.value?.id?.let { userId ->
                    // Convert Uri to File
                    val file = File(context.cacheDir, "profile_picture.jpg")
                    context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                        file.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

                    val response = userApi.updateProfilePicture(userId, filePart)

                    if (response.isSuccessful) {
                        response.body()?.toUser()?.let { updatedUser ->
                            _updateProfilePictureResult.value = NetworkResult.Success(updatedUser)
                        } ?: run {
                            _updateProfilePictureResult.value = NetworkResult.Error(Exception("Empty response body"))
                        }
                    } else {
                        _updateProfilePictureResult.value = NetworkResult.Error(Exception("Failed to update profile picture: ${response.code()}"))
                    }
                } ?: run {
                    _updateProfilePictureResult.value = NetworkResult.Error(Exception("User not authenticated"))
                }
            } catch (e: Exception) {
                _updateProfilePictureResult.value = NetworkResult.Error(e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout().collectLatest { /* result not needed */ }
        }
    }

    fun resetUpdateProfileResult() {
        _updateProfileResult.value = null
    }

    fun resetUpdatePasswordResult() {
        _updatePasswordResult.value = null
    }

    fun resetUpdateProfilePictureResult() {
        _updateProfilePictureResult.value = null
    }
}