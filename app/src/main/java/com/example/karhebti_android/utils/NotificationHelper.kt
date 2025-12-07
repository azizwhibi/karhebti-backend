package com.example.karhebti_android.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.karhebti_android.MainActivity
import com.example.karhebti_android.R
import com.example.karhebti_android.data.api.NotificationResponse

/**
 * Helper class for displaying Android notifications
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID_MESSAGES = "chat_messages"
        private const val CHANNEL_ID_GENERAL = "general_notifications"
        private const val CHANNEL_NAME_MESSAGES = "Chat Messages"
        private const val CHANNEL_NAME_GENERAL = "General Notifications"
        private var notificationId = 1000
    }

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels for Android 8.0+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for chat messages
            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                CHANNEL_NAME_MESSAGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                enableVibration(true)
                enableLights(true)
            }

            // Channel for general notifications
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                CHANNEL_NAME_GENERAL,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
                enableVibration(true)
            }

            // Register channels with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(generalChannel)

            android.util.Log.d("NotificationHelper", "✅ Notification channels created")
        }
    }

    /**
     * Show a notification from a NotificationResponse object
     */
    fun showNotification(notification: NotificationResponse) {
        android.util.Log.d("NotificationHelper", "📲 Showing notification: ${notification.type}")
        android.util.Log.d("NotificationHelper", "   Title: ${notification.titre}")
        android.util.Log.d("NotificationHelper", "   Message: ${notification.message}")

        // Choose channel based on notification type
        val channelId = when (notification.type) {
            "new_message" -> CHANNEL_ID_MESSAGES
            else -> CHANNEL_ID_GENERAL
        }

        // Create an intent to open the app when notification is clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // Add conversation ID if it's a message notification
            if (notification.type == "new_message") {
                notification.data?.get("conversationId")?.let { conversationId ->
                    putExtra("conversationId", conversationId.toString())
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // You may want to create a proper icon
            .setContentTitle(notification.titre)
            .setContentText(notification.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Add extra styling for message notifications
        if (notification.type == "new_message") {
            notificationBuilder
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVibrate(longArrayOf(0, 250, 250, 250))
        }

        // Show the notification
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId++, notificationBuilder.build())
                android.util.Log.d("NotificationHelper", "✅ Notification displayed successfully")
            }
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "❌ Permission denied for notifications: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "❌ Error showing notification: ${e.message}", e)
        }
    }

    /**
     * Show a simple notification with title and message
     */
    fun showSimpleNotification(title: String, message: String, channelId: String = CHANNEL_ID_GENERAL) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId++, notificationBuilder.build())
            }
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "❌ Permission denied: ${e.message}")
        }
    }

    /**
     * Cancel all notifications
     */
    fun cancelAll() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
