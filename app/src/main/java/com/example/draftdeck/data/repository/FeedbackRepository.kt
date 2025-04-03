package com.example.draftdeck.data.repository

import android.content.Context
import com.example.draftdeck.data.local.dao.FeedbackDao
import com.example.draftdeck.data.local.entity.FeedbackEntity
import com.example.draftdeck.data.local.entity.InlineCommentEntity
import com.example.draftdeck.data.local.entity.toFeedback
import com.example.draftdeck.data.model.Feedback
import com.example.draftdeck.data.model.InlineComment
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.remote.api.CreateFeedbackRequest
import com.example.draftdeck.data.remote.api.FeedbackApi
import com.example.draftdeck.data.remote.api.InlineCommentRequest
import com.example.draftdeck.data.remote.dto.toFeedback
import com.example.draftdeck.domain.util.FileHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.util.Date
import javax.inject.Inject

interface FeedbackRepository {
    fun getFeedbackForThesis(thesisId: String): Flow<NetworkResult<List<Feedback>>>
    fun getFeedbackById(feedbackId: String): Flow<NetworkResult<Feedback>>
    suspend fun createFeedback(
        thesisId: String,
        advisorId: String,
        advisorName: String,
        overallRemarks: String,
        inlineComments: List<InlineComment>
    ): Flow<NetworkResult<Feedback>>
    suspend fun updateFeedback(
        feedbackId: String,
        overallRemarks: String,
        inlineComments: List<InlineComment>
    ): Flow<NetworkResult<Feedback>>
    suspend fun deleteFeedback(feedbackId: String): Flow<NetworkResult<Unit>>
    suspend fun exportFeedbackAsPdf(
        feedbackId: String,
        context: Context
    ): Flow<NetworkResult<File>>
}

