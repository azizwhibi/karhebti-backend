package com.example.karhebti_android.data.api

import com.google.gson.annotations.SerializedName

/**
 * DTO alignment with backend
 * All field names must match backend exactly
 */

// ==================== AUTH DTOs ====================

data class SignupRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val motDePasse: String,
    val telephone: String
)

data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("motDePasse")
    val motDePasse: String
)

data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("user")
    val user: UserResponse
)

data class ForgotPasswordRequest(
    val email: String
)

data class VerifyOtpRequest(
    val email: String,
    val otp: String
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    @SerializedName("newPassword")
    val newPassword: String
)

// User DTOs utilisés par KarhebtiApiService et ailleurs
data class IdWrapper(@SerializedName("\$oid") val oid: String)
data class UserResponse(
    @SerializedName("_id")
    val id: Any?,

    val nom: String,

    val prenom: String,

    val email: String,

    val telephone: String?,

    val role: String,

    val createdAt: String? = null,

    val updatedAt: String? = null
){
    // Helper method to get ID as String regardless of format
    fun getIdString(): String? {
        return when (id) {
            is String -> id
            is IdWrapper -> id.oid
            is Map<*, *> -> (id["\$oid"] as? String) ?: (id["oid"] as? String)
            else -> null
        }
    }
}

data class UpdateUserRequest(
    val nom: String? = null,

    val prenom: String? = null,

    val telephone: String? = null
)

data class UpdateRoleRequest(
    val role: String
)

// ==================== NOTIFICATION DTOs ====================

data class NotificationResponse(
    @SerializedName("_id")
    val id: String,

    val titre: String,

    val message: String,

    val type: String,

    val user: UserResponse? = null,

    val document: DocumentResponse? = null,

    val lu: Boolean = false,

    val dateEcheance: String? = null,

    val createdAt: String? = null,

    val updatedAt: String? = null,

    val data: Map<String, Any>? = null
)

// ==================== ERROR DTO ====================

data class ErrorResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String,

    @SerializedName("error")
    val error: String? = null,

    @SerializedName("statusCode")
    val statusCode: Int? = null
)

// Message générique utilisé largement

data class MessageResponse(
    val message: String
)
