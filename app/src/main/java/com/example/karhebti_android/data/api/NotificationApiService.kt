package com.example.karhebti_android.data.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*
import java.lang.reflect.Type

/**
 * Notification-related endpoints extension to KarhebtiApiService
 * These methods should be added to the existing KarhebtiApiService interface
 */
interface NotificationApiService {

    // Notifications - Device Token
    @POST("notifications/update-device-token")
    suspend fun updateDeviceToken(
        @Body request: DeviceTokenRequest
    ): Response<UpdateTokenResponse>

    // Notifications - Fetch
    @GET("notifications")
    suspend fun getNotifications(): Response<NotificationsResponse>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>

    // Notifications - Actions
    @PATCH("notifications/{id}/read")
    suspend fun markNotificationAsRead(
        @Path("id") notificationId: String
    ): Response<NotificationItemResponse>

    @PATCH("notifications/mark-all-read")
    suspend fun markAllNotificationsAsRead(): Response<MarkAllReadResponse>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(
        @Path("id") notificationId: String
    ): Response<Void>

    // SOS/Breakdown Actions
    @POST("breakdowns/{id}/accept")
    suspend fun acceptBreakdownRequest(
        @Path("id") breakdownId: String
    ): Response<BreakdownActionResponse>

    @POST("breakdowns/{id}/reject")
    suspend fun rejectBreakdownRequest(
        @Path("id") breakdownId: String
    ): Response<BreakdownActionResponse>
}

// Request models
data class DeviceTokenRequest(
    @SerializedName("deviceToken")
    val deviceToken: String
)

data class UpdateTokenResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String
)

data class MarkAllReadResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("updatedCount")
    val updatedCount: Int
)

data class NotificationItemResponse(
    @SerializedName("_id")
    val id: String,

    @SerializedName("userId")
    val userId: String,

    // Backend can send either "title" or "titre"
    @SerializedName("title")
    val title: String?,

    // Backend can send either "body" or "message"
    @SerializedName("body")
    val body: String?,

    // Backend sends "status" (pending/read), we map to isRead
    @SerializedName("isRead")
    val isRead: Boolean = false,

    @SerializedName("createdAt")
    val createdAt: String?,

    @SerializedName("type")
    val type: String? = null, // Type de notification: "BREAKDOWN_REQUEST", "DOCUMENT_EXPIRY", etc.

    @SerializedName("data")
    val data: Map<String, String> = emptyMap()
) {
    // Helper pour vérifier si c'est une demande SOS
    fun isBreakdownRequest(): Boolean = type == "BREAKDOWN_REQUEST"

    // Extraire l'ID de la panne depuis les données
    fun getBreakdownId(): String? = data["breakdownId"]

    // Extraire la latitude de la panne
    fun getLatitude(): Double? = data["latitude"]?.toDoubleOrNull()

    // Extraire la longitude de la panne
    fun getLongitude(): Double? = data["longitude"]?.toDoubleOrNull()

    // Extraire le type de panne
    fun getBreakdownType(): String? = data["breakdownType"]
}

data class NotificationsResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("data")
    val data: List<NotificationItemResponse> = emptyList(),

    @SerializedName("metadata")
    val metadata: NotificationMetadata? = null
)

data class NotificationMetadata(
    @SerializedName("total")
    val total: Int = 0,

    @SerializedName("unreadCount")
    val unreadCount: Int = 0
)

data class BreakdownActionResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String = "",

    @SerializedName("breakdown")
    val breakdown: Any? = null
)

/**
 * Deserializer personnalisé pour NotificationItemResponse
 * Gère les champs français du backend: titre -> title, message -> body, status -> isRead
 */
class NotificationItemDeserializer : JsonDeserializer<NotificationItemResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): NotificationItemResponse {
        val jsonObject = json.asJsonObject

        val id = jsonObject.get("_id")?.asString ?: ""
        val userId = jsonObject.get("userId")?.asString ?: ""

        // Backend sends "titre" (French), map to "title"
        val title = jsonObject.get("titre")?.asString
            ?: jsonObject.get("title")?.asString
            ?: "(Sans titre)"

        // Backend sends "message" (French), map to "body"
        val body = jsonObject.get("message")?.asString
            ?: jsonObject.get("body")?.asString
            ?: ""

        // Backend sends "status" with values "pending"/"read", map to boolean
        val status = jsonObject.get("status")?.asString ?: "pending"
        val isRead = status == "read"

        val createdAt = jsonObject.get("createdAt")?.asString ?: ""

        // Optional data field
        val data = try {
            if (jsonObject.has("data") && jsonObject.get("data").isJsonObject) {
                context.deserialize<Map<String, String>>(
                    jsonObject.get("data"),
                    Map::class.java
                ) as Map<String, String>
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }

        return NotificationItemResponse(
            id = id,
            userId = userId,
            title = title,
            body = body,
            isRead = isRead,
            createdAt = createdAt,
            data = data
        )
    }
}

/**
 * Deserializer personnalisé pour NotificationsResponse
 * Gère les cas où le serveur envoie une réponse malformée
 */
class NotificationsResponseDeserializer : JsonDeserializer<NotificationsResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): NotificationsResponse {
        return try {
            val jsonObject = json.asJsonObject

            val success = jsonObject.get("success")?.asBoolean ?: false

            // Essayer d'obtenir data comme tableau
            val dataList: List<NotificationItemResponse> = try {
                if (jsonObject.has("data") && jsonObject.get("data").isJsonArray) {
                    val array = context.deserialize<Array<NotificationItemResponse>>(
                        jsonObject.get("data"),
                        Array<NotificationItemResponse>::class.java
                    )
                    array.toList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }

            // Essayer d'obtenir metadata
            val metadata: NotificationMetadata? = try {
                if (jsonObject.has("metadata") && jsonObject.get("metadata").isJsonObject) {
                    context.deserialize<NotificationMetadata>(
                        jsonObject.get("metadata"),
                        NotificationMetadata::class.java
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }

            NotificationsResponse(
                success = success,
                data = dataList,
                metadata = metadata
            )
        } catch (e: Exception) {
            NotificationsResponse(success = false, data = emptyList(), metadata = null)
        }
    }
}
