package com.example.draftdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.draftdeck.data.local.entity.FeedbackEntity
import com.example.draftdeck.data.local.entity.FeedbackWithComments
import com.example.draftdeck.data.local.entity.InlineCommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {
    @Transaction
    @Query("SELECT * FROM feedback WHERE thesisId = :thesisId")
    fun getFeedbackForThesis(thesisId: String): Flow<List<FeedbackWithComments>>

    @Query("SELECT * FROM feedback WHERE id = :feedbackId")
    fun getFeedbackById(feedbackId: String): Flow<FeedbackEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInlineComments(comments: List<InlineCommentEntity>)

    @Transaction
    suspend fun insertFeedbackWithComments(
        feedback: FeedbackEntity,
        comments: List<InlineCommentEntity>
    ) {
        insertFeedback(feedback)
        insertInlineComments(comments)
    }

    @Query("DELETE FROM feedback WHERE id = :feedbackId")
    suspend fun deleteFeedback(feedbackId: String)

    @Query("DELETE FROM inline_comments WHERE feedbackId = :feedbackId")
    suspend fun deleteInlineComments(feedbackId: String)

    @Transaction
    suspend fun deleteFeedbackWithComments(feedbackId: String) {
        deleteInlineComments(feedbackId)
        deleteFeedback(feedbackId)
    }

    @Query("DELETE FROM feedback")
    suspend fun clearFeedbacks()

    @Query("DELETE FROM inline_comments")
    suspend fun clearInlineComments()
}