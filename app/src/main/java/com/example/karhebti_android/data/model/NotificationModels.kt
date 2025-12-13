package com.example.karhebti_android.data.models

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ExpiringDocument(
    @SerializedName("id")
    val id: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("dateExpiration")
    val dateExpiration: String, // Format: "2025-11-22T00:00:00.000Z"

    @SerializedName("daysUntilExpiration")
    val daysUntilExpiration: Int,

    @SerializedName("willReceiveNotification")
    val willReceiveNotification: Boolean
) {
    val urgencyLevel: UrgencyLevel
        get() = when {
            daysUntilExpiration <= 3 -> UrgencyLevel.CRITICAL
            daysUntilExpiration <= 7 -> UrgencyLevel.WARNING
            else -> UrgencyLevel.INFO
        }

    val urgencyColor: Long
        get() = when (urgencyLevel) {
            UrgencyLevel.CRITICAL -> 0xFFDC3545 // Red
            UrgencyLevel.WARNING -> 0xFFFFC107  // Orange
            UrgencyLevel.INFO -> 0xFF28A745    // Green
        }

    val urgencyIcon: String
        get() = when (urgencyLevel) {
            UrgencyLevel.CRITICAL -> "🔴"
            UrgencyLevel.WARNING -> "🟡"
            UrgencyLevel.INFO -> "🟢"
        }
}

enum class UrgencyLevel {
    CRITICAL, WARNING, INFO
}

// Response wrapper
data class ExpiringDocumentsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: List<ExpiringDocument>,

    @SerializedName("count")
    val count: Int
)

