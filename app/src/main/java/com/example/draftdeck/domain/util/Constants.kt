package com.example.draftdeck.domain.util

object Constants {
    // API Endpoints
    const val BASE_URL = "https://api.draftdeck.example.com/"

    // Preferences Keys
    const val PREFERENCES_NAME = "draft_deck_preferences"
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_USER_ROLE = "user_role"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_PROFILE = "user_profile"

    // Database
    const val DATABASE_NAME = "draft_deck_database"

    // File handling
    const val MAX_FILE_SIZE_MB = 10
    const val SUPPORTED_DOCUMENT_FORMATS = "pdf,docx"

    // Roles
    const val ROLE_STUDENT = "student"
    const val ROLE_ADVISOR = "advisor"
    const val ROLE_ADMIN = "admin"

    // Submission Status
    const val STATUS_PENDING = "pending"
    const val STATUS_REVIEWED = "reviewed"
    const val STATUS_APPROVED = "approved"
    const val STATUS_REJECTED = "rejected"
}