package com.example.karhebti_android.data.repository

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.karhebti_android.data.api.*
import com.example.karhebti_android.data.preferences.TokenManager
import com.example.karhebti_android.data.preferences.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * AuthRepository: Centralizes authentication logic
 */
class AuthRepository(
    private val authApiService: AuthApiService,
    private val context: Context
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    // Get the main API service for signup and password reset endpoints
    private val karhebtiApiService: KarhebtiApiService by lazy {
        RetrofitClient.apiService
    }

    fun login(email: String, motDePasse: String): Flow<Result<AuthResponse>> = flow {
        try {
            Log.d(TAG, "Attempting login for: $email")

            val request = LoginRequest(email = email, motDePasse = motDePasse)
            val response = authApiService.login(request)

            when {
                response.isSuccessful -> {
                    response.body()?.let { body ->
                        // body est de type AuthResponse (voir DTOs.kt)
                        if (body.accessToken.isNotEmpty()) {
                            Log.d(TAG, "✅ Login successful")

                            // Save token to secure storage
                            saveTokenSecurely(body.accessToken)

                            // Save user info
                            body.user.let { user ->
                                TokenManager.getInstance(context).saveUser(
                                    UserData(
                                        id = user.id?.toString(),
                                        email = user.email,
                                        nom = user.nom,
                                        prenom = user.prenom,
                                        role = user.role,
                                        telephone = user.telephone
                                    )
                                )
                                Log.d(TAG, "✅ User saved: ${user.email}")
                                cacheUserId(user.id?.toString() ?: "")
                            }

                            emit(Result.success(body))
                        } else {
                            Log.e(TAG, "❌ Login response missing token")
                            emit(Result.failure(Exception("Login failed: empty token")))
                        }
                    } ?: emit(Result.failure(Exception("Empty response body")))
                }
                response.code() == 400 -> {
                    val message = "Validation error"
                    Log.e(TAG, "❌ 400 Bad Request: $message")
                    emit(Result.failure(Exception(message)))
                }
                response.code() == 401 -> {
                    val message = "Invalid email or password"
                    Log.e(TAG, "❌ 401 Unauthorized: $message")
                    emit(Result.failure(Exception(message)))
                }
                else -> {
                    val message = "Login failed with code ${response.code()}"
                    Log.e(TAG, "❌ API Error: $message")
                    emit(Result.failure(Exception(message)))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception during login: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    fun logout(): Flow<Result<Unit>> = flow {
        try {
            Log.d(TAG, "Logging out...")
            clearTokenSecurely()
            TokenManager.getInstance(context).clearAll()
            clearUserId()
            Log.d(TAG, "✅ Logout successful")
            emit(Result.success(Unit))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during logout: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    suspend fun signup(nom: String, prenom: String, email: String, password: String, telephone: String): Resource<AuthResponse> {
        return try {
            Log.d(TAG, "Starting signup for: $email")
            val request = SignupRequest(nom, prenom, email, password, telephone)
            val response = karhebtiApiService.signup(request)

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ Signup initiated successfully")
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = "Signup failed: ${response.code()}"
                Log.e(TAG, "❌ $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Signup error: ${e.message}", e)
            Resource.Error("Erreur d'inscription: ${e.localizedMessage}")
        }
    }

    suspend fun verifySignupOtp(email: String, otpCode: String): Resource<AuthResponse> {
        return try {
            Log.d(TAG, "Verifying signup OTP for: $email")
            val request = VerifySignupOtpRequest(email, otpCode)
            val response = karhebtiApiService.verifySignupOtp(request)

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ Signup OTP verified successfully")
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = "OTP verification failed: ${response.code()}"
                Log.e(TAG, "❌ $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Verify signup OTP error: ${e.message}", e)
            Resource.Error("Erreur de vérification: ${e.localizedMessage}")
        }
    }

    suspend fun forgotPassword(email: String): Resource<MessageResponse> {
        return try {
            Log.d(TAG, "Initiating forgot password for: $email")
            val request = ForgotPasswordRequest(email)
            val response = karhebtiApiService.forgotPassword(request)

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ Forgot password request sent successfully")
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = when (response.code()) {
                    404 -> "Email not found"
                    else -> "Request failed: ${response.code()}"
                }
                Log.e(TAG, "❌ $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Forgot password error: ${e.message}", e)
            Resource.Error("Erreur: ${e.localizedMessage}")
        }
    }

    suspend fun verifyOtp(email: String, otp: String): Resource<MessageResponse> {
        return try {
            Log.d(TAG, "Verifying OTP for: $email")
            val request = VerifyOtpRequest(email, otp)
            val response = karhebtiApiService.verifyOtp(request)

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ OTP verified successfully")
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = "OTP verification failed: ${response.code()}"
                Log.e(TAG, "❌ $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Verify OTP error: ${e.message}", e)
            Resource.Error("Erreur de vérification: ${e.localizedMessage}")
        }
    }

    suspend fun resetPassword(email: String, otp: String, newPassword: String): Resource<MessageResponse> {
        return try {
            Log.d(TAG, "Resetting password for: $email")
            val request = ResetPasswordRequest(email, otp, newPassword)
            val response = karhebtiApiService.resetPassword(request)

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ Password reset successfully")
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = "Password reset failed: ${response.code()}"
                Log.e(TAG, "❌ $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Reset password error: ${e.message}", e)
            Resource.Error("Erreur de réinitialisation: ${e.localizedMessage}")
        }
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Resource<MessageResponse> {
        return try {
            Log.d(TAG, "Changing password")
            val request = ChangePasswordRequest(currentPassword, newPassword)
            val response = karhebtiApiService.changePassword(request)

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ Password changed successfully")
                Resource.Success(response.body()!!)
            } else {
                val errorMsg = when (response.code()) {
                    401 -> "Current password is incorrect"
                    else -> "Password change failed: ${response.code()}"
                }
                Log.e(TAG, "❌ $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Change password error: ${e.message}", e)
            Resource.Error("Erreur de changement de mot de passe: ${e.localizedMessage}")
        }
    }

    fun isLoggedIn(): Boolean {
        return getTokenSecurely() != null && TokenManager.getInstance(context).getUser() != null
    }

    fun getCachedUserId(): String? {
        return try {
            val prefs = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
            prefs.getString("cached_user_id", null)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached user ID: ${e.message}")
            null
        }
    }

    // ===== PRIVATE HELPERS =====

    private fun saveTokenSecurely(token: String) {
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

            encryptedPrefs.edit().putString("jwt_token", token).apply()
            TokenManager.getInstance(context).saveToken(token)
            Log.d(TAG, "✅ Token saved securely")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving token: ${e.message}", e)
            TokenManager.getInstance(context).saveToken(token)
        }
    }

    private fun getTokenSecurely(): String? {
        return try {
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

            encryptedPrefs.getString("jwt_token", null)
                ?: TokenManager.getInstance(context).getToken()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting token: ${e.message}")
            TokenManager.getInstance(context).getToken()
        }
    }

    private fun clearTokenSecurely() {
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
            Log.d(TAG, "✅ Token cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing token: ${e.message}")
        }
    }

    private fun cacheUserId(userId: String) {
        try {
            if (userId.isNotBlank()) {
                val prefs = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
                prefs.edit().putString("cached_user_id", userId).apply()
                Log.d(TAG, "✅ User ID cached: $userId")
            } else {
                Log.w(TAG, "cacheUserId called with blank id - skipping cache")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error caching user ID: ${e.message}")
        }
    }

    private fun clearUserId() {
        try {
            val prefs = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
            prefs.edit().remove("cached_user_id").apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing user ID: ${e.message}")
        }
    }
}
