package com.example.draftdeck.ui.navigation

sealed class Screen(val route: String) {
    // Auth screens
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object EmailConfirmation : Screen("email_confirmation")

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