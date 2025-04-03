package com.example.draftdeck.data.remote.dto

import com.example.draftdeck.data.model.User

data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val surname: String,
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
        name = name,
        surname = surname,
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
        name = name,
        surname = surname,
        role = role,
        phoneNumber = phoneNumber,
        advisorName = advisorName,
        thesisCount = thesisCount,
        profilePictureUrl = profilePictureUrl
    )
}