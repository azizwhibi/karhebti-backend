package com.example.karhebti_android.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.karhebti_android.data.api.NotificationItemResponse
import com.example.karhebti_android.data.models.ExpiringDocument
import com.example.karhebti_android.data.repository.ExpiringDocumentsRepository
import com.example.karhebti_android.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI State for Expiring Documents
data class ExpiringDocumentsUiState(
    val isLoading: Boolean = true,
    val documents: List<ExpiringDocument> = emptyList(),
    val error: String? = null,
    val isEmpty: Boolean = false,
    val lastRefresh: Long = 0,
    val criticalCount: Int = 0,
    val warningCount: Int = 0
)

/**
 * ViewModel pour les documents expirant
 */
class ExpiringDocumentsViewModel(
    application: Application,
    private val repository: ExpiringDocumentsRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ExpiringDocumentsUiState>(
        ExpiringDocumentsUiState()
    )
    val uiState: StateFlow<ExpiringDocumentsUiState> = _uiState.asStateFlow()

    init {
        loadExpiringDocuments()
    }

    fun loadExpiringDocuments() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getExpiringDocuments().collect { result ->
                result.onSuccess { response ->
                    val criticalCount = response.data.count { it.daysUntilExpiration <= 3 }
                    val warningCount = response.data.count { it.daysUntilExpiration in 4..7 }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        documents = response.data,
                        isEmpty = response.data.isEmpty(),
                        lastRefresh = System.currentTimeMillis(),
                        criticalCount = criticalCount,
                        warningCount = warningCount
                    )
                    Log.d(TAG, "✅ Loaded ${response.data.size} expiring documents")
                }
                result.onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Unknown error",
                        isEmpty = _uiState.value.documents.isEmpty()
                    )
                    Log.e(TAG, "❌ Error loading documents: ${exception.message}")
                }
            }
        }
    }

    fun refreshDocuments() {
        viewModelScope.launch {
            repository.refreshExpiringDocuments().collect { result ->
                result.onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        documents = response.data,
                        isEmpty = response.data.isEmpty(),
                        lastRefresh = System.currentTimeMillis()
                    )
                    Log.d(TAG, "🔄 Documents refreshed")
                }
                result.onFailure { exception ->
                    Log.e(TAG, "Error refreshing documents: ${exception.message}")
                }
            }
        }
    }

    fun sortByUrgency(): List<ExpiringDocument> {
        return _uiState.value.documents.sortedBy { it.daysUntilExpiration }
    }

    fun filterByCritical(): List<ExpiringDocument> {
        return _uiState.value.documents.filter { it.daysUntilExpiration <= 3 }
    }

    fun filterByWarning(): List<ExpiringDocument> {
        return _uiState.value.documents.filter { it.daysUntilExpiration in 4..7 }
    }

    companion object {
        private const val TAG = "ExpiringDocumentsVM"
    }
}

// UI State for Notifications
data class NotificationUiState(
    val isLoading: Boolean = true,
    val notifications: List<NotificationItemResponse> = emptyList(),
    val unreadCount: Int = 0,
    val error: String? = null,
    val isEmpty: Boolean = false,
    val lastRefresh: Long = 0
)

/**
 * ViewModel pour les notifications
 */
class NotificationViewModel(
    application: Application,
    private val repository: NotificationRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<NotificationUiState>(
        NotificationUiState()
    )
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        try {
            loadNotifications()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing loadNotifications: ${e.message}", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Erreur lors du chargement des notifications",
                isEmpty = true
            )
        }

        try {
            loadUnreadCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing loadUnreadCount: ${e.message}", e)
        }
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                repository.getNotifications().collect { result ->
                    result.onSuccess { response ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            notifications = response.data,
                            isEmpty = response.data.isEmpty(),
                            lastRefresh = System.currentTimeMillis()
                        )
                        Log.d(TAG, "✅ Notifications loaded: ${response.data.size} items")
                    }
                    result.onFailure { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Erreur inconnue",
                            isEmpty = _uiState.value.notifications.isEmpty()
                        )
                        Log.e(TAG, "❌ Error loading notifications: ${exception.message}", exception)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadNotifications: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erreur inconnue",
                    isEmpty = true
                )
            }
        }
    }

    private fun loadUnreadCount() {
        viewModelScope.launch {
            try {
                repository.getUnreadCount().collect { result ->
                    result.onSuccess { count ->
                        _uiState.value = _uiState.value.copy(unreadCount = count)
                        Log.d(TAG, "Unread count: $count")
                    }
                    result.onFailure { exception ->
                        Log.e(TAG, "Error loading unread count: ${exception.message}", exception)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadUnreadCount: ${e.message}", e)
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId).collect { result ->
                result.onSuccess {
                    loadNotifications()
                    loadUnreadCount()
                    Log.d(TAG, "✅ Notification marked as read")
                }
                result.onFailure { exception ->
                    Log.e(TAG, "Error marking as read: ${exception.message}")
                }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead().collect { result ->
                result.onSuccess {
                    loadNotifications()
                    loadUnreadCount()
                    Log.d(TAG, "✅ All notifications marked as read")
                }
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId).collect { result ->
                result.onSuccess {
                    loadNotifications()
                    loadUnreadCount()
                    Log.d(TAG, "✅ Notification deleted")
                }
            }
        }
    }

    fun updateDeviceToken(deviceToken: String) {
        viewModelScope.launch {
            repository.updateDeviceToken(deviceToken).collect { result ->
                result.onSuccess {
                    Log.d(TAG, "✅ Device token updated")
                }
                result.onFailure { exception ->
                    Log.e(TAG, "Error updating device token: ${exception.message}")
                }
            }
        }
    }

    fun refreshNotifications() {
        loadNotifications()
        loadUnreadCount()
    }

    companion object {
        private const val TAG = "NotificationVM"
    }
}

