package com.example.draftdeck.ui.navigation

import android.util.Log

sealed class Screen(val route: String) {
    // Auth screens
    object Welcome : Screen("welcome")
    object Login : Screen("login?email={email}&fromVerification={fromVerification}") {
        fun createRoute(email: String? = null, fromVerification: Boolean = false): String {
            return "login?email=${email ?: ""}&fromVerification=$fromVerification"
        }
    }
    object Register : Screen("register")
    object EmailConfirmation : Screen("email_confirmation/{email}") {
        fun createRoute(email: String): String = "email_confirmation/$email"
    }
    object ForgotPassword : Screen("forgot_password")
    object PasswordResetVerification : Screen("password_reset_verification/{email}") {
        fun createRoute(email: String): String = "password_reset_verification/$email"
    }
    object ResetPassword : Screen("reset_password/{email}/{code}") {
        fun createRoute(email: String, code: String): String = "reset_password/$email/$code"
    }

    // Main screens
    object ThesisList : Screen("thesis_list")
    object ThesisDetail : Screen("thesis_detail/{thesisId}") {
        fun createRoute(thesisId: String): String = "thesis_detail/$thesisId"
    }
    object UploadThesis : Screen("upload_thesis")
    object UpdateThesis : Screen("update_thesis/{thesisId}") {
        fun createRoute(thesisId: String): String {
            val route = "update_thesis/$thesisId"
            Log.d("Screen", "UpdateThesis.createRoute called with thesisId: $thesisId, route: $route")
            return route
        }
    }

    // Feedback screens
    object FeedbackList : Screen("feedback_list/{thesisId}") {
        fun createRoute(thesisId: String): String = "feedback_list/$thesisId"
    }
    object AddFeedback : Screen("add_feedback/{thesisId}") {
        fun createRoute(thesisId: String): String = "add_feedback/$thesisId"
    }
    object FeedbackDetail : Screen("feedback_detail/{feedbackId}") {
        fun createRoute(feedbackId: String): String = "feedback_detail/$feedbackId"
    }

    // User profile screens
    object Profile : Screen("profile")
    object Notifications : Screen("notifications")
    
    // Admin screens
    object UserManagement : Screen("user_management")
    object AssignAdvisor : Screen("assign_advisor/{thesisId}") {
        fun createRoute(thesisId: String): String = "assign_advisor/$thesisId"
    }
    object StudentThesesList : Screen("student_theses/{studentId}") {
        fun createRoute(studentId: String): String = "student_theses/$studentId"
    }
}