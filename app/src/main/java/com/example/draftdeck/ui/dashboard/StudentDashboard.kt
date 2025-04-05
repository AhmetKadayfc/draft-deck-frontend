package com.example.draftdeck.ui.dashboard

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.draftdeck.data.remote.NetworkResult
import com.example.draftdeck.domain.util.Constants
import com.example.draftdeck.ui.components.DraftDeckAppBar
import com.example.draftdeck.ui.components.ErrorView
import com.example.draftdeck.ui.components.LoadingIndicator
import com.example.draftdeck.ui.thesis.components.ThesisCard

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
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    val currentUser by viewModel.currentUser.collectAsState()
    val thesisList by viewModel.thesisList.collectAsState()

    val filterTabs = listOf("All", "Pending", "Reviewed", "Approved")

    LaunchedEffect(Unit) {
        viewModel.loadThesisList()
    }

    LaunchedEffect(selectedTabIndex) {
        val status = when (selectedTabIndex) {
            0 -> null
            1 -> Constants.STATUS_PENDING
            2 -> Constants.STATUS_REVIEWED
            3 -> Constants.STATUS_APPROVED
            else -> null
        }
        viewModel.filterThesisByStatus(status)
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
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { isSearchActive = false },
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

            when (val result = thesisList) {
                is NetworkResult.Loading -> {
                    LoadingIndicator(fullScreen = true)
                }
                is NetworkResult.Success -> {
                    val filteredTheses = if (searchQuery.isBlank()) {
                        result.data
                    } else {
                        result.data.filter {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.description.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredTheses.isEmpty()) {
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
                            items(filteredTheses) { thesis ->
                                ThesisCard(
                                    thesis = thesis,
                                    onClick = { onNavigateToThesisDetail(thesis.id) }
                                )
                            }
                        }
                    }
                }
                is NetworkResult.Error -> {
                    ErrorView(
                        message = "Failed to load theses: ${result.exception.message}",
                        onRetry = { viewModel.refreshThesisList() }
                    )
                }
            }
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