package com.example.draftdeck.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.draftdeck.ui.auth.AuthViewModel
import com.example.draftdeck.ui.auth.EmailConfirmationScreen
import com.example.draftdeck.ui.auth.LoginScreen
import com.example.draftdeck.ui.auth.RegisterScreen
import com.example.draftdeck.ui.auth.WelcomeScreen
import com.example.draftdeck.ui.dashboard.DashboardViewModel
import com.example.draftdeck.ui.dashboard.StudentDashboard
import com.example.draftdeck.ui.feedback.AddFeedbackScreen
import com.example.draftdeck.ui.feedback.FeedbackScreen
import com.example.draftdeck.ui.feedback.FeedbackViewModel
import com.example.draftdeck.ui.thesis.ThesisDetailScreen
import com.example.draftdeck.ui.thesis.ThesisViewModel
import com.example.draftdeck.ui.thesis.UploadThesisScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // Get the shared auth view model
    val authViewModel: AuthViewModel = hiltViewModel()

    // Check if user is logged in
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = true)
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

        composable(Screen.Login.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.ThesisList.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.EmailConfirmation.route)
                },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }

        composable(Screen.EmailConfirmation.route) {
            val viewModel: AuthViewModel = hiltViewModel()
            EmailConfirmationScreen(
                viewModel = viewModel,
                onConfirmSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        // Main screens
        composable(Screen.ThesisList.route) {
            val viewModel: DashboardViewModel = hiltViewModel()
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
                    navController.navigate(Screen.UpdateThesis.createRoute(thesisId))
                },
                onNavigateToAddFeedback = { thesisId ->
                    navController.navigate(Screen.AddFeedback.createRoute(thesisId))
                }
            )
        }

        composable(Screen.UploadThesis.route) {
            val viewModel: ThesisViewModel = hiltViewModel()

            UploadThesisScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onUploadSuccess = { thesisId ->
                    navController.navigate(Screen.ThesisDetail.createRoute(thesisId)) {
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
            val viewModel: ThesisViewModel = hiltViewModel()

            UploadThesisScreen(
                viewModel = viewModel,
                thesisId = thesisId,
                isUpdate = true,
                onBackClick = { navController.popBackStack() },
                onUploadSuccess = { updatedThesisId ->
                    navController.navigate(Screen.ThesisDetail.createRoute(updatedThesisId)) {
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

            // We'll implement this screen later
        }
    }
}