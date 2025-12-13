package com.example.karhebti_android.data.model

import java.util.Date

data class Document(
    val id: String,
    val vehicleId: String,
    val vehicleName: String,
    val name: String,
    val type: DocumentType,
    val subtype: String,
    val uploadDate: Date,
    val expiryDate: Date? = null,
    val fileUrl: String,
    val fileSize: Long,
    val mimeType: String,
    val description: String? = null,
    val etat: String? = null
)

enum class DocumentType {
    ADMINISTRATIF, // Administrative (insurance, registration)
    ENTRETIEN, // Maintenance
    FACTURE, // Invoice
    AUTRE // Other
}
