package com.example.draftdeck.ui.navigation

sealed class Screen(val route: String) {
    // Auth screens
    object Welcome : Screen("welcome")
    object Login : Screen("login?email={email}&fromVerification={fromVerification}") {
        fun createRoute(email: String? = null, fromVerification: Boolean = false): String {
            // Only include email param if it's not null or empty
            val emailParam = if (email.isNullOrEmpty()) {
                ""
            } else {
                "email=$email"
            }
            
            val verificationParam = "fromVerification=$fromVerification"
            
            // Create route with proper query params
            return if (emailParam.isEmpty()) {
                "login?$verificationParam"
            } else {
                "login?$emailParam&$verificationParam"
            }
        }
    }
    object Register : Screen("register")
    object EmailConfirmation : Screen("email_confirmation/{email}") {
        fun createRoute(email: String) = "email_confirmation/$email"
    }

    // Main screens
    object ThesisList : Screen("thesis_list")
    object ThesisDetail : Screen("thesis_detail/{thesisId}") {
        fun createRoute(thesisId: String) = "thesis_detail/$thesisId"
    }
    object UploadThesis : Screen("upload_thesis")
    object UpdateThesis : Screen("update_thesis/{thesisId}") {
        fun createRoute(thesisId: String) = "update_thesis/$thesisId"
    }

    // Feedback screens
    object FeedbackList : Screen("feedback_list/{thesisId}") {
        fun createRoute(thesisId: String) = "feedback_list/$thesisId"
    }
    object AddFeedback : Screen("add_feedback/{thesisId}") {
        fun createRoute(thesisId: String) = "add_feedback/$thesisId"
    }
    object FeedbackDetail : Screen("feedback_detail/{feedbackId}") {
        fun createRoute(feedbackId: String) = "feedback_detail/$feedbackId"
    }

    // User profile screens
    object Profile : Screen("profile")
    object Notifications : Screen("notifications")
}