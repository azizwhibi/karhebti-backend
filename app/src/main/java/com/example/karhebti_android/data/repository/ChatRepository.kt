package com.example.karhebti_android.data.repository

import android.util.Log
import com.example.karhebti_android.data.api.*
import com.example.karhebti_android.data.websocket.ChatWebSocketClient

/**
 * Singleton ChatRepository - manages all chat-related network calls and WebSocket
 * Separated from MarketplaceRepository for better architecture
 */
class ChatRepository private constructor(
    private val apiService: KarhebtiApiService,
    private val token: String
) {
    companion object {
        private const val TAG = "ChatRepository"

        @Volatile
        private var INSTANCE: ChatRepository? = null

        // Singleton WebSocket client shared across the app
        @Volatile
        private var sharedWebSocketClient: ChatWebSocketClient? = null

        private val webSocketLock = Any()

        fun getInstance(apiService: KarhebtiApiService, token: String): ChatRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatRepository(apiService, token).also { INSTANCE = it }
            }
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
                    Log.d(TAG, "Conversation $index: id=${conv.id}")
                    Log.d(TAG, "  - Other user: ${conv.otherUser?.nom} ${conv.otherUser?.prenom}")
                    Log.d(TAG, "  - Car: ${conv.carDetails?.marque} ${conv.carDetails?.modele}")
                    Log.d(TAG, "  - Last message: ${conv.lastMessage?.take(30)}")
                }
                Resource.Success(conversations)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Error fetching conversations: Code=${response.code()}, Error=$errorMsg")
                Resource.Error("Failed to fetch conversations: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching conversations: ${e.javaClass.simpleName}", e)
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getConversation(conversationId: String): Resource<ConversationResponse> {
        return try {
            Log.d(TAG, "Fetching conversation: $conversationId")
            val response = apiService.getConversation(conversationId)
            if (response.isSuccessful && response.body() != null) {
                val conv = response.body()!!
                Log.d(TAG, "✓ Successfully fetched conversation")
                Log.d(TAG, "  - Other user: ${conv.otherUser?.nom} ${conv.otherUser?.prenom}")
                Log.d(TAG, "  - Car: ${conv.carDetails?.marque} ${conv.carDetails?.modele}")
                Resource.Success(conv)
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
            Log.d(TAG, "Fetching messages for conversation: $conversationId")
            val response = apiService.getMessages(conversationId)
            if (response.isSuccessful && response.body() != null) {
                val messages = response.body()!!
                Log.d(TAG, "✓ Successfully fetched ${messages.size} messages")
                Resource.Success(messages)
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
                Log.d(TAG, "✓ Message sent successfully")
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

    // ==================== WEBSOCKET ====================

    fun initWebSocket(
        onMessageReceived: (ChatMessage) -> Unit,
        onNotificationReceived: (NotificationResponse) -> Unit,
        onUserTyping: (String, String) -> Unit,
        onConnectionChanged: (Boolean) -> Unit
    ) {
        synchronized(webSocketLock) {
            // CRITICAL FIX: Check if WebSocket exists AND is connected
            if (sharedWebSocketClient != null && sharedWebSocketClient?.isConnected() == true) {
                Log.d(TAG, "✅ WebSocket already connected, reusing existing client")
                return
            }

            // If WebSocket exists but not connected, clean it up first
            if (sharedWebSocketClient != null && sharedWebSocketClient?.isConnected() == false) {
                Log.d(TAG, "⚠️ WebSocket exists but disconnected, cleaning up...")
                sharedWebSocketClient?.disconnect()
                sharedWebSocketClient = null
            }

            Log.d(TAG, "🔄 Creating new WebSocket client")
            sharedWebSocketClient = ChatWebSocketClient(
                token = token,
                onMessageReceived = onMessageReceived,
                onNotificationReceived = onNotificationReceived,
                onUserTyping = onUserTyping,
                onUserStatus = { _, _ -> }, // Not needed
                onConnectionChanged = onConnectionChanged
            )
            sharedWebSocketClient?.connect()
            Log.d(TAG, "✅ WebSocket connection initiated")
        }
    }

    fun disconnectWebSocket() {
        synchronized(webSocketLock) {
            Log.d(TAG, "Disconnecting WebSocket...")
            sharedWebSocketClient?.disconnect()
            sharedWebSocketClient = null
            Log.d(TAG, "WebSocket disconnected and cleared")
        }
    }

    fun joinConversation(conversationId: String) {
        val client = sharedWebSocketClient
        if (client?.isConnected() == true) {
            client.joinConversation(conversationId)
            Log.d(TAG, "✅ Joined conversation: $conversationId")
        } else {
            Log.w(TAG, "⚠️ Cannot join conversation - WebSocket not connected")
        }
    }

    fun leaveConversation(conversationId: String) {
        val client = sharedWebSocketClient
        if (client?.isConnected() == true) {
            client.leaveConversation(conversationId)
            Log.d(TAG, "✅ Left conversation: $conversationId")
        } else {
            Log.w(TAG, "⚠️ Cannot leave conversation - WebSocket not connected")
        }
    }

    fun sendTypingIndicator(conversationId: String) {
        val client = sharedWebSocketClient
        if (client?.isConnected() == true) {
            client.sendTypingIndicator(conversationId)
        } else {
            Log.w(TAG, "⚠️ Cannot send typing indicator - WebSocket not connected")
        }
    }
}
