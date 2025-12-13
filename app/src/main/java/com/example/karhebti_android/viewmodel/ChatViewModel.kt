package com.example.karhebti_android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.karhebti_android.data.api.*
import com.example.karhebti_android.data.preferences.TokenManager
import com.example.karhebti_android.data.repository.ChatRepository
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.utils.NotificationHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

/**
 * Singleton ChatViewModel - manages all chat-related functionality
 * Separated from MarketplaceViewModel for better architecture
 */
class ChatViewModel private constructor(application: Application) : AndroidViewModel(application) {

    companion object {
        @Volatile
        private var INSTANCE: ChatViewModel? = null

        fun getInstance(application: Application): ChatViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatViewModel(application).also { INSTANCE = it }
            }
        }
    }

    private val tokenManager = TokenManager(application)
    private val repository: ChatRepository by lazy {
        ChatRepository.getInstance(RetrofitClient.apiService, tokenManager.getToken() ?: "")
    }

    // CRITICAL: Add NotificationHelper
    private val notificationHelper = NotificationHelper(application.applicationContext)

    // Current conversation
    private val _currentConversation = MutableLiveData<Resource<ConversationResponse>>()
    val currentConversation: LiveData<Resource<ConversationResponse>> = _currentConversation

    // Messages for current conversation - Using mutableStateListOf would be better but LiveData for compatibility
    private val _messages = MutableLiveData<Resource<List<ChatMessage>>>()
    val messages: LiveData<Resource<List<ChatMessage>>> = _messages

    // Real-time messages (from WebSocket)
    private val _realtimeMessage = MutableLiveData<ChatMessage?>()
    val realtimeMessage: LiveData<ChatMessage?> = _realtimeMessage

    // Conversations list
    private val _conversations = MutableLiveData<Resource<List<ConversationResponse>>>()
    val conversations: LiveData<Resource<List<ConversationResponse>>> = _conversations

    // User typing indicator
    private val _userTyping = MutableLiveData<Pair<String, String>?>() // userId, conversationId
    val userTyping: LiveData<Pair<String, String>?> = _userTyping

    // WebSocket connection status
    private val _isWebSocketConnected = MutableLiveData<Boolean>()
    val isWebSocketConnected: LiveData<Boolean> = _isWebSocketConnected

    // Notifications (moved from MarketplaceViewModel)
    private val _realtimeNotification = MutableLiveData<NotificationResponse>()
    val realtimeNotification: LiveData<NotificationResponse> = _realtimeNotification

    // Track if WebSocket is initialized
    private var isWebSocketInitialized = false

    // Cache of messages to prevent race conditions
    private val messageCache = mutableMapOf<String, MutableList<ChatMessage>>()
    private val messageCacheLock = Any()

    // Keep track of the last message ID to force UI updates
    private var lastMessageId: String = ""

    init {
        android.util.Log.d("ChatViewModel", "ChatViewModel singleton instance created")
    }

    // ==================== CONVERSATIONS ====================

    fun loadConversations() {
        viewModelScope.launch {
            _conversations.value = Resource.Loading()
            _conversations.value = repository.getConversations()
        }
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            _currentConversation.value = Resource.Loading()
            val result = repository.getConversation(conversationId)
            _currentConversation.value = result

            // Log the conversation details to debug name display
            if (result is Resource.Success) {
                android.util.Log.d("ChatViewModel", "Loaded conversation: ${result.data?.id}")
                android.util.Log.d("ChatViewModel", "Other user: ${result.data?.otherUser?.nom} ${result.data?.otherUser?.prenom}")
                android.util.Log.d("ChatViewModel", "Car: ${result.data?.carDetails?.marque} ${result.data?.carDetails?.modele}")
            }
        }
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            _messages.value = Resource.Loading()
            val result = repository.getMessages(conversationId)

            // Initialize cache with loaded messages
            if (result is Resource.Success && result.data != null) {
                synchronized(messageCacheLock) {
                    messageCache[conversationId] = result.data.toMutableList()
                }
            }

            _messages.value = result
        }
    }

    fun sendMessage(conversationId: String, content: String) {
        viewModelScope.launch {
            val result = repository.sendMessage(conversationId, content)
            if (result is Resource.Success && result.data != null) {
                android.util.Log.d("ChatViewModel", "✅ Message sent successfully: ${result.data.id}")
                // Add message to cache and update UI immediately
                addMessageToList(conversationId, result.data)
            }
        }
    }

    fun markConversationAsRead(conversationId: String) {
        viewModelScope.launch {
            repository.markConversationAsRead(conversationId)
        }
    }

    // ==================== WEBSOCKET ====================

    fun connectWebSocket() {
        // CRITICAL FIX: Check if WebSocket is actually connected, not just initialized
        if (isWebSocketInitialized && _isWebSocketConnected.value == true) {
            android.util.Log.d("ChatViewModel", "WebSocket already connected and active")
            return
        }

        // If initialized but not connected, force reconnection
        if (isWebSocketInitialized && _isWebSocketConnected.value != true) {
            android.util.Log.d("ChatViewModel", "⚠️ WebSocket was initialized but disconnected, forcing reconnection")
            disconnectWebSocket()
        }

        android.util.Log.d("ChatViewModel", "Initializing WebSocket connection")
        isWebSocketInitialized = true

        repository.initWebSocket(
            onMessageReceived = { message ->
                android.util.Log.d("ChatViewModel", "📨 ========================================")
                android.util.Log.d("ChatViewModel", "📨 WebSocket message received in ViewModel")
                android.util.Log.d("ChatViewModel", "📨 Message ID: ${message.id}")
                android.util.Log.d("ChatViewModel", "📨 Conversation: ${message.conversationId}")
                android.util.Log.d("ChatViewModel", "📨 Sender: ${message.senderId}")
                android.util.Log.d("ChatViewModel", "📨 Content: ${message.content}")
                android.util.Log.d("ChatViewModel", "📨 ========================================")

                // CRITICAL: Track last message ID
                lastMessageId = message.id

                // CRITICAL FIX: Use viewModelScope to ensure proper threading
                viewModelScope.launch(Dispatchers.Main) {
                    // Update realtime message trigger
                    _realtimeMessage.value = message

                    // Add to message list immediately
                    addMessageToList(message.conversationId, message)

                    // Reload conversations list to update last message
                    launch { loadConversations() }
                }
            },
            onNotificationReceived = { notification ->
                android.util.Log.d("ChatViewModel", "🔔 Notification received: ${notification.type}")

                viewModelScope.launch(Dispatchers.Main) {
                    // CRITICAL FIX: Display actual Android notification
                    android.util.Log.d("ChatViewModel", "📲 Displaying Android notification...")
                    try {
                        notificationHelper.showNotification(notification)
                        android.util.Log.d("ChatViewModel", "✅ Android notification displayed")
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "❌ Failed to display notification: ${e.message}", e)
                    }

                    // Post notification for UI to handle
                    _realtimeNotification.postValue(notification)

                    // Handle new message notifications
                    if (notification.type == "new_message") {
                        val conversationId = notification.data?.get("conversationId") as? String
                        val currentConvId = _currentConversation.value?.data?.id

                        android.util.Log.d("ChatViewModel", "New message notification for conversation: $conversationId (current: $currentConvId)")

                        // Reload messages if needed (backup mechanism)
                        if (conversationId != null && conversationId == currentConvId) {
                            android.util.Log.d("ChatViewModel", "🔄 Reloading messages due to notification")
                            launch {
                                val result = repository.getMessages(conversationId)
                                if (result is Resource.Success) {
                                    synchronized(messageCacheLock) {
                                        messageCache[conversationId] = result.data?.toMutableList() ?: mutableListOf()
                                    }
                                    _messages.value = result
                                    android.util.Log.d("ChatViewModel", "✅ Messages reloaded: ${result.data?.size} messages")
                                }
                            }
                        }

                        // Always reload conversations list
                        launch { loadConversations() }
                    }
                }
            },
            onUserTyping = { userId, conversationId ->
                viewModelScope.launch(Dispatchers.Main) {
                    _userTyping.value = Pair(userId, conversationId)

                    // Clear typing indicator after 3 seconds
                    launch {
                        kotlinx.coroutines.delay(3000)
                        if (_userTyping.value?.first == userId && _userTyping.value?.second == conversationId) {
                            _userTyping.value = null
                        }
                    }
                }
            },
            onConnectionChanged = { isConnected ->
                android.util.Log.d("ChatViewModel", "WebSocket connection status changed: $isConnected")
                viewModelScope.launch(Dispatchers.Main) {
                    _isWebSocketConnected.value = isConnected

                    if (!isConnected) {
                        android.util.Log.w("ChatViewModel", "⚠️ WebSocket disconnected - Will auto-reconnect")
                    }
                }
            }
        )
    }

    /**
     * CRITICAL: Thread-safe method to add messages to the list
     */
    private fun addMessageToList(conversationId: String, message: ChatMessage) {
        synchronized(messageCacheLock) {
            val currentConvId = _currentConversation.value?.data?.id

            // Initialize cache if needed
            if (!messageCache.containsKey(conversationId)) {
                val existingMessages = (_messages.value as? Resource.Success)?.data?.toMutableList() ?: mutableListOf()
                messageCache[conversationId] = existingMessages
            }

            val cachedMessages = messageCache[conversationId]!!

            // Check for duplicates by ID
            val isDuplicate = cachedMessages.any { it.id == message.id }

            if (!isDuplicate) {
                cachedMessages.add(message)
                android.util.Log.d("ChatViewModel", "✅ Added message ${message.id} to cache. Total: ${cachedMessages.size}")

                // Update LiveData on main thread if this is the current conversation
                if (conversationId == currentConvId) {
                    // Create new list to trigger LiveData observers
                    val newList = ArrayList(cachedMessages)
                    _messages.value = Resource.Success(newList)

                    // ALSO force a value change by posting to ensure UI update
                    _messages.postValue(Resource.Success(newList))

                    android.util.Log.d("ChatViewModel", "✅ Updated LiveData with ${newList.size} messages")
                }else{
                    android.util.Log.d("ChatViewModel", "ℹ️ Message ${message.id} added to cache for conversation $conversationId, not current conversation")
                }
            } else {
                android.util.Log.d("ChatViewModel", "⚠️ Duplicate message ${message.id} ignored")
            }
        }
    }

    fun disconnectWebSocket() {
        android.util.Log.d("ChatViewModel", "Disconnecting WebSocket")
        repository.disconnectWebSocket()
        isWebSocketInitialized = false
        _isWebSocketConnected.value = false
    }

    fun joinConversation(conversationId: String) {
        repository.joinConversation(conversationId)
    }

    fun leaveConversation(conversationId: String) {
        repository.leaveConversation(conversationId)
    }

    fun sendTypingIndicator(conversationId: String) {
        repository.sendTypingIndicator(conversationId)
    }

    // Clear realtime message after it's been processed
    fun clearRealtimeMessage() {
        _realtimeMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        android.util.Log.d("ChatViewModel", "ChatViewModel cleared")
        disconnectWebSocket()
    }
}
