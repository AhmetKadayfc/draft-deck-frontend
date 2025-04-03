package com.example.draftdeck.di

import android.content.Context
import androidx.room.Room
import com.example.draftdeck.data.local.ThesisDatabase
import com.example.draftdeck.data.local.dao.FeedbackDao
import com.example.draftdeck.data.local.dao.ThesisDao
import com.example.draftdeck.data.local.dao.UserDao
import com.example.draftdeck.domain.util.Constants
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create()
    }

    @Provides
    @Singleton
    fun provideThesisDatabase(
        @ApplicationContext context: Context
    ): ThesisDatabase {
        return Room.databaseBuilder(
            context,
            ThesisDatabase::class.java,
            Constants.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: ThesisDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideThesisDao(database: ThesisDatabase): ThesisDao {
        return database.thesisDao()
    }

    @Provides
    @Singleton
    fun provideFeedbackDao(database: ThesisDatabase): FeedbackDao {
        return database.feedbackDao()
    }
}