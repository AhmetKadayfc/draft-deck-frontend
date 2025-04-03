package com.example.draftdeck.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String,
    val email: String,
    val name: String,
    val surname: String,
    val role: String,
    val phoneNumber: String? = null,
    val advisorName: String? = null,
    val thesisCount: Int = 0,
    val profilePictureUrl: String? = null,
) : Parcelable