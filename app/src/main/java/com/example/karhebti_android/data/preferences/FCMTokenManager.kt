package com.example.karhebti_android.data.preferences

import android.content.Context
import android.util.Log
import com.example.karhebti_android.data.notifications.FCMHelper
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Extension pour TokenManager afin de gérer le token FCM
 */
class FCMTokenManager(private val context: Context) {

    companion object {
        private const val TAG = "FCMTokenManager"
        private const val FCM_TOKEN_KEY = "fcm_token"
    }

    private val sharedPref = context.getSharedPreferences("karhebti_fcm", Context.MODE_PRIVATE)

    /**
     * Initialiser et obtenir le token FCM
     */
    fun initializeFCMToken(callback: (token: String) -> Unit) {
        // Vérifier si on a déjà un token
        val savedToken = sharedPref.getString(FCM_TOKEN_KEY, "")
        if (!savedToken.isNullOrEmpty()) {
            Log.d(TAG, "✅ Token FCM trouvé en cache: ${savedToken.take(20)}...")
            callback(savedToken)
            return
        }

        // Récupérer le nouveau token
        FCMHelper(context).getFCMToken { token ->
            if (token.isNotEmpty()) {
                // Sauvegarder le token
                sharedPref.edit().putString(FCM_TOKEN_KEY, token).apply()
                Log.d(TAG, "✅ Nouveau token FCM sauvegardé: ${token.take(20)}...")
                callback(token)
            } else {
                Log.e(TAG, "❌ Erreur: Impossible de récupérer le token FCM")
                callback("")
            }
        }
    }

    /**
     * Obtenir le token FCM sauvegardé
     */
    fun getFCMToken(): String? {
        return sharedPref.getString(FCM_TOKEN_KEY, "")
    }

    /**
     * Supprimer le token FCM (au logout)
     */
    fun clearFCMToken() {
        sharedPref.edit().remove(FCM_TOKEN_KEY).apply()
        Log.d(TAG, "✅ Token FCM supprimé")
    }
}

