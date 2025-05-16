package com.example.draftdeck.data.remote.api

import com.example.draftdeck.data.remote.dto.FeedbackDto
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class CreateFeedbackRequest(
    @SerializedName("thesis_id")
    val thesisId: String,
    
    @SerializedName("overall_comments")
    val overallRemarks: String,
    
    @SerializedName("comments")
    val inlineComments: List<InlineCommentRequest>,
    
    @SerializedName("rating")
    val rating: Int? = null,
    
    @SerializedName("recommendations")
    val recommendations: String? = null
)

data class InlineCommentRequest(
    @SerializedName("content")
    val content: String,
    
    @SerializedName("page")
    val pageNumber: Int,
    
    @SerializedName("position_x")
    val positionX: Float,
    
    @SerializedName("position_y")
    val positionY: Float
)

interface FeedbackApi {
    @GET("feedback/thesis/{thesisId}")
    suspend fun getFeedbackForThesis(@Path("thesisId") thesisId: String): Response<List<FeedbackDto>>

    @GET("feedback/{feedbackId}")
    suspend fun getFeedbackById(@Path("feedbackId") feedbackId: String): Response<FeedbackDto>

    @POST("feedback")
    suspend fun createFeedback(@Body request: CreateFeedbackRequest): Response<FeedbackDto>

    @PUT("feedback/{feedbackId}")
    suspend fun updateFeedback(
        @Path("feedbackId") feedbackId: String,
        @Body request: CreateFeedbackRequest
    ): Response<FeedbackDto>

    @DELETE("feedback/{feedbackId}")
    suspend fun deleteFeedback(@Path("feedbackId") feedbackId: String): Response<Unit>

    @GET("feedback/export/{feedbackId}")
    suspend fun exportFeedbackAsPdf(@Path("feedbackId") feedbackId: String): Response<okhttp3.ResponseBody>
}