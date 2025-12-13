package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.ui.components.SwipeableCarCard
import com.example.karhebti_android.viewmodel.MarketplaceViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import androidx.navigation.NavController
import com.example.karhebti_android.ui.components.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceBrowseScreen(
    onBackClick: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    navController: NavController? = null,
    viewModel: MarketplaceViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val availableCars by viewModel.availableCars.observeAsState()
    val swipeResult by viewModel.swipeResult.observeAsState()
    val realtimeNotification by viewModel.realtimeNotification.observeAsState()

    var currentIndex by remember { mutableStateOf(0) }
    var showMatchDialog by remember { mutableStateOf(false) }
    var matchedConversationId by remember { mutableStateOf<String?>(null) }
    var isSwipeInProgress by remember { mutableStateOf(false) }
    var loadingTimeout by remember { mutableStateOf(false) }

    // Load available cars on first composition
    LaunchedEffect(Unit) {
        viewModel.loadAvailableCars()

    }

    // Consolidated timeout handler - handles both timeout trigger and reset
    LaunchedEffect(availableCars) {
        when (availableCars) {
            is Resource.Loading -> {
                // Start timeout timer
                kotlinx.coroutines.delay(10000) // 10 seconds
                if (availableCars is Resource.Loading) {
                    loadingTimeout = true
                }
            }
            else -> {
                // Reset timeout when state changes from loading
                loadingTimeout = false
            }
        }
    }

    // Handle real-time notifications for swipe acceptance
    LaunchedEffect(realtimeNotification) {
        realtimeNotification?.let { notification ->
            if (notification.type == "swipe_accepted") {
                matchedConversationId = notification.data?.get("conversationId") as? String
                showMatchDialog = true
            }
        }
    }

    // Handle swipe result
    LaunchedEffect(swipeResult) {
        when (swipeResult) {
            is Resource.Success -> {
                // Move to next card after successful swipe
                if (isSwipeInProgress) {
                    currentIndex++
                    isSwipeInProgress = false
                }
            }
            is Resource.Error -> {
                // Handle error - still move to next card
                if (isSwipeInProgress) {
                    currentIndex++
                    isSwipeInProgress = false
                }
            }
            else -> {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {

        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse Cars") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Show filters */ }) {
                        Icon(Icons.Default.FilterList, "Filters")
                    }
                }
            )
        },
        bottomBar = {
            if (navController != null) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loadingTimeout -> {
                    // Show timeout error
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Connection Timeout",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Unable to load cars. Please check your internet connection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            loadingTimeout = false
                            currentIndex = 0
                            viewModel.loadAvailableCars()
                        }) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
                availableCars is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                availableCars is Resource.Success -> {
                    val carsState = availableCars as Resource.Success
                    val cars = carsState.data ?: emptyList()

                    if (cars.isEmpty() || currentIndex >= cars.size) {
                        // No more cars
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                if (currentIndex == 0) "No cars available" else "That's all for now!",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (currentIndex == 0) "There are no cars listed for sale at the moment" else "Check back later for more cars",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = {
                                currentIndex = 0
                                viewModel.loadAvailableCars()
                            }) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Refresh")
                            }
                        }
                    } else {
                        // Show current card
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SwipeableCarCard(
                                car = cars[currentIndex],
                                onSwipeLeft = {
                                    viewModel.swipeLeft(cars[currentIndex].id)
                                    isSwipeInProgress = true
                                },
                                onSwipeRight = {
                                    viewModel.swipeRight(cars[currentIndex].id)
                                    isSwipeInProgress = true
                                }
                            )
                        }

                        // Action buttons
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    if (currentIndex < cars.size) {
                                        viewModel.swipeLeft(cars[currentIndex].id)
                                        isSwipeInProgress = true
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.error
                            ) {
                                Icon(Icons.Default.Close, "Pass", modifier = Modifier.size(32.dp))
                            }

                            FloatingActionButton(
                                onClick = {
                                    if (currentIndex < cars.size) {
                                        viewModel.swipeRight(cars[currentIndex].id)
                                        isSwipeInProgress = true
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.Favorite, "Interested", modifier = Modifier.size(32.dp))
                            }
                        }

                        // Card counter
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        ) {
                            Text(
                                text = "${currentIndex + 1} / ${cars.size}",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                availableCars is Resource.Error -> {
                    val errorState = availableCars as Resource.Error
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Error loading cars",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            errorState.message ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            currentIndex = 0
                            viewModel.loadAvailableCars()
                        }) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Match dialog
            if (showMatchDialog && matchedConversationId != null) {
                AlertDialog(
                    onDismissRequest = { showMatchDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = { Text("It's a Match!") },
                    text = { Text("The seller is interested! Start chatting now.") },
                    confirmButton = {
                        Button(onClick = {
                            showMatchDialog = false
                            matchedConversationId?.let { onNavigateToChat(it) }
                        }) {
                            Text("Start Chat")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMatchDialog = false }) {
                            Text("Later")
                        }
                    }
                )
            }
        }
    }
}