class FeedbackRepositoryImpl @Inject constructor(
    private val feedbackApi: FeedbackApi,
    private val feedbackDao: FeedbackDao,
    private val fileHelper: FileHelper
) : FeedbackRepository {

    override fun getFeedbackForThesis(thesisId: String): Flow<NetworkResult<List<Feedback>>> = flow {
        emit(NetworkResult.Loading)

        // First emit data from local database
        feedbackDao.getFeedbackForThesis(thesisId)
            .map { feedbackWithComments -> feedbackWithComments.map { it.toFeedback() } }
            .collect { localFeedback ->
                emit(NetworkResult.Success(localFeedback))
            }

        // Then fetch from remote and update local
        try {
            val response = feedbackApi.getFeedbackForThesis(thesisId)
            if (response.isSuccessful) {
                response.body()?.let { dtos ->
                    val feedbacks = dtos.map { it.toFeedback() }

                    // Save to local database
                    feedbacks.forEach { feedback ->
                        val feedbackEntity = FeedbackEntity(
                            id = feedback.id,
                            thesisId = feedback.thesisId,
                            advisorId = feedback.advisorId,
                            advisorName = feedback.advisorName,
                            overallRemarks = feedback.overallRemarks,
                            status = feedback.status,
                            createdDate = feedback.createdDate
                        )

                        val commentEntities = feedback.inlineComments.map { comment ->
                            InlineCommentEntity(
                                id = comment.id,
                                feedbackId = feedback.id,
                                pageNumber = comment.pageNumber,
                                positionX = comment.position.x,
                                positionY = comment.position.y,
                                content = comment.content,
                                type = comment.type
                            )
                        }

                        feedbackDao.insertFeedbackWithComments(feedbackEntity, commentEntities)
                    }

                    emit(NetworkResult.Success(feedbacks))
                } ?: emit(NetworkResult.Error(Exception("Empty response body")))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override fun getFeedbackById(feedbackId: String): Flow<NetworkResult<Feedback>> = flow {
        emit(NetworkResult.Loading)

        try {
            val response = feedbackApi.getFeedbackById(feedbackId)
            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val feedback = dto.toFeedback()

                    // Save to local database
                    val feedbackEntity = FeedbackEntity(
                        id = feedback.id,
                        thesisId = feedback.thesisId,
                        advisorId = feedback.advisorId,
                        advisorName = feedback.advisorName,
                        overallRemarks = feedback.overallRemarks,
                        status = feedback.status,
                        createdDate = feedback.createdDate
                    )

                    val commentEntities = feedback.inlineComments.map { comment ->
                        InlineCommentEntity(
                            id = comment.id,
                            feedbackId = feedback.id,
                            pageNumber = comment.pageNumber,
                            positionX = comment.position.x,
                            positionY = comment.position.y,
                            content = comment.content,
                            type = comment.type
                        )
                    }

                    feedbackDao.insertFeedbackWithComments(feedbackEntity, commentEntities)

                    emit(NetworkResult.Success(feedback))
                } ?: emit(NetworkResult.Error(Exception("Empty response body")))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun createFeedback(
        thesisId: String,
        advisorId: String,
        advisorName: String,
        overallRemarks: String,
        inlineComments: List<InlineComment>
    ): Flow<NetworkResult<Feedback>> = flow {
        emit(NetworkResult.Loading)

        try {
            val commentRequests = inlineComments.map { comment ->
                InlineCommentRequest(
                    pageNumber = comment.pageNumber,
                    positionX = comment.position.x,
                    positionY = comment.position.y,
                    content = comment.content,
                    type = comment.type
                )
            }

            val request = CreateFeedbackRequest(
                thesisId = thesisId,
                advisorId = advisorId,
                overallRemarks = overallRemarks,
                inlineComments = commentRequests
            )

            val response = feedbackApi.createFeedback(request)

            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val feedback = dto.toFeedback()

                    // Save to local database
                    val feedbackEntity = FeedbackEntity(
                        id = feedback.id,
                        thesisId = feedback.thesisId,
                        advisorId = feedback.advisorId,
                        advisorName = feedback.advisorName,
                        overallRemarks = feedback.overallRemarks,
                        status = feedback.status,
                        createdDate = feedback.createdDate
                    )

                    val commentEntities = feedback.inlineComments.map { comment ->
                        InlineCommentEntity(
                            id = comment.id,
                            feedbackId = feedback.id,
                            pageNumber = comment.pageNumber,
                            positionX = comment.position.x,
                            positionY = comment.position.y,
                            content = comment.content,
                            type = comment.type
                        )
                    }

                    feedbackDao.insertFeedbackWithComments(feedbackEntity, commentEntities)

                    emit(NetworkResult.Success(feedback))
                } ?: emit(NetworkResult.Error(Exception("Empty response body")))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun updateFeedback(
        feedbackId: String,
        overallRemarks: String,
        inlineComments: List<InlineComment>
    ): Flow<NetworkResult<Feedback>> = flow {
        emit(NetworkResult.Loading)

        try {
            // First get existing feedback to get thesisId and advisorId
            val existingFeedback = feedbackApi.getFeedbackById(feedbackId).body()?.toFeedback()
                ?: throw Exception("Failed to get existing feedback")

            val commentRequests = inlineComments.map { comment ->
                InlineCommentRequest(
                    pageNumber = comment.pageNumber,
                    positionX = comment.position.x,
                    positionY = comment.position.y,
                    content = comment.content,
                    type = comment.type
                )
            }

            val request = CreateFeedbackRequest(
                thesisId = existingFeedback.thesisId,
                advisorId = existingFeedback.advisorId,
                overallRemarks = overallRemarks,
                inlineComments = commentRequests
            )

            val response = feedbackApi.updateFeedback(feedbackId, request)

            if (response.isSuccessful) {
                response.body()?.let { dto ->
                    val feedback = dto.toFeedback()

                    // Update local database
                    feedbackDao.deleteFeedbackWithComments(feedbackId)

                    val feedbackEntity = FeedbackEntity(
                        id = feedback.id,
                        thesisId = feedback.thesisId,
                        advisorId = feedback.advisorId,
                        advisorName = feedback.advisorName,
                        overallRemarks = feedback.overallRemarks,
                        status = feedback.status,
                        createdDate = feedback.createdDate
                    )

                    val commentEntities = feedback.inlineComments.map { comment ->
                        InlineCommentEntity(
                            id = comment.id,
                            feedbackId = feedback.id,
                            pageNumber = comment.pageNumber,
                            positionX = comment.position.x,
                            positionY = comment.position.y,
                            content = comment.content,
                            type = comment.type
                        )
                    }

                    feedbackDao.insertFeedbackWithComments(feedbackEntity, commentEntities)

                    emit(NetworkResult.Success(feedback))
                } ?: emit(NetworkResult.Error(Exception("Empty response body")))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun deleteFeedback(feedbackId: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading)

        try {
            val response = feedbackApi.deleteFeedback(feedbackId)

            if (response.isSuccessful) {
                // Delete from local database
                feedbackDao.deleteFeedbackWithComments(feedbackId)

                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }

    override suspend fun exportFeedbackAsPdf(
        feedbackId: String,
        context: Context
    ): Flow<NetworkResult<File>> = flow {
        emit(NetworkResult.Loading)

        try {
            val response = feedbackApi.exportFeedbackAsPdf(feedbackId)

            if (response.isSuccessful) {
                response.body()?.let { responseBody ->
                    val file = fileHelper.saveResponseBodyToFile(
                        responseBody,
                        context.cacheDir,
                        "feedback_$feedbackId.pdf"
                    )
                    emit(NetworkResult.Success(file))
                } ?: emit(NetworkResult.Error(Exception("Empty response body")))
            } else {
                emit(NetworkResult.Error(HttpException(response)))
            }
        } catch (e: HttpException) {
            emit(NetworkResult.Error(e))
        } catch (e: IOException) {
            emit(NetworkResult.Error(e))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e))
        }
    }
}