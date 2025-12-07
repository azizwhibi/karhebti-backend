package com.example.karhebti_android.data.api

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.karhebti_android.data.preferences.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * AuthInterceptor: Centralizes JWT management
 *
 * Responsibilities:
 * 1. Read JWT from secure storage (EncryptedSharedPreferences)
 * 2. Attach Authorization: Bearer <token> to every request
 * 3. Handle 401 responses by clearing token and signaling login refresh
 * 4. Log all auth-related events
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    companion object {
        private const val TAG = "AuthInterceptor"
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Step 1: Get JWT from secure storage
        val token = getSecureToken()
        Log.d(TAG, "Token available: ${token != null}")

        // Step 2: Add Authorization header if token exists
        val requestWithAuth = if (token != null) {
            Log.d(TAG, "Adding Authorization header to: ${originalRequest.url}")
            originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
                .build()
        } else {
            Log.w(TAG, "No token available for request: ${originalRequest.url}")
            originalRequest
        }

        // Step 3: Make the request
        val response = try {
            chain.proceed(requestWithAuth)
        } catch (e: Exception) {
            Log.e(TAG, "Network error: ${e.message}", e)
            throw e
        }

        // Step 4: Handle 401 Unauthorized
        if (response.code == 401) {
            Log.w(TAG, "Got 401 Unauthorized - clearing token and marking for logout")
            clearSecureToken()
            // Mark that we need to logout
            setLoginRequired(true)
        }

        // Step 5: Log response
        Log.d(TAG, "Response code: ${response.code} for ${originalRequest.url}")

        return response
    }

    private fun getSecureToken(): String? {
        return try {
            // Try to get from EncryptedSharedPreferences first
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                "secret_shared_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val token = encryptedPrefs.getString("jwt_token", null)

            // Fallback to TokenManager if not in encrypted prefs
            return token ?: TokenManager.getInstance(context).getToken()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading secure token: ${e.message}", e)
            // Fallback to TokenManager
            TokenManager.getInstance(context).getToken()
        }
    }

    private fun clearSecureToken() {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                "secret_shared_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            encryptedPrefs.edit().remove("jwt_token").apply()
            TokenManager.getInstance(context).clearAll()
            Log.d(TAG, "Token cleared from secure storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing token: ${e.message}", e)
            TokenManager.getInstance(context).clearAll()
        }
    }

    private fun setLoginRequired(required: Boolean) {
        try {
            val prefs = context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("login_required", required).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting login required flag: ${e.message}", e)
        }
    }
}

