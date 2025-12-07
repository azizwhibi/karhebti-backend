package com.example.karhebti_android.data.api

import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.JsonAdapter
import java.util.Date

// Ce fichier contient uniquement les modèles métier (cars, maintenances, garages, documents, etc.).
// Tous les DTOs d'authentification, de notifications et les réponses génériques
// (SignupRequest, LoginRequest, AuthResponse, UserResponse, ErrorResponse,
// NotificationResponse, MessageResponse, etc.) sont définis dans DTOs.kt.

// Car DTOs
data class CreateCarRequest(
    val marque: String,
    val modele: String,
    val annee: Int,
    val immatriculation: String,
    val typeCarburant: String
    // Note: kilometrage should NOT be included in car creation
    // It should only be updated after creation via UpdateCarRequest
)

data class UpdateCarRequest(
    val marque: String? = null,
    val modele: String? = null,
    val annee: Int? = null,
    val typeCarburant: String? = null,
    val kilometrage: Int? = null,
    val statut: String? = null,
    val prochainEntretien: String? = null,
    val joursProchainEntretien: Int? = null,
    val imageUrl: String? = null
)

data class CarResponse(
    @SerializedName("_id")
    val id: String,
    val marque: String,
    val modele: String,
    val annee: Int,
    val immatriculation: String,
    val typeCarburant: String,
    val kilometrage: Int? = null,
    val statut: String? = null, // "BON", "ATTENTION", "URGENT"
    val prochainEntretien: String? = null,
    val joursProchainEntretien: Int? = null,
    val imageUrl: String? = null,
    val price: Double? = null,
    val description: String? = null,
    @SerializedName("forSale")
    val isForSale: Boolean = false,
    val saleStatus: String? = null,
    @JsonAdapter(FlexibleUserDeserializer::class)
    val user: String? = null, // Can be either user ID string or user object
    val createdAt: Date,
    val updatedAt: Date
)

// Maintenance DTOs
data class CreateMaintenanceRequest(
    val type: String, // vidange, révision, réparation
    val title: String, // vidange, révision, réparation
    val date: String, // ISO 8601 format
    val dueAt: String, // ISO 8601 format
    val cout: Double,
    val garage: String, // Garage ID
    val voiture: String // Car ID
)

data class UpdateMaintenanceRequest(
    val type: String? = null,
    val date: String? = null,
    val cout: Double? = null
)

