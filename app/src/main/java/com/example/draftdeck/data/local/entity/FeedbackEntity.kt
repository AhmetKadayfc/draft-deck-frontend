package com.example.draftdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.Embedded
import com.example.draftdeck.data.model.CommentPosition
import com.example.draftdeck.data.model.Feedback
import com.example.draftdeck.data.model.InlineComment
import java.util.Date

@Entity(tableName = "feedback")
data class FeedbackEntity(
    @PrimaryKey
    val id: String,
    val thesisId: String,
    val advisorId: String,
    val advisorName: String,
    val overallRemarks: String,
    val status: String,
    val createdDate: Date
)

@Entity(tableName = "inline_comments")
data class InlineCommentEntity(
    @PrimaryKey
    val id: String,
    val feedbackId: String,
    val pageNumber: Int,
    val positionX: Float,
    val positionY: Float,
    val content: String,
    val type: String
)

data class FeedbackWithComments(
    @Embedded
    val feedback: FeedbackEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "feedbackId"
    )
    val comments: List<InlineCommentEntity>
)


fun FeedbackWithComments.toFeedback(): Feedback {
    return Feedback(
        id = feedback.id,
        thesisId = feedback.thesisId,
        advisorId = feedback.advisorId,
        advisorName = feedback.advisorName,
        overallRemarks = feedback.overallRemarks,
        inlineComments = comments.map { it.toInlineComment() },
        status = feedback.status,
        createdDate = feedback.createdDate
    )
}

fun InlineCommentEntity.toInlineComment(): InlineComment {
    return InlineComment(
        id = id,
        pageNumber = pageNumber,
        position = CommentPosition(
            x = positionX,
            y = positionY
        ),
        content = content,
        type = type
    )
}