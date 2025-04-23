package com.example.draftdeck.data.remote.api

import android.util.Log
import com.example.draftdeck.data.remote.dto.AssignAdvisorResponse
import com.example.draftdeck.data.remote.dto.ThesisCreateResponse
import com.example.draftdeck.data.remote.dto.ThesisDto
import com.example.draftdeck.data.remote.dto.ThesisResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface ThesisApi {
    // Main endpoint that uses the server's role-based approach
    @GET("theses")
    suspend fun getTheses(@QueryMap queryParams: Map<String, String> = emptyMap()): Response<ThesisResponse>

    @GET("theses/{thesisId}")
    suspend fun getThesisById(@Path("thesisId") thesisId: String): Response<ThesisDto>

    @Multipart
    @POST("theses")
    suspend fun uploadThesis(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("thesis_type") thesisType: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ThesisCreateResponse>

    @Multipart
    @PUT("theses/{thesisId}")
    suspend fun updateThesis(
        @Path("thesisId") thesisId: String,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("thesis_type") thesisType: RequestBody,
        @Part file: MultipartBody.Part?
    ): Response<ThesisDto>

    @PUT("theses/{thesisId}/status")
    suspend fun updateThesisStatus(
        @Path("thesisId") thesisId: String,
        @Body status: String
    ): Response<ThesisDto>

    @DELETE("theses/{thesisId}")
    suspend fun deleteThesis(@Path("thesisId") thesisId: String): Response<Unit>

    @GET("theses/download/{thesisId}")
    suspend fun downloadThesis(@Path("thesisId") thesisId: String): Response<ResponseBody>

    // Regular assign advisor endpoint (assigns the current logged-in user)
    @POST("theses/{thesisId}/assign")
    suspend fun assignAdvisorToThesis(
        @Path("thesisId") thesisId: String
    ): Response<AssignAdvisorResponse>
    
    // Admin endpoint to assign a specific advisor to a thesis
    // Using the backend implementation which expects advisor_id in the request body
    @POST("theses/{thesisId}/assign")
    suspend fun adminAssignAdvisorToThesis(
        @Path("thesisId") thesisId: String,
        @Body advisorIdRequest: Map<String, String>
    ): Response<AssignAdvisorResponse>
}

// Data class for the advisor ID request body
data class AdvisorIdRequest(val advisor_id: String)

// Extension function to simplify calling the admin assign endpoint
suspend fun ThesisApi.adminAssignAdvisorToThesis(
    thesisId: String,
    advisorId: String
): Response<AssignAdvisorResponse> {
    Log.d("ThesisApi", "Creating request to assign advisor $advisorId to thesis $thesisId")
    
    try {
        // Create request body with advisor_id parameter
        val requestBody = mapOf("advisor_id" to advisorId)
        
        val response = this.adminAssignAdvisorToThesis(thesisId, requestBody)
        Log.d("ThesisApi", "Response code: ${response.code()}")
        Log.d("ThesisApi", "Response successful: ${response.isSuccessful}")
        
        response.body()?.let {
            Log.d("ThesisApi", "Response message: ${it.message}")
            Log.d("ThesisApi", "Response thesis: ${it.thesis}")
            Log.d("ThesisApi", "Response thesis ID: ${it.thesis.id}")
            Log.d("ThesisApi", "Response advisor ID: ${it.thesis.advisorId}")
        } ?: Log.e("ThesisApi", "Response body is null")
        
        return response
    } catch (e: Exception) {
        Log.e("ThesisApi", "Exception in adminAssignAdvisorToThesis: ${e.message}", e)
        throw e
    }
}