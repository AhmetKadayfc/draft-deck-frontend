package com.example.draftdeck.di

import com.example.draftdeck.data.repository.AuthRepository
import com.example.draftdeck.data.repository.AuthRepositoryImpl
import com.example.draftdeck.data.repository.FeedbackRepository
import com.example.draftdeck.data.repository.FeedbackRepositoryImpl
import com.example.draftdeck.data.repository.ThesisRepository
import com.example.draftdeck.data.repository.ThesisRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindThesisRepository(
        thesisRepositoryImpl: ThesisRepositoryImpl
    ): ThesisRepository

    @Binds
    @Singleton
    abstract fun bindFeedbackRepository(
        feedbackRepositoryImpl: FeedbackRepositoryImpl
    ): FeedbackRepository
}