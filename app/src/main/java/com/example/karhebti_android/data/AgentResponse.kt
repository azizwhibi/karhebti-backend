package com.example.karhebti_android.data

// Data class représentant un agent d'assistance
// (optionnel, pour TrackingScreen)
data class AgentResponse(
    val id: Int,
    val name: String,
    val phone: String,
    val photoUrl: String?,
    val is_available: Boolean,
    val latitude: Double?,
    val longitude: Double?
)
