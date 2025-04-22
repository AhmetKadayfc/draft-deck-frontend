package com.example.draftdeck.data.remote.dto

import com.example.draftdeck.data.model.Thesis
import com.google.gson.annotations.SerializedName
import java.util.Date

data class ThesisDto(
    val id: String,
    val title: String,
    val description: String? = null,
    
    @SerializedName("student_id")
    val studentId: String,
    
    @SerializedName("student_name")
    val studentName: String,
    
    @SerializedName("advisor_id")
    val advisorId: String?,
    
    @SerializedName("advisor_name")
    val advisorName: String?,
    
    @SerializedName("thesis_type")
    val submissionType: String,
    
    @SerializedName("has_file")
    val hasFile: Boolean,
    
    @SerializedName("file_name")
    val fileName: String?,
    
    @SerializedName("download_url")
    val fileUrl: String?,
    
    val version: Int,
    val status: String,
    
    @SerializedName("submitted_at")
    val submissionDate: Date?,
    
    @SerializedName("created_at")
    val createdAt: Date?,
    
    @SerializedName("updated_at")
    val lastUpdated: Date?
)

fun ThesisDto.toThesis(): Thesis {
    return Thesis(
        id = id,
        title = title,
        description = description ?: "",
        studentId = studentId,
        studentName = studentName,
        advisorId = advisorId ?: "",
        advisorName = advisorName ?: "",
        submissionType = submissionType,
        fileUrl = fileUrl ?: "",
        fileType = fileName?.substringAfterLast(".")?.let { 
            if (it.equals("pdf", ignoreCase = true)) "pdf" else "docx" 
        } ?: "",
        version = version,
        status = status,
        submissionDate = submissionDate ?: createdAt ?: Date(),
        lastUpdated = lastUpdated ?: Date()
    )
}