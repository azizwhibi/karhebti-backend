package com.example.karhebti_android.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.karhebti_android.MainActivity
import com.example.karhebti_android.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class KarhebtiMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "KarhebtiMessaging"
        private const val CHANNEL_ID_DOCUMENT = "document_expiration"
        private const val CHANNEL_ID_SOS = "sos_breakdown_requests"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "✅ MESSAGE REÇU!")
        Log.d(TAG, "De: ${remoteMessage.from}")
        Log.d(TAG, "Notification: ${remoteMessage.notification}")
        Log.d(TAG, "Data: ${remoteMessage.data}")

        // Extraire le type de notification depuis les données
        val notificationType = remoteMessage.data["type"] ?: "GENERAL"

        // Afficher la notification peu importe la source
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: remoteMessage.data["titre"]
            ?: "Karhebti"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "Notification reçue"

        Log.d(TAG, "Type: $notificationType")
        Log.d(TAG, "Affichage: $title - $body")

        // Choisir le bon channel selon le type
        val channelId = if (notificationType == "BREAKDOWN_REQUEST") {
            CHANNEL_ID_SOS
        } else {
            CHANNEL_ID_DOCUMENT
        }

        showNotification(title, body, remoteMessage.data, channelId, notificationType)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "✅ Nouveau Token FCM: $token")
        // Envoyer automatiquement le token au backend
        val fcmTokenService = FCMTokenService(applicationContext)
        fcmTokenService.registerDeviceToken()
    }

    private fun showNotification(
        title: String,
        message: String,
        data: Map<String, String>,
        channelId: String = CHANNEL_ID_DOCUMENT,
        notificationType: String = "GENERAL"
    ) {
        try {
            Log.d(TAG, "🔔 Création de la notification...")

            // Intent pour ouvrir l'app
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("from_notification", true)
            intent.putExtra("notification_type", notificationType)

            // Ajouter les données de la notification pour accès direct
            data.forEach { (key, value) ->
                intent.putExtra(key, value)
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(), // ID unique pour chaque notification
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Créer les channels (Android 8+)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Channel pour les documents
                val documentChannel = NotificationChannel(
                    CHANNEL_ID_DOCUMENT,
                    "Document Expiration Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                )
                documentChannel.description = "Alerts for documents expiring soon"
                notificationManager.createNotificationChannel(documentChannel)

                // Channel pour les demandes SOS (haute priorité)
                val sosChannel = NotificationChannel(
                    CHANNEL_ID_SOS,
                    "SOS Breakdown Requests",
                    NotificationManager.IMPORTANCE_HIGH
                )
                sosChannel.description = "Urgent breakdown assistance requests"
                sosChannel.enableVibration(true)
                sosChannel.vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                notificationManager.createNotificationChannel(sosChannel)

                Log.d(TAG, "✅ Channels créés")
            }

            // Créer la notification avec priorité selon le type
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))

            // Ajouter vibration et son pour les demandes SOS
            if (notificationType == "BREAKDOWN_REQUEST") {
                notificationBuilder
                    .setVibrate(longArrayOf(0, 500, 250, 500, 250, 500))
                    .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            } else {
                notificationBuilder.setVibrate(longArrayOf(0, 500, 250, 500))
            }

            // Afficher la notification avec un ID unique
            val notificationId = if (notificationType == "BREAKDOWN_REQUEST") {
                // Utiliser l'ID de breakdown si disponible, sinon timestamp
                data["breakdownId"]?.hashCode() ?: System.currentTimeMillis().toInt()
            } else {
                1
            }

            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d(TAG, "✅✅✅ NOTIFICATION AFFICHÉE: $title (Type: $notificationType, ID: $notificationId)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur: ${e.message}", e)
        }
    }
}

