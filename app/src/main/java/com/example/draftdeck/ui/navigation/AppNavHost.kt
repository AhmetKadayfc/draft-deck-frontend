package com.example.draftdeck.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.draftdeck.domain.util.Constants
import com.example.draftdeck.ui.auth.AuthViewModel
import com.example.draftdeck.ui.auth.EmailConfirmationScreen
import com.example.draftdeck.ui.auth.ForgotPasswordScreen
import com.example.draftdeck.ui.auth.LoginScreen
import com.example.draftdeck.ui.auth.PasswordResetCodeVerificationScreen
import com.example.draftdeck.ui.auth.RegisterScreen
import com.example.draftdeck.ui.auth.ResetPasswordScreen
import com.example.draftdeck.ui.auth.WelcomeScreen
import com.example.draftdeck.ui.dashboard.AdvisorDashboard
import com.example.draftdeck.ui.dashboard.DashboardViewModel
import com.example.draftdeck.ui.dashboard.StudentDashboard
import com.example.draftdeck.ui.feedback.AddFeedbackScreen
import com.example.draftdeck.ui.feedback.FeedbackDetailScreen
import com.example.draftdeck.ui.feedback.FeedbackScreen
import com.example.draftdeck.ui.feedback.FeedbackViewModel
import com.example.draftdeck.ui.notifications.NotificationScreen
import com.example.draftdeck.ui.notifications.NotificationViewModel
import com.example.draftdeck.ui.profile.ProfileScreen
import com.example.draftdeck.ui.profile.ProfileViewModel
import com.example.draftdeck.ui.thesis.ThesisDetailScreen
import com.example.draftdeck.ui.thesis.ThesisViewModel
import com.example.draftdeck.ui.thesis.UploadThesisScreen
import com.example.draftdeck.ui.admin.AdminViewModel
import com.example.draftdeck.ui.admin.UserManagementScreen
import com.example.draftdeck.ui.admin.AssignAdvisorScreen
import com.example.draftdeck.ui.admin.StudentThesesScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // Get the shared auth view model
    val authViewModel: AuthViewModel = hiltViewModel()

    // Check if user is logged in
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = false)
    val startDestination = if (isLoggedIn) Screen.ThesisList.route else Screen.Welcome.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Auth flow
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(
            route = Screen.Login.route,
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("fromVerification") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) {
            val viewModel: AuthViewModel = hiltViewModel()
            // Get email parameter, ensure it's not empty or the placeholder
            val email = it.arguments?.getString("email")?.let { email ->
                if (email.isEmpty() || email == "{email}") null else email
            }
            val fromVerification = it.arguments?.getBoolean("fromVerification") ?: false
            
            LoginScreen(
                viewModel = viewModel,
                email = email,
                fromVerification = fromVerification,
                onLoginSuccess = {
                    navController.navigate(Screen.ThesisList.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToEmailVerification = { unverifiedEmail ->
                    // Navigate to email verification screen with the unverified email
                    navController.navigate(Screen.EmailConfirmation.createRoute(unverifiedEmail))
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        composable(Screen.Register.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = { email ->
                    navController.navigate(Screen.EmailConfirmation.createRoute(email))
                },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }

        composable(
            route = Screen.EmailConfirmation.route,
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val viewModel: AuthViewModel = hiltViewModel()
            val email = backStackEntry.arguments?.getString("email")
            EmailConfirmationScreen(
                viewModel = viewModel,
                email = email,
                onConfirmSuccess = {
                    // Navigate to login screen with pre-filled email and success message
                    navController.navigate(Screen.Login.createRoute(email = email, fromVerification = true)) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // Password reset flow
        composable(Screen.ForgotPassword.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVerifyCode = { email ->
                    navController.navigate(Screen.PasswordResetVerification.createRoute(email))
                }
            )
        }
        
        composable(
            route = Screen.PasswordResetVerification.route,
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val viewModel: AuthViewModel = hiltViewModel()
            val email = backStackEntry.arguments?.getString("email")
            PasswordResetCodeVerificationScreen(
                viewModel = viewModel,
                email = email,
                onNavigateBack = { navController.popBackStack() },
                onVerifySuccess = { code ->
                    val currentEmail = email ?: ""
                    navController.navigate(Screen.ResetPassword.createRoute(currentEmail, code)) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = false }
                    }
                }
            )
        }
        
        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                },
                navArgument("code") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val viewModel: AuthViewModel = hiltViewModel()
            val email = backStackEntry.arguments?.getString("email")
            val code = backStackEntry.arguments?.getString("code")
            ResetPasswordScreen(
                viewModel = viewModel,
                email = email,
                code = code,
                onNavigateBack = { navController.popBackStack() },
                onResetSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                    }
                }
            )
        }

        // Main screens
        composable(Screen.ThesisList.route) {
            val viewModel: DashboardViewModel = hiltViewModel()
            val currentUser by viewModel.currentUser.collectAsState(initial = null)

            if (currentUser?.role == Constants.ROLE_STUDENT){
                StudentDashboard(
                    viewModel = viewModel,
                    onNavigateToThesisDetail = { thesisId ->
                        navController.navigate(Screen.ThesisDetail.createRoute(thesisId))
                    },
                    onNavigateToUploadThesis = {
                        navController.navigate(Screen.UploadThesis.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.ThesisList.route) { inclusive = true }
                        }
                    }
                )
            }
            else if (currentUser?.role == Constants.ROLE_ADMIN) {
                AdvisorDashboard(
                    viewModel = viewModel,
                    isAdmin = true,
                    onNavigateToThesisDetail = { thesisId ->
                        navController.navigate(Screen.ThesisDetail.createRoute(thesisId))
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onNavigateToUserManagement = {
                        navController.navigate(Screen.UserManagement.route)
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.ThesisList.route) { inclusive = true }
                        }
                    }
                )
            }
            else {
                AdvisorDashboard(
                    viewModel = viewModel,
                    onNavigateToThesisDetail = { thesisId ->
                        navController.navigate(Screen.ThesisDetail.createRoute(thesisId))
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.ThesisList.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(
            route = Screen.ThesisDetail.route,
            arguments = listOf(
                navArgument("thesisId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val thesisId = backStackEntry.arguments?.getString("thesisId") ?: ""
            val viewModel: ThesisViewModel = hiltViewModel()

            ThesisDetailScreen(
                thesisId = thesisId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToFeedback = { thesisId ->
                    navController.navigate(Screen.FeedbackList.createRoute(thesisId))
                },
                onNavigateToUpdateThesis = { thesisId ->
                    Log.d("AppNavHost", "onNavigateToUpdateThesis called with thesisId: $thesisId")
                    navController.navigate(Screen.UpdateThesis.createRoute(thesisId))
                },
                onNavigateToAddFeedback = { thesisId ->
                    navController.navigate(Screen.AddFeedback.createRoute(thesisId))
                }
            )
        }

        composable(Screen.UploadThesis.route) {
            UploadThesisScreen(
                onBackClick = { navController.popBackStack() },
                onSuccessNavigate = {
                    navController.navigate(Screen.ThesisList.route) {
                        popUpTo(Screen.ThesisList.route)
                    }
                }
            )
        }

        composable(
            route = Screen.UpdateThesis.route,
            arguments = listOf(
                navArgument("thesisId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val thesisId = backStackEntry.arguments?.getString("thesisId") ?: ""
            Log.d("AppNavHost", "Navigated to UpdateThesis with thesis ID: $thesisId")

            UploadThesisScreen(
                thesisId = thesisId,
                onBackClick = { navController.popBackStack() },
                onSuccessNavigate = {
                    navController.navigate(Screen.ThesisDetail.createRoute(thesisId)) {
                        popUpTo(Screen.ThesisDetail.createRoute(thesisId)) { inclusive = true }
                    }
                }
            )
        }

        // Feedback screens
        composable(
            route = Screen.FeedbackList.route,
            arguments = listOf(
                navArgument("thesisId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val thesisId = backStackEntry.arguments?.getString("thesisId") ?: ""
            val viewModel: FeedbackViewModel = hiltViewModel()

            FeedbackScreen(
                thesisId = thesisId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToFeedbackDetail = { feedbackId ->
                    navController.navigate(Screen.FeedbackDetail.createRoute(feedbackId))
                },
                onNavigateToAddFeedback = { feedbackThesisId ->
                    navController.navigate(Screen.AddFeedback.createRoute(feedbackThesisId))
                }
            )
        }

        composable(
            route = Screen.AddFeedback.route,
            arguments = listOf(
                navArgument("thesisId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val thesisId = backStackEntry.arguments?.getString("thesisId") ?: ""
            val viewModel: FeedbackViewModel = hiltViewModel()

            AddFeedbackScreen(
                thesisId = thesisId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onFeedbackSuccess = {
                    navController.navigate(Screen.FeedbackList.createRoute(thesisId)) {
                        popUpTo(Screen.ThesisDetail.createRoute(thesisId))
                    }
                }
            )
        }

        composable(
            route = Screen.FeedbackDetail.route,
            arguments = listOf(
                navArgument("feedbackId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val feedbackId = backStackEntry.arguments?.getString("feedbackId") ?: ""
            val viewModel: FeedbackViewModel = hiltViewModel()

            FeedbackDetailScreen(
                feedbackId = feedbackId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToUpdateFeedback = { feedbackId ->
                    // Navigate to update feedback (not implemented separately,
                    // could reuse AddFeedbackScreen with isUpdate = true)
                    navController.popBackStack()
                }
            )
        }

        // Profile and notification screens
        composable(Screen.Profile.route) {
            val viewModel: ProfileViewModel = hiltViewModel()

            ProfileScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.ThesisList.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Notifications.route) {
            val viewModel: NotificationViewModel = hiltViewModel()

            NotificationScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToRelatedItem = { type, itemId ->
                    when (type) {
                        "Feedback" -> navController.navigate(Screen.FeedbackDetail.createRoute(itemId))
                        "StatusUpdate", "Reminder" -> navController.navigate(Screen.ThesisDetail.createRoute(itemId))
                        else -> navController.popBackStack()
                    }
                }
            )
        }

        // Admin screens
        composable(Screen.UserManagement.route) {
            val viewModel: AdminViewModel = hiltViewModel()

            UserManagementScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onViewStudentTheses = { studentId ->
                    navController.navigate(Screen.StudentThesesList.createRoute(studentId))
                }
            )
        }
        
        composable(
            route = Screen.StudentThesesList.route,
            arguments = listOf(
                navArgument("studentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: ""
            val viewModel: AdminViewModel = hiltViewModel()

            StudentThesesScreen(
                studentId = studentId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onAssignAdvisor = { thesisId ->
                    navController.navigate(Screen.AssignAdvisor.createRoute(thesisId))
                },
                onNavigateToThesisDetail = { thesisId ->
                    navController.navigate(Screen.ThesisDetail.createRoute(thesisId))
                }
            )
        }

        composable(
            route = Screen.AssignAdvisor.route,
            arguments = listOf(
                navArgument("thesisId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val thesisId = backStackEntry.arguments?.getString("thesisId") ?: ""
            val viewModel: AdminViewModel = hiltViewModel()

            AssignAdvisorScreen(
                thesisId = thesisId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onAssignSuccess = {
                    navController.popBackStack()
                }
            )
        }
    }
}