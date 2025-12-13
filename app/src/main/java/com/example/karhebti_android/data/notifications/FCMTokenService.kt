package com.example.karhebti_android.data.notifications

import android.content.Context
import android.util.Log
import com.example.karhebti_android.data.api.DeviceTokenRequest
import com.example.karhebti_android.data.api.RetrofitClient
import com.example.karhebti_android.data.preferences.TokenManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Service pour gérer l'enregistrement et l'envoi du token FCM au backend
 */
class FCMTokenService(private val context: Context) {

    companion object {
        private const val TAG = "FCMTokenService"
        private const val PREF_NAME = "fcm_token_prefs"
        private const val KEY_LAST_TOKEN = "last_sent_token"
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val notificationApiService = RetrofitClient.notificationApiService

    /**
     * Obtenir et envoyer le token FCM au backend
     */
    fun registerDeviceToken() {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Vérifier et initialiser Firebase si nécessaire
                    if (FirebaseApp.getApps(context).isEmpty()) {
                        try {
                            Log.d(TAG, "🔧 Initialisant Firebase...")
                            FirebaseApp.initializeApp(context)
                            Log.d(TAG, "✅ Firebase initialisé")
                        } catch (e: Exception) {
                            Log.w(TAG, "Firebase déjà initialisé ou erreur: ${e.message}")
                        }
                    }

                    // Obtenir le token FCM
                    try {
                        val token = Tasks.await(FirebaseMessaging.getInstance().token, 30, TimeUnit.SECONDS)
                        Log.d(TAG, "✅ Token FCM obtenu: $token")

                        // Vérifier si le token a changé
                        val lastSentToken = prefs.getString(KEY_LAST_TOKEN, null)
                        if (token == lastSentToken) {
                            Log.d(TAG, "ℹ️ Token identique, pas besoin de renvoyer")
                            return@launch
                        }

                        // Envoyer au backend
                        sendTokenToBackend(token)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Erreur lors de l'obtention du token FCM: ${e.message}", e)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erreur critique dans registerDeviceToken: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors du lancement du coroutine scope: ${e.message}", e)
        }
    }

    /**
     * Envoyer le token au backend
     */
    private suspend fun sendTokenToBackend(deviceToken: String) {
        try {
            val jwtToken = TokenManager.getInstance(context).getToken()
            if (jwtToken.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ Pas de JWT token, impossible d'envoyer le device token")
                return
            }

            try {
                val request = DeviceTokenRequest(deviceToken = deviceToken)
                // L'AuthInterceptor ajoute automatiquement le header Authorization avec le JWT
                val response = notificationApiService.updateDeviceToken(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "✅ Token envoyé au backend avec succès")
                    // Sauvegarder le token envoyé
                    prefs.edit().putString(KEY_LAST_TOKEN, deviceToken).apply()
                } else {
                    Log.e(TAG, "❌ Erreur backend: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de l'appel API: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur dans sendTokenToBackend: ${e.message}", e)
        }
    }

    /**
     * S'abonner aux topics de notifications
     */
    fun subscribeToTopics() {
        val fcmHelper = FCMHelper(context)
        fcmHelper.subscribeToTopic("document_expiration")
        fcmHelper.subscribeToTopic("all_users")
        Log.d(TAG, "✅ Abonné aux topics de notifications")
    }

    /**
     * Forcer le rafraîchissement du token
     */
    fun forceRefreshToken() {
        prefs.edit().remove(KEY_LAST_TOKEN).apply()
        registerDeviceToken()
    }
}
