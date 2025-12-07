package com.example.karhebti_android.data.api

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Custom deserializer for user field that can be either a String (ID) or an object
 */
class FlexibleUserDeserializer : JsonDeserializer<String?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): String? {
        if (json == null || json.isJsonNull) {
            return null
        }

        return when {
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                json.asString
            }
            json.isJsonObject -> {
                // Extract the _id field from the user object
                json.asJsonObject.get("_id")?.asString
            }
            else -> null
        }
    }
}

/**
 * Custom deserializer for user field that preserves the full UserResponse object
 * Used in ConversationResponse to get full user details
 */
class FlexibleUserObjectDeserializer : JsonDeserializer<UserResponse?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): UserResponse? {
        if (json == null || json.isJsonNull) {
            return null
        }

        return when {
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                // If it's just an ID string, we can't create a full UserResponse
                // Return null and let the app handle it
                null
            }
            json.isJsonObject -> {
                // Parse the full user object
                try {
                    val obj = json.asJsonObject
                    UserResponse(
                        id = obj.get("_id")?.asString,
                        nom = obj.get("nom")?.asString ?: "",
                        prenom = obj.get("prenom")?.asString ?: "",
                        email = obj.get("email")?.asString ?: "",
                        telephone = obj.get("telephone")?.asString,
                        role = obj.get("role")?.asString ?: "utilisateur",
                        createdAt = obj.get("createdAt")?.asString,
                        updatedAt = obj.get("updatedAt")?.asString
                    )
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }
}

/**
 * Custom deserializer for garage field that can be either a String (ID) or an object
 */
class FlexibleGarageDeserializer : JsonDeserializer<String?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): String? {
        if (json == null || json.isJsonNull) {
            return null
        }

        return when {
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                json.asString
            }
            json.isJsonObject -> {
                // Extract the _id field from the garage object
                json.asJsonObject.get("_id")?.asString
            }
            else -> null
        }
    }
}

/**
 * Custom deserializer for voiture/car field that can be either a String (ID) or an object
 */
class FlexibleCarDeserializer : JsonDeserializer<String?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): String? {
        if (json == null || json.isJsonNull) {
            return null
        }

        return when {
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                json.asString
            }
            json.isJsonObject -> {
                // Extract the _id field from the car object
                json.asJsonObject.get("_id")?.asString
            }
            else -> null
        }
    }
}

/**
 * Custom deserializer for car field that preserves the full MarketplaceCarResponse object
 * Used in ConversationResponse to get full car details
 */
class FlexibleCarObjectDeserializer : JsonDeserializer<MarketplaceCarResponse?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): MarketplaceCarResponse? {
        if (json == null || json.isJsonNull) {
            return null
        }

        return when {
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                // If it's just an ID string, we can't create a full car response
                null
            }
            json.isJsonObject -> {
                // Parse the full car object
                try {
                    val obj = json.asJsonObject
                    MarketplaceCarResponse(
                        id = obj.get("_id")?.asString ?: "",
                        marque = obj.get("marque")?.asString ?: "",
                        modele = obj.get("modele")?.asString ?: "",
                        annee = obj.get("annee")?.asInt ?: 0,
                        immatriculation = obj.get("immatriculation")?.asString ?: "",
                        typeCarburant = obj.get("typeCarburant")?.asString ?: "",
                        kilometrage = obj.get("kilometrage")?.asInt,
                        statut = obj.get("statut")?.asString,
                        imageUrl = obj.get("imageUrl")?.asString,
                        images = null,
                        imageMeta = null,
                        price = obj.get("price")?.asDouble,
                        description = obj.get("description")?.asString,
                        isForSale = obj.get("forSale")?.asBoolean ?: false,
                        saleStatus = obj.get("saleStatus")?.asString,
                        user = obj.get("user")?.asString,
                        ownerName = obj.get("ownerName")?.asString,
                        ownerPhone = obj.get("ownerPhone")?.asString,
                        createdAt = null,
                        updatedAt = null,
                        version = obj.get("__v")?.asInt
                    )
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }
}

/**
 * Custom deserializer for service field that can be either a String (ID) or an object
 */
class FlexibleServiceDeserializer : JsonDeserializer<String?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): String? {
        if (json == null || json.isJsonNull) {
            return null
        }

        return when {
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                json.asString
            }
            json.isJsonObject -> {
                // Extract the _id field from the service object
                json.asJsonObject.get("_id")?.asString
            }
            else -> null
        }
    }
}
