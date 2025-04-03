package com.example.draftdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.draftdeck.data.model.Thesis
import java.util.Date

@Entity(tableName = "theses")
data class ThesisEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val studentId: String,
    val studentName: String,
    val advisorId: String,
    val advisorName: String,
    val submissionType: String,
    val fileUrl: String,
    val fileType: String,
    val version: Int,
    val status: String,
    val submissionDate: Date,
    val lastUpdated: Date
)

fun ThesisEntity.toThesis(): Thesis {
    return Thesis(
        id = id,
        title = title,
        description = description,
        studentId = studentId,
        studentName = studentName,
        advisorId = advisorId,
        advisorName = advisorName,
        submissionType = submissionType,
        fileUrl = fileUrl,
        fileType = fileType,
        version = version,
        status = status,
        submissionDate = submissionDate,
        lastUpdated = lastUpdated
    )
}

fun Thesis.toThesisEntity(): ThesisEntity {
    return ThesisEntity(
        id = id,
        title = title,
        description = description,
        studentId = studentId,
        studentName = studentName,
        advisorId = advisorId,
        advisorName = advisorName,
        submissionType = submissionType,
        fileUrl = fileUrl,
        fileType = fileType,
        version = version,
        status = status,
        submissionDate = submissionDate,
        lastUpdated = lastUpdated
    )
}