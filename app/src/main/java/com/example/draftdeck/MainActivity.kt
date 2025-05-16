package com.example.draftdeck

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.draftdeck.domain.util.AuthEvent
import com.example.draftdeck.domain.util.AuthNavigator
import com.example.draftdeck.ui.navigation.AppNavHost
import com.example.draftdeck.ui.theme.DraftDeckTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var authNavigator: AuthNavigator
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        
        lifecycleScope.launch {
            authNavigator.authEvents.collectLatest { event ->
                when (event) {
                    is AuthEvent.Unauthorized -> {
                        // Show a toast message informing the user they need to login again
                        Toast.makeText(
                            this@MainActivity,
                            "Your session has expired. Please login again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        
        setContent {
            DraftDeckTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(authNavigator)
                }
            }
        }
    }
}

@Composable
fun MainApp(authNavigator: AuthNavigator) {
    val navController = rememberNavController()
    
    // Observe auth events to handle navigation
    LaunchedEffect(key1 = Unit) {
        authNavigator.authEvents.collectLatest { event ->
            when (event) {
                is AuthEvent.Unauthorized -> {
                    // Navigate to login screen
                    authNavigator.navigateToLogin(navController)
                }
            }
        }
    }

    AppNavHost(navController = navController)
}
