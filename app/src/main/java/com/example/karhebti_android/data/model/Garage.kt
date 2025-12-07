package com.example.karhebti_android.data.model

data class Garage(
    val id: String,
    val nom: String,
    val adresse: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Double, // in km
    val rating: Float,
    val reviewCount: Int,
    val phoneNumber: String,
    val isOpen: Boolean,
    val openUntil: String? = null,
    val services: List<GarageService>,
    val imageUrl: String? = null
)

enum class GarageService {
    REVISION,
    VIDANGE,
    PNEUS,
    FREINS,
    CONTROLE_TECHNIQUE,
    BATTERIE,
    CLIMATISATION,
    CARROSSERIE
}
