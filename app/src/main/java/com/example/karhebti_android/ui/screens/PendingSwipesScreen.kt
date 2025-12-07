package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.karhebti_android.data.api.SwipeResponse
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.viewmodel.MarketplaceViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingSwipesScreen(
    onBackClick: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: MarketplaceViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val pendingSwipes by viewModel.pendingSwipes.observeAsState()
    val swipeResponseResult by viewModel.swipeResponseResult.observeAsState()
    val realtimeNotification by viewModel.realtimeNotification.observeAsState()

    var showChatDialog by remember { mutableStateOf(false) }
    var chatConversationId by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var loadingTimeout by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadPendingSwipes()
       // viewModel.connectWebSocket()
    }

    // Consolidated timeout handler - handles both timeout trigger and reset
    LaunchedEffect(pendingSwipes) {
        when (pendingSwipes) {
            is Resource.Loading -> {
                // Start timeout timer
                kotlinx.coroutines.delay(10000) // 10 seconds
                if (pendingSwipes is Resource.Loading) {
                    loadingTimeout = true
                }
            }
            else -> {
                // Reset timeout when state changes from loading
                loadingTimeout = false
            }
        }
    }

    // Handle swipe response result
    LaunchedEffect(swipeResponseResult) {
        when (swipeResponseResult) {
            is Resource.Success -> {
                val result = (swipeResponseResult as Resource.Success).data
                if (result?.status == "accepted" && result.conversationId != null) {
                    chatConversationId = result.conversationId
                    showChatDialog = true
                } else if (result?.status == "declined") {
                    // Just reload the list for decline
                }
                // Reload pending swipes
                viewModel.loadPendingSwipes()
            }
            is Resource.Error -> {
                errorMessage = (swipeResponseResult as Resource.Error).message ?: "Failed to process request"
                showErrorDialog = true
                // Reload to reset UI
                viewModel.loadPendingSwipes()
            }
            else -> {}
        }
    }

    // Handle new swipe notifications
    LaunchedEffect(realtimeNotification) {
        realtimeNotification?.let {
            if (it.type == "swipe_right") {
                viewModel.loadPendingSwipes()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
           // viewModel.disconnectWebSocket()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pending Requests") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPendingSwipes() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
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
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Unable to load pending requests. Please check your internet connection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = {
                            loadingTimeout = false
                            viewModel.loadPendingSwipes()
                        }) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
                pendingSwipes is Resource.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                pendingSwipes is Resource.Success -> {
                    val swipes = (pendingSwipes as Resource.Success).data ?: emptyList()
                    if (swipes.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No pending requests",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "You'll see requests here when buyers are interested in your cars",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(swipes) { swipe ->
                                PendingSwipeCard(
                                    swipe = swipe,
                                    isProcessing = swipeResponseResult is Resource.Loading,
                                    onAccept = { viewModel.acceptSwipe(swipe.id) },
                                    onDecline = { viewModel.declineSwipe(swipe.id) }
                                )
                            }
                        }
                    }
                }
                pendingSwipes is Resource.Error -> {
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
                            "Error loading requests",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            (pendingSwipes as Resource.Error).message ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.loadPendingSwipes() }) {
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

            // Chat dialog
            if (showChatDialog && chatConversationId != null) {
                AlertDialog(
                    onDismissRequest = { showChatDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = { Text("Request Accepted!") },
                    text = { Text("You can now chat with the buyer about your car.") },
                    confirmButton = {
                        Button(onClick = {
                            showChatDialog = false
                            chatConversationId?.let { onNavigateToChat(it) }
                        }) {
                            Text("Start Chat")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showChatDialog = false }) {
                            Text("Later")
                        }
                    }
                )
            }

            // Error dialog
            if (showErrorDialog) {
                AlertDialog(
                    onDismissRequest = { showErrorDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = { Text("Error") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        Button(onClick = { showErrorDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PendingSwipeCard(
    swipe: SwipeResponse,
    isProcessing: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var isProcessingState by remember { mutableStateOf(false) }

    // Get buyer name and car name from the populated details
    val buyerName = swipe.buyerDetails?.let { "${it.prenom} ${it.nom}" } ?: "Someone"
    val carName = swipe.carDetails?.let { "${it.marque} ${it.modele}" } ?: "your car"

    // Format date
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateText = swipe.createdAt?.let { dateFormat.format(it) } ?: "Unknown date"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "$buyerName is interested in your $carName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Decline")
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept")
                }
            }
        }
    }
}

fun formatSwipeTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> "$hours hours ago"
        days < 7 -> "$days days ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }
}
