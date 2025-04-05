package com.example.draftdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.draftdeck.data.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val phoneNumber: String?,
    val advisorName: String?,
    val thesisCount: Int,
    val profilePictureUrl: String?
)

fun UserEntity.toUser(): User {
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

fun User.toUserEntity(): UserEntity {
    return UserEntity(
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