data class MaintenanceResponse(
    @SerializedName("_id")
    val id: String,
    val type: String,
    val date: Date,
    val cout: Double = 0.0,
    val status: String? = "pending",
    @JsonAdapter(FlexibleGarageDeserializer::class)
    val garage: String? = null, // garage ID or extracted from object
    @JsonAdapter(FlexibleCarDeserializer::class)
    val voiture: String? = null, // car ID or extracted from object
    val user: String? = null, // User ID who created the maintenance
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

// Garage DTOs


// Document DTOs
data class CreateDocumentRequest(
    val type: String, // assurance, carte grise, contrôle technique
    val dateEmission: String,
    val dateExpiration: String,
    val fichier: String,
    val voiture: String
)

data class UpdateDocumentRequest(
    val type: String? = null,
    val dateEmission: String? = null,
    val dateExpiration: String? = null,
    val fichier: String? = null,
    val description: String? = null,
    val etat: String? = null
)

data class DocumentResponse(
    @SerializedName("_id")
    val id: String,
    val type: String,
    val dateEmission: Date,
    val dateExpiration: Date,
    val fichier: String,
    @JsonAdapter(FlexibleCarDeserializer::class)
    val voiture: String? = null, // Can be either car ID string or car object
    val createdAt: Date,
    val updatedAt: Date,
    val description: String? = null,
    val etat: String? = null
)

// OCR DTOs
data class OcrDocumentData(
    val type: String,           // "assurance", "carte_grise", "permis", "visite_technique", "inconnu"
    val dateEmission: String?,  // ISO 8601 format: "2024-01-01T00:00:00.000Z"
    val dateExpiration: String? // ISO 8601 format: "2025-12-31T00:00:00.000Z"
)

data class OcrDocumentResponse(
    val success: Boolean,
    val data: OcrDocumentData
)

// Part DTOs
data class CreatePartRequest(
    val nom: String,
    val type: String,
    val dateInstallation: String,
    val kilometrageRecommande: Int,
    val voiture: String
)

data class PartResponse(
    @SerializedName("_id")
    val id: String,
    val nom: String,
    val type: String,
    val dateInstallation: Date,
    val kilometrageRecommande: Int,
    val voiture: String? = null, // Changed from CarResponse to String (car ID)
    val createdAt: Date,
    val updatedAt: Date
)

// ==================== NEW FEATURES DTOs ====================

// Image Upload DTOs
data class ImageMeta(
    val width: Int? = null,
    val height: Int? = null,
    val format: String? = null,
    val size: Int? = null
)

data class CarImageResponse(
    @SerializedName("_id")
    val id: String,
    val imageUrl: String?,
    val imageMeta: ImageMeta?
)

// OTP Login DTOs
data class SendOtpLoginRequest(
    val identifier: String // email or phone
)

data class VerifyOtpLoginRequest(
    val identifier: String,
    val code: String
)

data class OtpResponse(
    val ok: Boolean,
    val message: String,
    val code: String? = null // Only in dev mode
)

// Email Verification DTOs
data class SendEmailVerificationRequest(
    val email: String
)

data class VerifyEmailRequest(
    val email: String,
    val code: String
)

data class EmailVerificationResponse(
    val ok: Boolean,
    val message: String
)

// Entretiens Filter/Search DTOs
data class EntretiensFilterParams(
    val search: String? = null,
    val status: String? = null, // urgent, bientot, overdue
    val dateFrom: String? = null, // ISO-8601
    val dateTo: String? = null, // ISO-8601
    val tags: List<String>? = null,
    val minCost: Double? = null,
    val maxCost: Double? = null,
    val minMileage: Int? = null,
    val maxMileage: Int? = null,
    val sort: String? = "dueAt", // dueAt, createdAt, cout, mileage
    val order: String? = "asc", // asc, desc
    val page: Int = 1,
    val limit: Int = 20
)

data class MaintenanceExtendedResponse(
    @SerializedName("_id")
    val id: String,
    val type: String,
    val title: String,
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    val status: String, // urgent, bientot
    val date: Date,
    val dueAt: Date,
    val cout: Double = 0.0,
    val mileage: Int? = null,
    val isOverdue: Boolean = false,
    @JsonAdapter(FlexibleGarageDeserializer::class)
    val garage: String? = null,
    @JsonAdapter(FlexibleCarDeserializer::class)
    val voiture: String? = null,
    val ownerId: String? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class PaginatedMaintenancesResponse(
    val data: List<MaintenanceExtendedResponse>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

// Widget DTOs
data class UpcomingMaintenanceWidget(
    @SerializedName("_id")
    val id: String,
    val title: String,
    val voiture: String,
    val dueAt: Date,
    val status: String,
    val plate: String? = null
)

// Reclamation (Feedback) DTOs
data class CreateReclamationRequest(
    val type: String,
    val titre: String,
    val message: String,
    val garage: String? = null,
    val service: String? = null
)

data class UpdateReclamationRequest(
    val titre: String? = null,
    val message: String? = null
)

data class ReclamationResponse(
    @SerializedName("_id")
    val id: String,
    val type: String,
    val titre: String,
    val message: String,
    @JsonAdapter(FlexibleUserDeserializer::class)
    val user: String? = null,
    val garage: GarageResponse? = null,
    val service: ServiceResponse? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

// ==================== SERVICES DTOs ====================



// ==================== AI FEATURES DTOs ====================

// Road Issue Reporting
data class ReportRoadIssueRequest(
    val type: String, // pothole, accident, etc.
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val severity: String? = null // low, medium, high
)

data class RoadIssueResponse(
    @SerializedName("_id")
    val id: String,
    val type: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val severity: String,
    val status: String,
    val createdAt: Date
)

// Danger Zones
data class DangerZone(
    @SerializedName("_id")
    val id: String,
    val type: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val severity: String,
    val radius: Double? = null,
    val reportCount: Int? = null
)

// Maintenance Recommendations
data class MaintenanceRecommendationRequest(
    val carId: String,
    val mileage: Int,
    val lastMaintenanceDate: String? = null
)

data class MaintenanceRecommendationResponse(
    val recommendations: List<MaintenanceRecommendation>,
    val urgentItems: List<MaintenanceRecommendation>,
    val estimatedTotalCost: Double
)

data class MaintenanceRecommendation(
    val type: String,
    val description: String,
    val priority: String, // low, medium, high, urgent
    val estimatedCost: Double,
    val dueInKm: Int? = null,
    val dueInDays: Int? = null
)

// Garage Recommendations


// ==================== SIGNUP VERIFICATION DTOs ====================
// Request to verify OTP and complete signup
data class VerifySignupOtpRequest(
    val email: String,
    val code: String
)

// ==================== TRANSLATION API MODELS ====================

// Translation API DTOs
data class TranslateRequest(
    val text: List<String>,  // Can send multiple strings
    val targetLanguage: String,
    val sourceLanguage: String? = null
)

data class TranslateResponse(
    val translations: List<String>
)

data class BatchTranslateItem(
    val key: String,
    val text: String
)

data class BatchTranslateRequest(
    val items: List<BatchTranslateItem>,
    val targetLanguage: String,
    val sourceLanguage: String? = null
)

data class BatchTranslateResponse(
    val translations: Map<String, String>
)

data class Language(
    val code: String,
    val name: String,
    val nativeName: String
)

data class LanguagesResponse(
    val languages: List<Language>
)

data class CachedTranslationsResponse(
    val translations: Map<String, String>,
    val count: Int
)

// Local model for Room database
data class TranslationEntity(
    val id: String = "",
    val key: String,
    val languageCode: String,
    val originalText: String,
    val translatedText: String,
    val updatedAt: Long = System.currentTimeMillis()
)

// ==================== MARKETPLACE / SWIPE FEATURE DTOs ====================

// Car listing for marketplace
data class MarketplaceCarResponse(
    @SerializedName("_id")
    val id: String,
    val marque: String,
    val modele: String,
    val annee: Int,
    val immatriculation: String,
    val typeCarburant: String,
    val kilometrage: Int? = null,
    val statut: String? = null,
    val imageUrl: String? = null,
    val images: List<String>? = null,
    val imageMeta: ImageMeta? = null,
    val price: Double? = null,
    val description: String? = null,
    @SerializedName("forSale")
    val isForSale: Boolean = false,
    val saleStatus: String? = null,
    @JsonAdapter(FlexibleUserDeserializer::class)
    val user: String? = null,
    val ownerName: String? = null,
    val ownerPhone: String? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    @SerializedName("__v")
    val version: Int? = null
)

// Swipe request
data class CreateSwipeRequest(
    val carId: String,
    val direction: String // "left" or "right"
)

// Swipe response
data class SwipeResponse(
    @SerializedName("_id")
    val id: String,
    @SerializedName("carId")
    @JsonAdapter(FlexibleCarObjectDeserializer::class)
    val carDetails: MarketplaceCarResponse? = null,
    @SerializedName("userId")
    @JsonAdapter(FlexibleUserObjectDeserializer::class)
    val buyerDetails: UserResponse? = null,
    @JsonAdapter(FlexibleUserObjectDeserializer::class)
    val sellerId: UserResponse? = null,
    val direction: String? = null,
    val status: String? = null, // "pending", "accepted", "declined"
    val createdAt: Date? = null,
    @SerializedName("__v")
    val version: Int? = null
) {
    // Helper properties for backward compatibility
    val carId: String? get() = carDetails?.id
    val buyerId: String? get() = buyerDetails?.getIdString()
    val sellerDetails: UserResponse? get() = sellerId
}

// Response when seller accepts/declines
data class SwipeStatusResponse(
    @SerializedName("_id")
    val id: String,
    val status: String,
    val conversationId: String? = null,
    val message: String
)

// My swipes list
data class MySwipesResponse(
    val sent: List<SwipeResponse>,
    val received: List<SwipeResponse>
)

// Conversation response
data class ConversationResponse(
    @SerializedName("_id")
    val id: String,
    @JsonAdapter(FlexibleCarObjectDeserializer::class)
    val carId: MarketplaceCarResponse? = null,
    @JsonAdapter(FlexibleUserObjectDeserializer::class)
    val buyerId: UserResponse? = null,
    @JsonAdapter(FlexibleUserObjectDeserializer::class)
    val sellerId: UserResponse? = null,
    val participants: List<String>? = null,
    val status: String? = null,
    val messages: List<Any>? = null, // Can be message objects or IDs
    val lastMessage: String? = null,
    val lastMessageAt: Date? = null,
    val unreadCount: Int? = null,
    val unreadCountBuyer: Int? = null,
    val unreadCountSeller: Int? = null,
    val carDetails: MarketplaceCarResponse? = null,
    val otherUser: UserResponse? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    @SerializedName("__v")
    val version: Int? = null
) {
    // Helper property to get the car (prioritizes carId over carDetails)
    val car: MarketplaceCarResponse?
        get() = carId ?: carDetails

    // Helper property to determine which user is "other" based on current user
    fun getOtherUser(currentUserId: String): UserResponse? {
        return when {
            otherUser != null -> otherUser
            buyerId?.id == currentUserId -> sellerId
            sellerId?.id == currentUserId -> buyerId
            else -> buyerId ?: sellerId // Fallback to any available user
        }
    }
}

// Chat message
data class ChatMessage(
    @SerializedName("_id")
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val isRead: Boolean = false,
    val createdAt: Date
)

// Send message request
data class SendMessageRequest(
    val content: String
)

// Unread count
data class UnreadCount(
    val conversationId: String,
    val count: Int
)

data class UnreadCountResponse(
    val success: Boolean,
    val unreadCount: Int
)

data class ListCarForSaleRequest(
    val price: Double,
    val description: String? = null
)

// WebSocket message types
data class WsJoinConversation(
    val conversationId: String
)

data class WsLeaveConversation(
    val conversationId: String
)

data class WsSendMessage(
    val conversationId: String,
    val content: String
)

data class WsTyping(
    val conversationId: String
)

data class WsMessageReceived(
    val event: String = "new_message",
    val data: ChatMessage
)

data class WsNotificationReceived(
    val event: String = "notification",
    val data: NotificationResponse
)

data class WsUserTyping(
    val event: String = "user_typing",
    val userId: String,
    val conversationId: String
)

data class WsUserStatus(
    val event: String, // "user_online" or "user_offline"
    val userId: String
)


data class GarageRecommendation(
    val id: String,
    val nom: String,
    val adresse: String,
    val telephone: String,
    val note: Double,
    val services: List<String>,
    val distanceEstimee: String,
    val recommande: Boolean
)





// ==================== GARAGE DTOs ====================

data class GarageResponse(
    @SerializedName("_id") val id: String,
    val nom: String,
    val adresse: String,
    val telephone: String,
    val noteUtilisateur: Double,
    val serviceTypes: List<String>? = null,
    val heureOuverture: String? = null,
    val heureFermeture: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("numberOfBays") val numberOfBays: Int? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)
// ✅ Nouvelle réponse pour la création de garage qui inclut les repair bays
data class CreateGarageResponse(
    val garage: GarageResponse,
    val repairBays: List<RepairBayResponse>? = null
)
data class CreateGarageRequest(
    val nom: String,
    val adresse: String,
    val telephone: String,
    val noteUtilisateur: Double?,
    val heureOuverture: String?,
    val heureFermeture: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("numberOfBays") val numberOfBays: Int? = null
)

data class UpdateGarageRequest(
    val nom: String? = null,
    val adresse: String? = null,
    val telephone: String? = null,
    val noteUtilisateur: Double? = null,
    val heureOuverture: String? = null,
    val heureFermeture: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val numberOfBays: Int? = null
)

// ==================== SERVICE DTOs ====================

data class ServiceResponse(
    @SerializedName("_id") val id: String,
    val type: String,
    val coutMoyen: Double,
    val dureeEstimee: Int,
    val garage: Any?,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class UpdateServiceRequest(
    val type: String,
    val coutMoyen: Double,
    val dureeEstimee: Int
)

data class CreateServiceRequest(
    val type: String,
    val coutMoyen: Double,
    val dureeEstimee: Int,
    val garage: String
)

data class GarageServiceRequest(
    val type: String,
    val coutMoyen: Double,
    val dureeEstimee: Int
)

// ==================== RESERVATION DTOs ====================

data class CreateReservationRequest(
    @SerializedName("userId") val userId: String? = null,
    val email: String? = null,
    @SerializedName("garageId") val garageId: String,
    val date: String,
    @SerializedName("heureDebut") val heureDebut: String,
    @SerializedName("heureFin") val heureFin: String,
    val services: List<String>? = null,
    val status: String = "en_attente",
    val commentaires: String? = null
)

data class UpdateReservationRequest(
    val date: String? = null,
    @SerializedName("heureDebut") val heureDebut: String? = null,
    @SerializedName("heureFin") val heureFin: String? = null,
    val services: List<String>? = null,
    val status: String? = null,
    val commentaires: String? = null,
    val isPaid: Boolean? = null
)

data class UpdateReservationStatusRequest(
    val status: String
)

data class ReservationResponse(
    @SerializedName("_id") val id: String,
    @SerializedName("userId") val userId: Any?,
    @SerializedName("garageId") val garageId: Any?,
    @SerializedName("repairBayId") val repairBayId: Any? = null,
    val date: Date,
    @SerializedName("heureDebut") val heureDebut: String,
    @SerializedName("heureFin") val heureFin: String,
    val services: List<String>? = null,
    val status: String,
    val commentaires: String? = null,
    @SerializedName("updatedBy") val updatedBy: Any? = null,
    @SerializedName("isPaid") val isPaid: Boolean = false,
    @SerializedName("totalAmount") val totalAmount: Double = 0.0,
    @SerializedName("createdAt") val createdAt: Date? = null,
    @SerializedName("updatedAt") val updatedAt: Date? = null
) {
    // Helper methods with support for both String and IdWrapper formats
    fun getUserId(): String? {
        return when (userId) {
            is String -> userId
            is UserResponse -> userId.getIdString()
            is Map<*, *> -> (userId["_id"] as? String) ?: (userId["id"] as? String)
            else -> null
        }
    }

    fun getUserName(): String? {
        return when (userId) {
            is UserResponse -> "${userId.prenom} ${userId.nom}"
            is Map<*, *> -> {
                val prenom = userId["prenom"] as? String ?: ""
                val nom = userId["nom"] as? String ?: ""
                "$prenom $nom".trim()
            }
            else -> null
        }
    }

    fun getUserEmail(): String? {
        return when (userId) {
            is UserResponse -> userId.email
            is Map<*, *> -> userId["email"] as? String
            else -> null
        }
    }

    fun getGarageId(): String? {
        return when (garageId) {
            is String -> garageId
            is GarageResponse -> garageId.id
            is Map<*, *> -> (garageId["_id"] as? String) ?: (garageId["id"] as? String)
            else -> null
        }
    }

    fun getGarageName(): String? {
        return when (garageId) {
            is GarageResponse -> garageId.nom
            is Map<*, *> -> garageId["nom"] as? String
            else -> null
        }
    }

    fun getGarageAddress(): String? {
        return when (garageId) {
            is GarageResponse -> garageId.adresse
            is Map<*, *> -> garageId["adresse"] as? String
            else -> null
        }
    }

    fun getGaragePhone(): String? {
        return when (garageId) {
            is GarageResponse -> garageId.telephone
            is Map<*, *> -> garageId["telephone"] as? String
            else -> null
        }
    }

    fun getUpdatedByName(): String? {
        return when (updatedBy) {
            is UserResponse -> "${updatedBy.prenom} ${updatedBy.nom}"
            is Map<*, *> -> {
                val prenom = updatedBy["prenom"] as? String ?: ""
                val nom = updatedBy["nom"] as? String ?: ""
                "$prenom $nom".trim()
            }
            else -> null
        }
    }

    fun getRepairBayId(): String? {
        return when (repairBayId) {
            is String -> repairBayId
            is RepairBayResponse -> repairBayId.id
            is Map<*, *> -> (repairBayId["_id"] as? String) ?: (repairBayId["id"] as? String)
            else -> null
        }
    }

    fun getRepairBayName(): String? {
        return when (repairBayId) {
            is RepairBayResponse -> repairBayId.name
            is Map<*, *> -> repairBayId["name"] as? String
            else -> null
        }
    }

    fun getRepairBayNumber(): Int? {
        return when (repairBayId) {
            is RepairBayResponse -> repairBayId.bayNumber
            is Map<*, *> -> repairBayId["bayNumber"] as? Int
            else -> null
        }
    }
}

data class ReservationListResponse(
    val reservations: List<ReservationResponse>,
    val total: Int? = null,
    val page: Int? = null,
    val limit: Int? = null,
    val totalPages: Int? = null
)

data class PopulatedReservationResponse(
    @SerializedName("_id") val id: String,
    val userId: UserResponse?,
    val garageId: GarageResponse?,
    val date: String,
    val heureDebut: String,
    val heureFin: String,
    val status: String,
    val createdAt: Date?,
    val updatedAt: Date?
)

// ==================== REPAIR BAY DTOs ====================

data class RepairBayResponse(
    @SerializedName("_id") val id: String,
    @SerializedName("garageId") val garageId: String,
    @SerializedName("bayNumber") val bayNumber: Int,
    val name: String,
    @SerializedName("heureOuverture") val heureOuverture: String,
    @SerializedName("heureFermeture") val heureFermeture: String,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("createdAt") val createdAt: Date? = null,
    @SerializedName("updatedAt") val updatedAt: Date? = null
)

data class CreateRepairBayRequest(
    @SerializedName("garageId") val garageId: String,
    @SerializedName("bayNumber") val bayNumber: Int,
    val name: String,
    @SerializedName("heureOuverture") val heureOuverture: String,
    @SerializedName("heureFermeture") val heureFermeture: String,
    @SerializedName("isActive") val isActive: Boolean = true
)

// ==================== LOCATION DTOs ====================

data class LocationSuggestion(
    val display_name: String,
    val lat: String,
    val lon: String,
    val address: OsmAddress?
)

data class OsmAddress(
    val road: String?,
    val city: String?,
    val country: String?,
    val postcode: String?
)

data class OsmLocationSuggestion(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val address: AddressDetails
)

data class AddressDetails(
    val road: String?,
    val city: String?,
    val country: String?,
    val postcode: String?
)

