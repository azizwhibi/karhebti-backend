package com.example.karhebti_android.data.repository

import android.content.Context
import android.util.Log
import com.example.karhebti_android.data.api.*
import com.example.karhebti_android.data.models.ExpiringDocument
import com.example.karhebti_android.data.models.ExpiringDocumentsResponse
import com.example.karhebti_android.data.preferences.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository pour gérer les documents expirant
 */
class ExpiringDocumentsRepository(
    private val karhebtiApiService: KarhebtiApiService,
    private val context: Context
) {

    fun getExpiringDocuments(): Flow<Result<ExpiringDocumentsResponse>> = flow {
        try {
            val jwtToken = TokenManager.getInstance(context).getToken()
                ?: throw Exception("JWT token not found")

            // TODO: Add endpoint to KarhebtiApiService if needed
            // For now, return empty list
            Log.d(TAG, "Fetching expiring documents")
            emit(Result.success(ExpiringDocumentsResponse(
                success = true,
                message = "OK",
                data = emptyList(),
                count = 0
            )))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching expiring documents: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    fun refreshExpiringDocuments(): Flow<Result<ExpiringDocumentsResponse>> = flow {
        try {
            Log.d(TAG, "🔄 Refreshing expiring documents")
            emit(Result.success(ExpiringDocumentsResponse(
                success = true,
                message = "OK",
                data = emptyList(),
                count = 0
            )))
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing documents: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    companion object {
        private const val TAG = "ExpiringDocumentsRepository"
    }
}

/**
 * Repository pour gérer les notifications
 */
class NotificationRepository(
    private val notificationApiService: NotificationApiService,
    private val context: Context
) {

    fun getNotifications(): Flow<Result<NotificationsResponse>> = flow {
        try {
            val jwtToken = TokenManager.getInstance(context).getToken()

            if (jwtToken.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ JWT token is empty - user may need to login")
                emit(Result.failure(Exception("JWT token not found - please login")))
                return@flow
            }

            Log.d(TAG, "Fetching notifications (AuthInterceptor will attach token)")

            val response = notificationApiService.getNotifications()

            when {
                response.isSuccessful -> {
                    response.body()?.let { body ->
                        Log.d(
                            TAG,
                            "✅ Notifications chargées: ${body.data.size} items, unread: ${body.metadata?.unreadCount ?: 0}"
                        )
                        emit(Result.success(body))
                    } ?: emit(Result.failure(Exception("Empty response body")))
                }
                response.code() == 401 -> {
                    Log.e(TAG, "❌ 401 Unauthorized - JWT may be expired")
                    TokenManager.getInstance(context).clearAll()
                    emit(Result.failure(Exception("Unauthorized - please login again")))
                }
                response.code() == 403 -> {
                    Log.e(TAG, "❌ 403 Forbidden - insufficient permissions")
                    emit(Result.failure(Exception("Forbidden - insufficient permissions")))
                }
                response.code() == 400 -> {
                    val errorMessage = response.errorBody()?.string() ?: "Validation error"
                    Log.e(TAG, "❌ 400 Bad Request: $errorMessage")
                    emit(Result.failure(Exception("Validation error: $errorMessage")))
                }
                else -> {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "❌ API Error ${response.code()}: $errorMessage")
                    emit(Result.failure(Exception("API Error: ${response.code()}")))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching notifications: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    fun getUnreadCount(): Flow<Result<Int>> = flow {
        try {
            val jwtToken = TokenManager.getInstance(context).getToken()

            if (jwtToken.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ JWT token is empty for unread count")
                emit(Result.failure(Exception("JWT token not found")))
                return@flow
            }

            Log.d(TAG, "Fetching unread count (AuthInterceptor will attach token)")

            val response = notificationApiService.getUnreadCount()

            when {
                response.isSuccessful -> {
                    val count = response.body()?.unreadCount ?: 0
                    Log.d(TAG, "✅ Unread count: $count")
                    emit(Result.success(count))
                }
                response.code() == 401 -> {
                    Log.e(TAG, "❌ 401 - clearing token")
                    TokenManager.getInstance(context).clearAll()
                    emit(Result.failure(Exception("Unauthorized")))
                }
                else -> {
                    Log.e(TAG, "❌ Error ${response.code()}")
                    emit(Result.failure(Exception("API Error: ${response.code()}")))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching unread count: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    fun markAsRead(notificationId: String): Flow<Result<Boolean>> = flow {
        try {
            val jwtToken = TokenManager.getInstance(context).getToken()
                ?: throw Exception("JWT token not found")

            Log.d(TAG, "Marking notification as read (AuthInterceptor will attach token)")

            val response = notificationApiService.markNotificationAsRead(
                notificationId = notificationId
            )

            if (response.isSuccessful) {
                Log.d(TAG, "✅ Notification marked as read: $notificationId")
                emit(Result.success(true))
            } else {
                emit(Result.failure(Exception("API Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    fun markAllAsRead(): Flow<Result<Boolean>> = flow {
        try {
            val jwtToken = TokenManager.getInstance(context).getToken()
                ?: throw Exception("JWT token not found")

            Log.d(TAG, "Marking all notifications as read (AuthInterceptor will attach token)")

            val response = notificationApiService.markAllNotificationsAsRead()

            if (response.isSuccessful) {
                Log.d(TAG, "✅ All notifications marked as read")
                emit(Result.success(true))
            } else {
                emit(Result.failure(Exception("API Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all as read: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    fun deleteNotification(notificationId: String): Flow<Result<Boolean>> = flow {
        try {
            val jwtToken = TokenManager.getInstance(context).getToken()
                ?: throw Exception("JWT token not found")

            Log.d(TAG, "Deleting notification (AuthInterceptor will attach token)")

            val response = notificationApiService.deleteNotification(
                notificationId = notificationId
            )

            if (response.isSuccessful) {
                Log.d(TAG, "✅ Notification deleted: $notificationId")
                emit(Result.success(true))
            } else {
                emit(Result.failure(Exception("API Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting notification: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    fun updateDeviceToken(deviceToken: String): Flow<Result<Boolean>> = flow {
        try {
            val jwtToken = TokenManager.getInstance(context).getToken()
                ?: throw Exception("JWT token not found")

            Log.d(TAG, "Updating device token (AuthInterceptor will attach token)")

            val request = DeviceTokenRequest(deviceToken = deviceToken)

            val response = notificationApiService.updateDeviceToken(
                request = request
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "✅ Device token updated successfully")
                emit(Result.success(true))
            } else {
                emit(Result.failure(Exception("Failed to update device token")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device token: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    companion object {
        private const val TAG = "NotificationRepository"
    }
}
