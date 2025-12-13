package com.example.karhebti_android.data.polling

import android.util.Log
import com.example.karhebti_android.data.api.ChatMessage
import com.example.karhebti_android.data.api.KarhebtiApiService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * HTTP Polling service for real-time chat when WebSocket is not available
 * Polls the server every few seconds to check for new messages
 */
class ChatPollingService(
    private val apiService: KarhebtiApiService,
    private val pollingIntervalMs: Long = 3000L // Poll every 3 seconds
) {
    companion object {
        private const val TAG = "ChatPollingService"
    }

    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _newMessages = MutableStateFlow<ChatMessage?>(null)
    val newMessages: StateFlow<ChatMessage?> = _newMessages

    private var lastMessageId: String? = null
    private var currentConversationId: String? = null
    private var isPolling = false

    /**
     * Start polling for new messages in a conversation
     */
    fun startPolling(conversationId: String) {
        if (isPolling && currentConversationId == conversationId) {
            Log.d(TAG, "Already polling conversation: $conversationId")
            return
        }

        stopPolling()

        currentConversationId = conversationId
        isPolling = true
        lastMessageId = null

        Log.d(TAG, "🔄 Starting polling for conversation: $conversationId")

        pollingJob = scope.launch {
            while (isActive && isPolling) {
                try {
                    pollForNewMessages(conversationId)
                    delay(pollingIntervalMs)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Polling error: ${e.message}", e)
                    delay(pollingIntervalMs * 2) // Back off on error
                }
            }
        }
    }

    /**
     * Stop polling
     */
    fun stopPolling() {
        if (!isPolling) return

        Log.d(TAG, "⏹️ Stopping polling for conversation: $currentConversationId")
        isPolling = false
        pollingJob?.cancel()
        pollingJob = null
        currentConversationId = null
        lastMessageId = null
    }

    /**
     * Poll the server for new messages
     */
    private suspend fun pollForNewMessages(conversationId: String) {
        try {
            val response = apiService.getMessages(conversationId)

            if (response.isSuccessful) {
                val messages = response.body() ?: emptyList()

                if (messages.isNotEmpty()) {
                    val latestMessage = messages.last()

                    // Check if this is a new message
                    if (lastMessageId == null) {
                        // First poll, just store the ID
                        lastMessageId = latestMessage.id
                        Log.d(TAG, "📝 Initial poll - Latest message ID: $lastMessageId")
                    } else if (latestMessage.id != lastMessageId) {
                        // New message detected!
                        Log.d(TAG, "✨ New message detected: ${latestMessage.id}")
                        Log.d(TAG, "📨 Content: ${latestMessage.content}")

                        lastMessageId = latestMessage.id
                        _newMessages.value = latestMessage

                        // Reset after emitting
                        delay(100)
                        _newMessages.value = null
                    }
                }
            } else {
                Log.w(TAG, "⚠️ Polling failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error polling messages: ${e.message}")
            throw e
        }
    }

    /**
     * Update the polling interval
     */
    fun setPollingInterval(intervalMs: Long) {
        if (isPolling) {
            val currentConv = currentConversationId
            stopPolling()
            if (currentConv != null) {
                startPolling(currentConv)
            }
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopPolling()
        scope.cancel()
    }
}

