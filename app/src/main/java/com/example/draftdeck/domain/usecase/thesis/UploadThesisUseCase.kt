package com.example.draftdeck.domain.usecase.thesis

import android.net.Uri
import android.content.Context
import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.data.model.User
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.repository.ThesisRepository
import com.example.draftdeck.domain.util.Constants
import com.example.draftdeck.domain.util.FileHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

class UploadThesisUseCase @Inject constructor(
    private val thesisRepository: ThesisRepository,
    private val fileHelper: FileHelper
) {
    suspend operator fun invoke(
        context: Context,
        title: String,
        description: String,
        fileUri: Uri,
        submissionType: String,
        currentUser: User,
        advisorId: String
    ): Flow<NetworkResult<Thesis>> = flow {
        emit(NetworkResult.Loading)

        try {
            val file = fileHelper.getFileFromUri(fileUri, context)
                ?: throw Exception("Failed to process the file")

            // Validate file size
            if (!fileHelper.isFileSizeValid(file, Constants.MAX_FILE_SIZE_MB)) {
                throw Exception("File size exceeds the maximum limit of ${Constants.MAX_FILE_SIZE_MB}MB")
            }

            // Validate file extension
            val extension = fileHelper.getFileExtension(file.name)?.lowercase() ?: ""
            if (!Constants.SUPPORTED_DOCUMENT_FORMATS.split(",").contains(extension)) {
                throw Exception("Unsupported file format. Please upload PDF or DOCX files")
            }

            thesisRepository.uploadThesis(
                title = title,
                description = description,
                studentId = currentUser.id,
                advisorId = advisorId,
                submissionType = submissionType,
                file = file
            ).collect { result ->
                emit(result)
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }
}