package com.example.draftdeck.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val type: String, // Feedback, StatusUpdate, Reminder
    val relatedItemId: String?, // Thesis ID or Feedback ID
    val isRead: Boolean,
    val createdDate: Date,
) : Parcelable