package com.example.karhebti_android.data.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Helper pour gérer Firebase Cloud Messaging et les tokens FCM
 */
class FCMHelper(private val context: Context) {

    companion object {
        private const val TAG = "FCMHelper"
    }

    /**
     * Initialiser Firebase si nécessaire
     */
    private fun ensureFirebaseInitialized() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.d(TAG, "🔧 Initialisant Firebase...")
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "✅ Firebase initialisé")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de l'initialisation de Firebase: ${e.message}")
        }
    }

    /**
     * Obtenir le token FCM actuel
     */
    fun getFCMToken(callback: (token: String) -> Unit) {
        try {
            ensureFirebaseInitialized()
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Erreur lors de la récupération du token", task.exception)
                    callback("")
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d(TAG, "Token FCM obtenu: $token")
                callback(token)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur getFCMToken: ${e.message}", e)
            callback("")
        }
    }

    /**
     * S'abonner à un topic de notifications
     */
    fun subscribeToTopic(topic: String) {
        try {
            ensureFirebaseInitialized()
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "✅ Abonné au topic: $topic")
                    } else {
                        Log.e(TAG, "❌ Erreur abonnement au topic: $topic")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur subscribeToTopic: ${e.message}", e)
        }
    }

    /**
     * Se désabonner d'un topic
     */
    fun unsubscribeFromTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ Désabonné du topic: $topic")
                } else {
                    Log.e(TAG, "❌ Erreur désabonnement du topic: $topic")
                }
            }
    }

    /**
     * Activer les notifications push
     */
    fun enableNotifications() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        Log.d(TAG, "✅ Notifications push activées")
    }

    /**
     * Désactiver les notifications push
     */
    fun disableNotifications() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = false
        Log.d(TAG, "❌ Notifications push désactivées")
    }
}

