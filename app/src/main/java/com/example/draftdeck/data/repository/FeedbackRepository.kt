package com.example.draftdeck.data.repository

import android.content.Context
import android.util.Log
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
import com.example.draftdeck.domain.util.NetworkConnectivityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
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
    private val fileHelper: FileHelper,
    override val networkConnectivityManager: NetworkConnectivityManager
) : FeedbackRepository, BaseRepository {

    override val TAG = "FeedbackRepoDebug"

    override fun getFeedbackForThesis(thesisId: String): Flow<NetworkResult<List<Feedback>>> {
        return getDataWithOfflineSupport(
            localDataSource = {
                try {
                    val localFeedback = feedbackDao.getFeedbackForThesis(thesisId)
                        .map { feedbackWithComments -> feedbackWithComments.map { it.toFeedback() } }
                        .first()
                    
                    if (localFeedback.isNotEmpty()) localFeedback else null
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading feedback from local database: ${e.message}")
                    null
                }
            },
            remoteDataSource = {
                val response = feedbackApi.getFeedbackForThesis(thesisId)
                if (response.isSuccessful) {
                    response.body()?.map { it.toFeedback() } ?: throw IOException("Empty response body")
                } else {
                    throw HttpException(response)
                }
            },
            saveRemoteData = { feedbacks ->
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
            }
        )
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
        overallRemarks: String,
        inlineComments: List<InlineComment>
    ): Flow<NetworkResult<Feedback>> = flow {
        emit(NetworkResult.Loading)

        try {
            val commentRequests = inlineComments.map { comment ->
                InlineCommentRequest(
                    content = comment.content,
                    pageNumber = comment.pageNumber,
                    positionX = comment.position.x,
                    positionY = comment.position.y
                )
            }

            val request = CreateFeedbackRequest(
                thesisId = thesisId,
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
                    content = comment.content,
                    pageNumber = comment.pageNumber,
                    positionX = comment.position.x,
                    positionY = comment.position.y
                )
            }

            val request = CreateFeedbackRequest(
                thesisId = existingFeedback.thesisId,
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