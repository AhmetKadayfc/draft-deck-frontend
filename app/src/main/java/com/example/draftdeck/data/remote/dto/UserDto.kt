package com.example.draftdeck.data.remote.dto

import com.example.draftdeck.data.model.User
import com.google.gson.annotations.SerializedName

data class UserDto(
    val id: String,
    val email: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    val role: String,
    val phoneNumber: String? = null,
    val advisorName: String? = null,
    val thesisCount: Int = 0,
    val profilePictureUrl: String? = null,
)

fun UserDto.toUser(): User {
    return User(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        role = role,
        phoneNumber = phoneNumber,
        advisorName = advisorName,
        thesisCount = thesisCount,
        profilePictureUrl = profilePictureUrl
    )
}

fun User.toUserDto(): UserDto {
    return UserDto(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        role = role,
        phoneNumber = phoneNumber,
        advisorName = advisorName,
        thesisCount = thesisCount,
        profilePictureUrl = profilePictureUrl
    )
}