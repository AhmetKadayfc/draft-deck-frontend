package com.example.draftdeck.ui.dashboard

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.draftdeck.data.model.Thesis
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.data.remote.handle
import com.example.draftdeck.domain.util.Constants
import com.example.draftdeck.ui.components.DraftDeckAppBar
import com.example.draftdeck.ui.components.ErrorView
import com.example.draftdeck.ui.components.LoadingIndicator
import com.example.draftdeck.ui.thesis.components.ThesisCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dashboard screen for student users to view and manage their theses.
 * Supports status filtering, search, and thesis creation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    viewModel: DashboardViewModel,
    onNavigateToThesisDetail: (String) -> Unit,
    onNavigateToUploadThesis: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val TAG = "StudentDashboardDebug"
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var isInitialLoad by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState()
    val thesisList by viewModel.thesisList.collectAsState()

    // Debug logging for current state
    LaunchedEffect(currentUser) {
        Log.d(TAG, "Current user updated: ${currentUser?.id} - ${currentUser?.role}")
    }
    
    LaunchedEffect(thesisList) {
        Log.d(TAG, "Thesis list state updated: $thesisList")
        when (thesisList) {
            is NetworkResult.Success -> {
                val data = (thesisList as NetworkResult.Success<List<Thesis>>).data
                Log.d(TAG, "Successfully received ${data.size} theses")
                data.forEach { thesis ->
                    Log.d(TAG, "Thesis: ${thesis.id} - ${thesis.title}")
                }
            }
            is NetworkResult.Error -> {
                val error = (thesisList as NetworkResult.Error).exception
                Log.e(TAG, "Error in thesis list: ${error.message}", error)
            }
            is NetworkResult.Loading -> {
                Log.d(TAG, "Thesis list is loading")
            }
            is NetworkResult.Idle -> {
                Log.d(TAG, "Thesis list is idle")
            }
        }
    }

    // Status filter tabs
    val filterTabs = listOf("All", "Pending", "Reviewed", "Approved")

    // Combined LaunchedEffect to handle loading based on filtering changes
    // This prevents multiple simultaneous API calls
    LaunchedEffect(isInitialLoad, selectedTabIndex, searchQuery) {
        if (isInitialLoad) {
            // Initial load without filters
            Log.d(TAG, "Initial load - calling loadThesisList()")
            viewModel.loadThesisList()
            isInitialLoad = false
        } else {
            // Apply filters
            val status = when (selectedTabIndex) {
                0 -> null
                1 -> Constants.STATUS_PENDING
                2 -> Constants.STATUS_REVIEWED
                3 -> Constants.STATUS_APPROVED
                else -> null
            }
            
            Log.d(TAG, "Applying filters - Tab: ${filterTabs[selectedTabIndex]}, Status: ${status ?: "ALL"}, Query: $searchQuery")
            
            // Debounce search input to prevent too many API calls
            delay(300)
            
            // Set all parameters at once and make a single API call
            viewModel.applyFilters(
                status = status,
                query = if (searchQuery.isBlank()) null else searchQuery
            )
        }
    }

    Scaffold(
        topBar = {
            DraftDeckAppBar(
                title = "Thesis",
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentUser?.role == Constants.ROLE_STUDENT) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToUploadThesis,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Thesis"
                        )
                    },
                    text = { Text("New Thesis") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { 
                    searchQuery = it
                },
                onSearch = { 
                    isSearchActive = false
                },
                active = isSearchActive,
                onActiveChange = { isSearchActive = it },
                placeholder = { Text("Search thesis") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search suggestions would go here
            }

            // Status filter tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                filterTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            // Thesis list content
            thesisList.handle(
                onIdle = {
                    // Do nothing in initial state
                },
                onLoading = {
                    LoadingIndicator(fullScreen = true)
                },
                onSuccess = { data ->
                    if (data.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No theses found",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(data) { thesis ->
                                ThesisCard(
                                    thesis = thesis,
                                    onClick = { onNavigateToThesisDetail(thesis.id) }
                                )
                            }
                        }
                    }
                },
                onError = { exception ->
                    ErrorView(
                        message = "Failed to load theses: ${exception.message}",
                        onRetry = { viewModel.refreshThesisList() }
                    )
                }
            )
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun StudentDashboardPreview() {
//    // Creating sample data for preview
//    val sampleTheses = listOf(
//        Thesis(
//            id = "1",
//            title = "Analysis of Machine Learning Algorithms",
//            description = "Comparative study of various ML algorithms",
//            studentId = "s1",
//            studentName = "John Doe",
//            advisorId = "a1",
//            advisorName = "Dr. Smith",
//            submissionType = "Draft",
//            fileUrl = "url",
//            fileType = "pdf",
//            version = 1,
//            status = Constants.STATUS_PENDING,
//            submissionDate = Date(),
//            lastUpdated = Date()
//        ),
//        Thesis(
//            id = "2",
//            title = "Impact of Social Media on Youth",
//            description = "Research on the psychological effects of social media usage",
//            studentId = "s1",
//            studentName = "John Doe",
//            advisorId = "a1",
//            advisorName = "Dr. Smith",
//            submissionType = "Final",
//            fileUrl = "url",
//            fileType = "pdf",
//            version = 2,
//            status = Constants.STATUS_REVIEWED,
//            submissionDate = Date(),
//            lastUpdated = Date()
//        )
//    )
//
//    val sampleUser = User(
//        id = "s1",
//        email = "john@example.com",
//        firstName = "John",
//        lastName = "Doe",
//        role = Constants.ROLE_STUDENT,
//        advisorName = "Dr. Smith",
//        thesisCount = 2
//    )
//
//    DraftDeckTheme {
//        StudentDashboard(
//            viewModel = PreviewViewModelProvider.getDashboardViewModel(),
//            onNavigateToThesisDetail = {},
//            onNavigateToUploadThesis = {},
//            onNavigateToProfile = {},
//            onNavigateToNotifications = {},
//            onLogout = {}
//        )
//    }
//}