package com.example.karhebti_android.data.repository

import android.util.Log
import com.example.karhebti_android.data.api.*
import com.example.karhebti_android.data.websocket.ChatWebSocketClient
import com.example.karhebti_android.data.polling.ChatPollingService
import kotlinx.coroutines.flow.StateFlow

class MarketplaceRepository(
    private val apiService: KarhebtiApiService,
    private val notificationApiService: NotificationApiService,
    private val token: String
) {
    companion object {
        private const val TAG = "MarketplaceRepository"

        // Singleton WebSocket client - shared across all repository instances
        @Volatile
        private var sharedWebSocketClient: ChatWebSocketClient? = null

        // Singleton Polling service - shared across all repository instances
        @Volatile
        private var sharedPollingService: ChatPollingService? = null

        private val webSocketLock = Any()
    }

    // WebSocket client instance
    private var webSocketClient: ChatWebSocketClient? = null

    // HTTP Polling service (fallback when WebSocket fails)
    private var pollingService: ChatPollingService? = null

    // ==================== CARS ====================

    suspend fun getAvailableCars(): Resource<List<MarketplaceCarResponse>> {
        return try {
            Log.d(TAG, "Fetching available cars...")
            val response = apiService.getAvailableCars()
            Log.d(TAG, "Response code: ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                val cars = response.body()!!
                Log.d(TAG, "✓ Successfully fetched ${cars.size} available cars")
                cars.forEachIndexed { index, car ->
                    Log.d(TAG, "Car $index: ${car.marque} ${car.modele} (${car.annee}) - forSale: ${car.isForSale}, saleStatus: ${car.saleStatus}")
                }
                Resource.Success(cars)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error fetching available cars: Code=${response.code()}, Error=$errorMsg")
                Resource.Error("Failed to fetch cars: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching available cars: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Exception stacktrace:", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun listCarForSale(carId: String, price: Double, description: String?): Resource<MarketplaceCarResponse> {
        return try {
            Log.d(TAG, "Listing car for sale: carId=$carId, price=$price")
            val request = ListCarForSaleRequest(price, description)
            val response = apiService.listCarForSale(carId, request)
            Log.d(TAG, "Response code: ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✓ Successfully listed car for sale")
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error listing car for sale: Code=${response.code()}, Error=$errorMsg")
                Resource.Error("Failed to list car: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception listing car for sale: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "Exception message: ${e.message}")
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun unlistCar(carId: String): Resource<MarketplaceCarResponse> {
        return try {
            Log.d(TAG, "Unlisting car: carId=$carId")
            val response = apiService.unlistCar(carId)
            Log.d(TAG, "Response code: ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✓ Successfully unlisted car")
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error unlisting car: Code=${response.code()}, Error=$errorMsg")
                Resource.Error("Failed to unlist car: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception unlisting car: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "Exception message: ${e.message}")
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun createSwipe(carId: String, direction: String): Resource<SwipeResponse> {
        return try {
            val request = CreateSwipeRequest(carId, direction)
            val response = apiService.createSwipe(request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error creating swipe: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating swipe", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun acceptSwipe(swipeId: String): Resource<SwipeStatusResponse> {
        return try {
            val response = apiService.acceptSwipe(swipeId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error accepting swipe: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception accepting swipe", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun declineSwipe(swipeId: String): Resource<SwipeStatusResponse> {
        return try {
            val response = apiService.declineSwipe(swipeId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error declining swipe: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception declining swipe", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMySwipes(): Resource<MySwipesResponse> {
        return try {
            val response = apiService.getMySwipes()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error fetching my swipes: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching my swipes", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getPendingSwipes(): Resource<List<SwipeResponse>> {
        return try {
            Log.d(TAG, "Fetching pending swipes...")
            val response = apiService.getPendingSwipes()
            Log.d(TAG, "Response code: ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                val swipes = response.body()!!
                Log.d(TAG, "✓ Successfully fetched ${swipes.size} pending swipes")
                swipes.forEachIndexed { index, swipe ->
                    Log.d(TAG, "Swipe $index: buyerId=${swipe.buyerId}, status=${swipe.status}, carId=${swipe.carId}")
                }
                Resource.Success(swipes)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error fetching pending swipes: Code=${response.code()}, Error=$errorMsg")
                Resource.Error("Failed to fetch pending swipes: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching pending swipes: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Exception stacktrace:", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    // ==================== CONVERSATIONS ====================

    suspend fun getConversations(): Resource<List<ConversationResponse>> {
        return try {
            Log.d(TAG, "Fetching conversations...")
            val response = apiService.getConversations()
            Log.d(TAG, "Response code: ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                val conversations = response.body()!!
                Log.d(TAG, "✓ Successfully fetched ${conversations.size} conversations")
                conversations.forEachIndexed { index, conv ->
                    Log.d(TAG, "Conversation $index: id=${conv.id}, lastMessage=${conv.lastMessage?.take(30)}, unreadCount=${conv.unreadCount}")
                }
                Resource.Success(conversations)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error fetching conversations: Code=${response.code()}, Error=$errorMsg")
                Resource.Error("Failed to fetch conversations: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching conversations: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Exception stacktrace:", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getConversation(conversationId: String): Resource<ConversationResponse> {
        return try {
            val response = apiService.getConversation(conversationId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error fetching conversation: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching conversation", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMessages(conversationId: String): Resource<List<ChatMessage>> {
        return try {
            val response = apiService.getMessages(conversationId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error fetching messages: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching messages", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun sendMessage(conversationId: String, content: String): Resource<ChatMessage> {
        return try {
            val request = SendMessageRequest(content)
            val response = apiService.sendMessage(conversationId, request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error sending message: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception sending message", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun markConversationAsRead(conversationId: String): Resource<MessageResponse> {
        return try {
            val response = apiService.markConversationAsRead(conversationId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error marking conversation as read: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception marking conversation as read", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    // ==================== NOTIFICATIONS ====================

    suspend fun getNotifications(): Resource<List<NotificationResponse>> {
        return try {
            val response = apiService.getNotifications()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error fetching notifications: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching notifications", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getUnreadCount(): Resource<UnreadCountResponse> {
        return try {
            val response = notificationApiService.getUnreadCount()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error fetching unread count: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching unread count", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun markNotificationAsRead(notificationId: String): Resource<NotificationResponse> {
        return try {
            val response = apiService.markNotificationAsRead(notificationId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error marking notification as read: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception marking notification as read", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun markAllNotificationsAsRead(): Resource<MessageResponse> {
        return try {
            val response = apiService.markAllNotificationsAsRead()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error marking all notifications as read: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception marking all notifications as read", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    // ==================== WEBSOCKET ====================

    fun initWebSocket(
        onMessageReceived: (ChatMessage) -> Unit,
        onNotificationReceived: (NotificationResponse) -> Unit,
        onUserTyping: (String, String) -> Unit,
        onUserStatus: (String, Boolean) -> Unit,
        onConnectionChanged: (Boolean) -> Unit
    ) {
        // Use the shared WebSocket client instance
        val client = sharedWebSocketClient ?: synchronized(webSocketLock) {
            sharedWebSocketClient ?: ChatWebSocketClient(
                token = token,
                onMessageReceived = onMessageReceived,
                onNotificationReceived = onNotificationReceived,
                onUserTyping = onUserTyping,
                onUserStatus = onUserStatus,
                onConnectionChanged = onConnectionChanged
            ).also { sharedWebSocketClient = it }
        }

        webSocketClient = client
        webSocketClient?.connect()
    }

    fun disconnectWebSocket() {
        webSocketClient?.disconnect()
        webSocketClient = null
    }

    fun joinConversation(conversationId: String) {
        webSocketClient?.joinConversation(conversationId)
    }

    fun leaveConversation(conversationId: String) {
        webSocketClient?.leaveConversation(conversationId)
    }

    fun sendChatMessage(conversationId: String, content: String) {
        webSocketClient?.sendChatMessage(conversationId, content)
    }

    fun sendTypingIndicator(conversationId: String) {
        webSocketClient?.sendTypingIndicator(conversationId)
    }

    fun isWebSocketConnected(): Boolean {
        return webSocketClient?.isConnected() ?: false
    }

    // ==================== POLLING (HTTP Fallback) ====================

    fun initPolling(
        onMessageReceived: (ChatMessage) -> Unit
    ) {
        // Use the shared Polling service instance
        val service = sharedPollingService ?: synchronized(webSocketLock) {
            sharedPollingService ?: ChatPollingService(
                apiService = apiService,
                pollingIntervalMs = 3000L // Poll every 3 seconds
            ).also { sharedPollingService = it }
        }

        pollingService = service
        Log.d(TAG, "🔄 Initializing HTTP Polling service")
    }

    fun startPollingConversation(conversationId: String) {
        pollingService?.startPolling(conversationId)
    }

    fun stopPollingConversation() {
        pollingService?.stopPolling()
    }

    fun getPollingMessages(): StateFlow<ChatMessage?>? {
        return pollingService?.newMessages
    }

    fun cleanupPolling() {
        pollingService?.cleanup()
        pollingService = null
    }
}
