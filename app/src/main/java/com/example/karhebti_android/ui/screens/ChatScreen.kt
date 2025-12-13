package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.karhebti_android.data.api.ChatMessage
import com.example.karhebti_android.data.preferences.TokenManager
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val currentUserId = tokenManager.getUserId()

    // Use singleton ChatViewModel
    val viewModel = remember { ChatViewModel.getInstance(context.applicationContext as android.app.Application) }

    val conversation by viewModel.currentConversation.observeAsState()
    val messages by viewModel.messages.observeAsState()
    val realtimeMessage by viewModel.realtimeMessage.observeAsState()
    val userTyping by viewModel.userTyping.observeAsState()
    val isConnected by viewModel.isWebSocketConnected.observeAsState(false)

    var messageText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // CRITICAL FIX: Create a derived state that forces recomposition
    val messageList = remember(messages) {
        (messages as? Resource.Success)?.data ?: emptyList()
    }

    // Track message count with derived state
    val messageCount = messageList.size

    // Load conversation and messages on first launch
    LaunchedEffect(conversationId) {
        android.util.Log.d("ChatScreen", "🚀 Loading conversation: $conversationId")
        viewModel.loadConversation(conversationId)
        viewModel.loadMessages(conversationId)

        // CRITICAL FIX: Always try to connect WebSocket when screen is opened
        // This ensures reconnection even if navigating back
        viewModel.connectWebSocket()

        // Small delay to ensure WebSocket is connected before joining
        kotlinx.coroutines.delay(500)
        viewModel.joinConversation(conversationId)
        viewModel.markConversationAsRead(conversationId)
    }

    // CRITICAL FIX: Monitor connection status and rejoin if reconnected
    LaunchedEffect(isConnected) {
        if (isConnected) {
            android.util.Log.d("ChatScreen", "✅ WebSocket connected, ensuring we're in room: $conversationId")
            // Small delay to ensure connection is stable
            kotlinx.coroutines.delay(300)
            viewModel.joinConversation(conversationId)
        } else {
            android.util.Log.w("ChatScreen", "⚠️ WebSocket disconnected")
        }
    }

    // CRITICAL FIX: Scroll to bottom when message count changes
    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            android.util.Log.d("ChatScreen", "📨 Message count changed to: $messageCount, scrolling to bottom")
            // Use a small delay to ensure the list is rendered
            kotlinx.coroutines.delay(100)
            scope.launch {
                listState.animateScrollToItem(messageCount - 1)
            }
        }
    }

    // CRITICAL FIX: Also trigger on realtime message for immediate feedback
    LaunchedEffect(realtimeMessage) {
        realtimeMessage?.let { message ->
            if (message.conversationId == conversationId) {
                android.util.Log.d("ChatScreen", "⚡ Real-time message for current conversation, scrolling...")
                kotlinx.coroutines.delay(150)
                if (messageList.isNotEmpty()) {
                    scope.launch {
                        listState.animateScrollToItem(messageList.size - 1)
                    }
                }
            }
            // Clear after processing
            kotlinx.coroutines.delay(500)
            viewModel.clearRealtimeMessage()
        }
    }

    // Handle typing indicator
    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty() && !isTyping) {
            isTyping = true
            viewModel.sendTypingIndicator(conversationId)
        } else if (messageText.isEmpty() && isTyping) {
            isTyping = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("ChatScreen", "Leaving conversation: $conversationId")
            viewModel.leaveConversation(conversationId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = (conversation as? Resource.Success)?.data?.getOtherUser(currentUserId ?: "")?.let {
                                "${it.nom} ${it.prenom}"
                            } ?: "Chat",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (userTyping?.second == conversationId) {
                            Text(
                                text = "typing...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (isConnected) {
                            Text(
                                text = "online",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Car info card
            (conversation as? Resource.Success)?.data?.car?.let { car ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${car.marque} ${car.modele}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Year: ${car.annee} • ${car.kilometrage ?: "N/A"} km",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        car.price?.let {
                            Text(
                                "$${it}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Messages list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (messages) {
                    is Resource.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is Resource.Success -> {
                        // CRITICAL FIX: Use the derived messageList state
                        if (messageList.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No messages yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Start the conversation!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // CRITICAL FIX: Use key parameter to ensure proper recomposition
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    count = messageList.size,
                                    key = { index -> messageList[index].id }
                                ) { index ->
                                    val message = messageList[index]
                                    MessageBubble(
                                        message = message,
                                        isCurrentUser = message.senderId == currentUserId
                                    )
                                }
                            }
                        }
                    }
                    is Resource.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Error loading messages",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    else -> {}
                }
            }

            // Message input
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(conversationId, messageText.trim())
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isCurrentUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentUser)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatMessageTime(message.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrentUser)
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun formatMessageTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date)
    }
}
