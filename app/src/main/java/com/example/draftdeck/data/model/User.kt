package com.example.draftdeck.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val phoneNumber: String? = null,
    val advisorName: String? = null,
    val thesisCount: Int = 0,
    val profilePictureUrl: String? = null
) : Parcelable