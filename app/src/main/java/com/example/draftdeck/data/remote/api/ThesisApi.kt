package com.example.draftdeck.data.remote.api

import com.example.draftdeck.data.remote.dto.ThesisDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ThesisApi {
    @GET("theses")
    suspend fun getAllTheses(): Response<List<ThesisDto>>

    @GET("theses/student/{studentId}")
    suspend fun getThesesByStudentId(@Path("studentId") studentId: String): Response<List<ThesisDto>>

    @GET("theses/advisor/{advisorId}")
    suspend fun getThesesByAdvisorId(@Path("advisorId") advisorId: String): Response<List<ThesisDto>>

    @GET("theses/{thesisId}")
    suspend fun getThesisById(@Path("thesisId") thesisId: String): Response<ThesisDto>

    @Multipart
    @POST("theses")
    suspend fun uploadThesis(
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("studentId") studentId: RequestBody,
        @Part("advisorId") advisorId: RequestBody,
        @Part("submissionType") submissionType: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<ThesisDto>

    @Multipart
    @PUT("theses/{thesisId}")
    suspend fun updateThesis(
        @Path("thesisId") thesisId: String,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("submissionType") submissionType: RequestBody,
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
    suspend fun downloadThesis(@Path("thesisId") thesisId: String): Response<okhttp3.ResponseBody>
}