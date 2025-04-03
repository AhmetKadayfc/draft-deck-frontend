package com.example.draftdeck.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.draftdeck.data.local.dao.FeedbackDao
import com.example.draftdeck.data.local.dao.ThesisDao
import com.example.draftdeck.data.local.dao.UserDao
import com.example.draftdeck.data.local.entity.FeedbackEntity
import com.example.draftdeck.data.local.entity.InlineCommentEntity
import com.example.draftdeck.data.local.entity.ThesisEntity
import com.example.draftdeck.data.local.entity.UserEntity
import com.example.draftdeck.data.local.util.Converters

@Database(
    entities = [
        UserEntity::class,
        ThesisEntity::class,
        FeedbackEntity::class,
        InlineCommentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ThesisDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun thesisDao(): ThesisDao
    abstract fun feedbackDao(): FeedbackDao
}