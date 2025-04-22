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
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Date::class.java, FlexibleDateAdapter())
            .create()
    }

    /**
     * Custom date adapter that tries multiple ISO-8601 format patterns
     * to handle different date formats from the API
     */
    class FlexibleDateAdapter : JsonDeserializer<Date> {
        private val formatters = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )

        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): Date? {
            if (json == null || !json.isJsonPrimitive) {
                return null
            }

            val dateString = json.asString
            
            for (formatter in formatters) {
                try {
                    return formatter.parse(dateString)
                } catch (e: ParseException) {
                    // Try the next formatter
                }
            }
            
            throw JsonParseException("Unable to parse date: $dateString")
        }
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