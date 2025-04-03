package com.example.draftdeck.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Thesis(
    val id: String,
    val title: String,
    val description: String,
    val studentId: String,
    val studentName: String,
    val advisorId: String,
    val advisorName: String,
    val submissionType: String, // Draft or Final
    val fileUrl: String,
    val fileType: String, // pdf or docx
    val version: Int,
    val status: String, // Pending, Reviewed, Approved, Rejected
    val submissionDate: Date,
    val lastUpdated: Date,
) : Parcelable