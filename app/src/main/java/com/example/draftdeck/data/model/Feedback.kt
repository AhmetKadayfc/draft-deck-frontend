package com.example.draftdeck.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Feedback(
    val id: String,
    val thesisId: String,
    val advisorId: String,
    val advisorName: String,
    val overallRemarks: String,
    val inlineComments: List<InlineComment>,
    val status: String, // Pending, Completed
    val createdDate: Date,
) : Parcelable

@Parcelize
data class InlineComment(
    val id: String,
    val pageNumber: Int,
    val position: CommentPosition,
    val content: String,
    val type: String, // Suggestion, Correction, Question
) : Parcelable

@Parcelize
data class CommentPosition(
    val x: Float,
    val y: Float,
) : Parcelable