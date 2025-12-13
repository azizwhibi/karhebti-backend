package com.example.karhebti_android.data.model

import java.util.Date

data class Entretien(
    val id: String,
    val vehicleId: String,
    val vehicleName: String,
    val type: MaintenanceType,
    val title: String,
    val description: String,
    val date: Date,
    val location: String,
    val garageName: String? = null,
    val price: Double,
    val status: EntretienStatus,
    val urgency: UrgencyLevel,
    val tasks: List<String> = emptyList(),
    val mileage: Int? = null,
    val daysUntil: Int? = null,
    val kmUntil: Int? = null
)

enum class MaintenanceType {
    VIDANGE, // Oil change
    REVISION, // General revision
    PNEUS, // Tires
    FREINS, // Brakes
    FILTRE, // Filter
    CONTROLE_TECHNIQUE, // Technical inspection
    BATTERIE, // Battery
    AUTRE // Other
}

enum class EntretienStatus {
    A_VENIR, // Upcoming
    TERMINE, // Completed
    EN_COURS, // In progress
    ANNULE // Cancelled
}

enum class UrgencyLevel {
    NORMAL,
    ATTENTION,
    URGENT
}

