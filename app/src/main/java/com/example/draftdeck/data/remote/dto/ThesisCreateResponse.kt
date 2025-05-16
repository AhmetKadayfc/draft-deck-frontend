package com.example.draftdeck.data.remote.dto

import com.example.draftdeck.data.model.Thesis
import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Data class representing the server response for thesis creation endpoint
 */
data class ThesisCreateResponse(
    val message: String,
    val thesis: ThesisCreateResponseDto
)

data class ThesisCreateResponseDto(
    val id: String,
    val title: String,
    val status: String,
    
    @SerializedName("thesis_type")
    val thesisType: String,
    
    @SerializedName("created_at")
    val createdAt: Date?,
    
    @SerializedName("file_name")
    val fileName: String? = null
)

/**
 * Extension function to convert a thesis creation response to a complete Thesis model
 * Fills in missing fields with reasonable defaults
 */
fun ThesisCreateResponseDto.toThesis(
    studentId: String,
    studentName: String,
    description: String = "",
    advisorId: String = "",
    advisorName: String = ""
): Thesis {
    val now = Date()
    
    // Determine file type from file name
    val fileType = when {
        fileName?.endsWith(".pdf", ignoreCase = true) == true -> "pdf"
        fileName?.endsWith(".docx", ignoreCase = true) == true -> "docx"
        else -> "" // Default to empty string if file name is missing or unrecognized
    }
    
    return Thesis(
        id = id,
        title = title,
        description = description,
        studentId = studentId,
        studentName = studentName,
        advisorId = advisorId,
        advisorName = advisorName,
        submissionType = thesisType,
        fileUrl = "", // Will be empty for newly created theses
        fileType = fileType,
        version = 1, // First version
        status = status,
        submissionDate = createdAt ?: now,
        lastUpdated = createdAt ?: now
    )
} 