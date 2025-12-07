package com.example.karhebti_android.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.example.karhebti_android.data.api.RetrofitClient
import com.google.gson.Gson
import org.json.JSONObject

class TokenManager(context: Context) {
    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "karhebti_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "user_data"

        @Volatile
        private var instance: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveToken(token: String) {
        android.util.Log.d("TokenManager", "Saving token: $token")
        prefs.edit().putString(KEY_TOKEN, token).apply()
        android.util.Log.d("TokenManager", "Token saved. Verifying: ${getToken()}")
        RetrofitClient.setAuthToken(token)
    }

    fun getToken(): String? {
        val token = prefs.getString(KEY_TOKEN, null)
        android.util.Log.d("TokenManager", "Getting token: ${if (token != null) "Found (length: ${token.length})" else "NULL"}")
        return token
    }

    fun saveUser(user: UserData) {
        val userJson = gson.toJson(user)
        prefs.edit().putString(KEY_USER, userJson).apply()
    }

    fun getUser(): UserData? {
        val userJson = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(userJson, UserData::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        RetrofitClient.setAuthToken(null)
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null && getUser() != null
    }

    fun isAdmin(): Boolean {
        return getUser()?.role == "admin"
    }

    // Initialize token on app start
    fun initializeToken() {
        val token = getToken()
        if (token != null) {
            RetrofitClient.setAuthToken(token)
        }
    }

    // Extract user ID from JWT token
    fun getUserIdFromToken(token: String): String? {
        return try {
            val splitToken = token.split(".")
            if (splitToken.size > 1) {
                val payload = String(Base64.decode(splitToken[1], Base64.DEFAULT))
                val jsonObject = JSONObject(payload)
                jsonObject.getString("sub")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Get current user ID
    fun getUserId(): String? {
        return getUser()?.id ?: (getToken()?.let { getUserIdFromToken(it) })
    }
}

data class UserData(
    val id: String?, // Changed to nullable to match backend response
    val email: String,
    val nom: String,
    val prenom: String,
    val role: String,
    val telephone: String? = null
)
