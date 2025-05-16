package com.example.draftdeck.data.remote.dto

import com.example.draftdeck.data.model.Thesis
import com.google.gson.annotations.SerializedName
import java.util.Date

data class ThesisDto(
    val id: String?,
    val title: String?,
    val description: String? = null,
    
    @SerializedName("student_id")
    val studentId: String?,
    
    @SerializedName("student_name")
    val studentName: String?,
    
    @SerializedName("advisor_id")
    val advisorId: String?,
    
    @SerializedName("advisor_name")
    val advisorName: String?,
    
    @SerializedName("thesis_type")
    val submissionType: String?,
    
    @SerializedName("has_file")
    val hasFile: Boolean = false,
    
    @SerializedName("file_name")
    val fileName: String?,
    
    @SerializedName("download_url")
    val fileUrl: String?,
    
    val version: Int? = 1,
    val status: String?,
    
    @SerializedName("submitted_at")
    val submissionDate: Date?,
    
    @SerializedName("created_at")
    val createdAt: Date?,
    
    @SerializedName("updated_at")
    val lastUpdated: Date?
)

/**
 * Response class for the assign advisor endpoint
 * Format: {"message": "Thesis assigned successfully", "thesis": {...}}
 * Backend response may include only partial thesis data:
 * {"message": "Thesis assigned successfully", "thesis": {"id": "...", "title": "...", "advisor_id": "...", "updated_at": "..."}}
 */
data class AssignAdvisorResponse(
    val message: String,
    val thesis: ThesisDto
)

// Extension function to convert simplified thesis DTO from advisor assignment to full Thesis
fun AssignAdvisorResponse.toThesis(): Thesis {
    // Convert simplified DTO to domain model with only the provided fields
    return this.thesis.toThesis()
}

fun ThesisDto.toThesis(): Thesis {
    val safeId = id ?: java.util.UUID.randomUUID().toString()
    
    // Determine file type from file name or set default
    val fileType = when {
        fileName?.endsWith(".pdf", ignoreCase = true) == true -> "pdf"
        fileName?.endsWith(".docx", ignoreCase = true) == true -> "docx"
        // Check fileUrl as fallback
        fileUrl?.endsWith(".pdf", ignoreCase = true) == true -> "pdf"
        fileUrl?.endsWith(".docx", ignoreCase = true) == true -> "docx"
        // Default to empty if no info available
        else -> ""
    }
    
    return Thesis(
        id = safeId,
        title = title ?: "Untitled Thesis",
        description = description ?: "",
        studentId = studentId ?: "",
        studentName = studentName ?: "Unknown Student",
        advisorId = advisorId ?: "",
        advisorName = advisorName ?: "",
        submissionType = submissionType ?: "draft",
        fileUrl = fileUrl ?: "",
        fileType = fileType,
        version = version ?: 1,
        status = status ?: "pending",
        submissionDate = submissionDate ?: createdAt ?: Date(),
        lastUpdated = lastUpdated ?: Date()
    )
}