package com.example.karhebti_android.data

import com.google.gson.annotations.SerializedName

// Data class représentant une panne (breakdown)
// Alignée avec la réponse du backend (MongoDB ObjectId -> String)
data class BreakdownResponse(
    @SerializedName("_id")
    val id: String,

    @SerializedName("user_id")
    val userId: String?,

    @SerializedName("vehicle_id")
    val vehicleId: String?,

    val type: String = "UNKNOWN",

    val status: String = "OPEN",

    val description: String?,

    val latitude: Double?,
    val longitude: Double?,

    @SerializedName("assigned_to")
    val assignedTo: String?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("updated_at")
    val updatedAt: String?
)
