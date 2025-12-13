package com.example.karhebti_android.data.model

data class Vehicle(
    val id: String,
    val name: String,
    val brand: String,
    val model: String,
    val year: Int,
    val plateNumber: String,
    val mileage: Int,
    val status: VehicleStatus,
    val nextMaintenance: String? = null,
    val nextMaintenanceDays: Int? = null,
    val imageUrl: String? = null
)

enum class VehicleStatus {
    BON, // Good
    ATTENTION, // Attention needed
    URGENT // Urgent maintenance needed
}

