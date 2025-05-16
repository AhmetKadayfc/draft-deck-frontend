package com.example.draftdeck.data.remote.dto

import com.example.draftdeck.data.model.Feedback
import com.google.gson.annotations.SerializedName

data class FeedbackListResponseDto(
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("feedback")
    val feedback: List<FeedbackDto>,
    
    @SerializedName("thesis_id")
    val thesisId: String,
    
    @SerializedName("thesis_title")
    val thesisTitle: String
)

fun FeedbackListResponseDto.toFeedbackList(): List<Feedback> {
    return this.feedback.map { it.toFeedback() }
} 