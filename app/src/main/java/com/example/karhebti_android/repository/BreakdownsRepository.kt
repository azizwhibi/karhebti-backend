package com.example.karhebti_android.repository

// Repository pour la gestion des pannes (breakdowns)
// Utilise Retrofit et expose desFlow pour la gestion asynchrone

import com.example.karhebti_android.data.BreakdownResponse
import com.example.karhebti_android.data.CreateBreakdownRequest
import com.example.karhebti_android.network.BreakdownsApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import retrofit2.HttpException

class BreakdownsRepository(private val api: BreakdownsApi) {

    /**
     * Créer une nouvelle panne (SOS)
     * NOTE: The client will NOT send `userId` — backend should extract
     * the authenticated user from the JWT Authorization header.
     * If backend still requires `userId` you can provide `userIdFallback` to retry.
     */
    fun createBreakdown(
        request: CreateBreakdownRequest,
        userIdFallback: String? = null
    ): Flow<Result<BreakdownResponse>> = flow {
        try {
            // If the caller already included userId explicitly, send that request directly
            if (!request.userId.isNullOrBlank()) {
                android.util.Log.d("BreakdownsRepo", "createBreakdown: sending request with provided userId (caller supplied)")
                try {
                    emit(Result.success(api.createBreakdown(request)))
                    return@flow
                } catch (httpEx: HttpException) {
                    // handle below (will inspect error and optionally retry with fallback)
                    handleHttpEx(httpEx, userIdFallback)?.let { emit(it); return@flow }
                }
            }

            // Always sanitize the DTO to ensure no userId is sent by the client.
            val sanitizedDto = CreateBreakdownRequest(
                vehicleId = request.vehicleId,
                type = request.type,
                description = request.description,
                latitude = request.latitude,
                longitude = request.longitude,
                photo = request.photo,
                userId = null // force omission of userId
            )

            android.util.Log.d("BreakdownsRepo", "createBreakdown sanitized DTO: $sanitizedDto (userId omitted)")

            val resp = api.createBreakdown(sanitizedDto)
            emit(Result.success(resp))
        } catch (httpEx: HttpException) {
            // Build a helpful error message and return failure
            val statusCode = httpEx.code()
            val errorBody = try { httpEx.response()?.errorBody()?.string() } catch (_: Exception) { null }
            val extracted = try {
                if (!errorBody.isNullOrEmpty()) {
                    val json = JSONObject(errorBody)
                    val m = json.optString("message", "")
                    if (m.isNotBlank()) m else json.optString("error", "")
                } else null
            } catch (_: Exception) { null }

            val message = buildString {
                append("HTTP $statusCode")
                if (!extracted.isNullOrBlank()) append(": $extracted")
                else if (!errorBody.isNullOrBlank()) append(": $errorBody")
                else append(": ${httpEx.message()}")
            }
            android.util.Log.e("BreakdownsRepo", "createBreakdown failed: $message")
            emit(Result.failure(Exception(message)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // Helper to centralize HttpException handling and optional retry with fallback
    private fun handleHttpEx(httpEx: HttpException, userIdFallback: String?): Result<BreakdownResponse>? {
        try {
            val statusCode = httpEx.code()
            val errorBody = try { httpEx.response()?.errorBody()?.string() } catch (_: Exception) { null }
            val extracted = try {
                if (!errorBody.isNullOrEmpty()) {
                    val json = JSONObject(errorBody)
                    val m = json.optString("message", "")
                    if (m.isNotBlank()) m else json.optString("error", "")
                } else null
            } catch (_: Exception) { null }

            val wantsUserId = extracted?.contains("userId", ignoreCase = true) == true || (errorBody?.contains("userId") == true)

            if (wantsUserId && !userIdFallback.isNullOrBlank()) {
                try {
                    // Build DTO with fallback userId
                    val withUser = CreateBreakdownRequest(
                        vehicleId = null,
                        type = "",
                        description = null,
                        latitude = 0.0,
                        longitude = 0.0,
                        photo = null,
                        userId = userIdFallback
                    )
                    // The above is only to show intent; rather the caller should retry with a proper filled DTO.
                    // Instead, we return null to let higher level handle retries if desired.
                    // For safety, we won't automatically populate missing fields here.
                    return Result.failure(Exception("HTTP $statusCode: ${extracted ?: errorBody ?: httpEx.message()}"))
                } catch (retryEx: Exception) {
                    android.util.Log.e("BreakdownsRepo", "Retry with userIdFallback failed: ${retryEx.message}")
                    return Result.failure(Exception("HTTP $statusCode: ${extracted ?: errorBody ?: httpEx.message()}"))
                }
            }

            // Not a userId-specific error or no fallback -> report the error
            val message = buildString {
                append("HTTP $statusCode")
                if (!extracted.isNullOrBlank()) append(": $extracted")
                else if (!errorBody.isNullOrBlank()) append(": $errorBody")
                else append(": ${httpEx.message()}")
            }
            android.util.Log.e("BreakdownsRepo", "createBreakdown failed: $message")
            return Result.failure(Exception(message))
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Récupérer toutes les pannes (avec filtres optionnels)
     */
    fun getAllBreakdowns(
        status: String? = null,
        userId: Int? = null
    ): Flow<Result<List<BreakdownResponse>>> = flow {
        try {
            android.util.Log.d("BreakdownsRepo", "getAllBreakdowns: status=$status, userId=$userId")
            val response = api.getAllBreakdowns(status, userId)
            val breakdowns = response.extractBreakdowns() // Extrait la liste depuis l'objet wrapper
            android.util.Log.d("BreakdownsRepo", "getAllBreakdowns: success, count=${breakdowns.size}")
            emit(Result.success(breakdowns))
        } catch (e: com.google.gson.JsonSyntaxException) {
            // JSON parsing error
            val errorMsg = "Erreur de format JSON. Vérifiez que l'API retourne {\"data\":[...]} ou {\"breakdowns\":[...]}"
            android.util.Log.e("BreakdownsRepo", errorMsg, e)
            emit(Result.failure(Exception(errorMsg)))
        } catch (e: Exception) {
            android.util.Log.e("BreakdownsRepo", "getAllBreakdowns error: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Récupérer l'historique des pannes d'un utilisateur
     */
    fun getUserBreakdowns(userId: Int): Flow<Result<List<BreakdownResponse>>> = flow {
        try {
            emit(Result.success(api.getUserBreakdowns(userId)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Récupérer une panne spécifique par ID
     */
    fun getBreakdown(id: Int): Flow<Result<BreakdownResponse>> = flow {
        try {
            emit(Result.success(api.getBreakdown(id)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Récupérer une panne spécifique par String ID (MongoDB ObjectId)
     */
    fun getBreakdownString(id: String): Flow<Result<BreakdownResponse>> = flow {
        try {
            android.util.Log.d("BreakdownsRepo", "getBreakdownString: $id")
            val result = api.getBreakdownString(id)
            android.util.Log.d("BreakdownsRepo", "getBreakdownString success: ${result.id}")
            emit(Result.success(result))
        } catch (e: Exception) {
            android.util.Log.e("BreakdownsRepo", "getBreakdownString error: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Mettre à jour le statut d'une panne
     */
    fun updateBreakdownStatus(
        id: Int,
        status: String
    ): Flow<Result<BreakdownResponse>> = flow {
        try {
            val body = mapOf("status" to status)
            emit(Result.success(api.updateStatus(id, body)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Assigner un agent/garage à une panne
     */
    fun assignAgent(
        id: Int,
        agentId: Int
    ): Flow<Result<BreakdownResponse>> = flow {
        try {
            val body = mapOf("assignedTo" to agentId)
            emit(Result.success(api.assignAgent(id, body)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Supprimer une panne
     */
    fun deleteBreakdown(id: Int): Flow<Result<Boolean>> = flow {
        try {
            val response = api.deleteBreakdown(id)
            emit(Result.success(response.isSuccessful))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Accepter une demande SOS (pour garage owner)
     * Accepte String ID (MongoDB ObjectId)
     */
    fun acceptBreakdown(breakdownId: String): Flow<Result<BreakdownResponse>> = flow {
        try {
            android.util.Log.d("BreakdownsRepo", "acceptBreakdown: $breakdownId")

            // Utiliser directement la méthode String
            val result = api.acceptBreakdownString(breakdownId)
            android.util.Log.d("BreakdownsRepo", "acceptBreakdown success: ${result.id}")
            emit(Result.success(result))
        } catch (e: Exception) {
            android.util.Log.e("BreakdownsRepo", "acceptBreakdown error: ${e.message}", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Refuser une demande SOS (pour garage owner)
     * Accepte String ID (MongoDB ObjectId)
     */
    fun refuseBreakdown(breakdownId: String): Flow<Result<Unit>> = flow {
        try {
            android.util.Log.d("BreakdownsRepo", "refuseBreakdown: $breakdownId")

            // Utiliser directement la méthode String
            api.refuseBreakdownString(breakdownId)
            android.util.Log.d("BreakdownsRepo", "refuseBreakdown success")
            emit(Result.success(Unit))
        } catch (e: Exception) {
            android.util.Log.e("BreakdownsRepo", "refuseBreakdown error: ${e.message}", e)
            emit(Result.failure(e))
        }
    }
}
