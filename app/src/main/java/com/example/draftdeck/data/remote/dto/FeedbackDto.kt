package com.example.draftdeck.data.remote.dto

import com.example.draftdeck.data.model.CommentPosition
import com.example.draftdeck.data.model.Feedback
import com.example.draftdeck.data.model.InlineComment
import java.util.Date

data class FeedbackDto(
    val id: String,
    val thesisId: String,
    val advisorId: String,
    val advisorName: String,
    val overallRemarks: String,
    val inlineComments: List<InlineCommentDto>,
    val status: String,
    val createdDate: Date,
)

data class InlineCommentDto(
    val id: String,
    val pageNumber: Int,
    val position: CommentPositionDto,
    val content: String,
    val type: String,
)

data class CommentPositionDto(
    val x: Float,
    val y: Float,
)

fun FeedbackDto.toFeedback(): Feedback {
    return Feedback(
        id = id,
        thesisId = thesisId,
        advisorId = advisorId,
        advisorName = advisorName,
        overallRemarks = overallRemarks,
        inlineComments = inlineComments.map { it.toInlineComment() },
        status = status,
        createdDate = createdDate
    )
}

fun InlineCommentDto.toInlineComment(): InlineComment {
    return InlineComment(
        id = id,
        pageNumber = pageNumber,
        position = position.toCommentPosition(),
        content = content,
        type = type
    )
}

fun CommentPositionDto.toCommentPosition(): CommentPosition {
    return CommentPosition(
        x = x,
        y = y
    )
}