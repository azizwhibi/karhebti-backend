package com.example.karhebti_android.network

// Retrofit interface pour le module Gestion des pannes
import com.example.karhebti_android.data.BreakdownResponse
import com.example.karhebti_android.data.BreakdownsListResponse
import com.example.karhebti_android.data.CreateBreakdownRequest
import retrofit2.Response
import retrofit2.http.*

interface BreakdownsApi {
    /**
     * Créer une nouvelle panne (SOS)
     * POST /breakdowns
     */
    @POST("breakdowns")
    suspend fun createBreakdown(@Body request: CreateBreakdownRequest): BreakdownResponse

    /**
     * Récupérer toutes les pannes
     * GET /breakdowns
     */
    @GET("breakdowns")
    suspend fun getAllBreakdowns(
        @Query("status") status: String? = null,
        @Query("userId") userId: Int? = null
    ): BreakdownsListResponse

    /**
     * Récupérer les pannes d'un utilisateur spécifique
     * GET /breakdowns/user/{userId}
     */
    @GET("breakdowns/user/{userId}")
    suspend fun getUserBreakdowns(@Path("userId") userId: Int): List<BreakdownResponse>

    /**
     * Récupérer une panne spécifique
     * GET /breakdowns/{id}
     */
    @GET("breakdowns/{id}")
    suspend fun getBreakdown(@Path("id") id: Int): BreakdownResponse

    /**
     * Récupérer une panne spécifique avec String ID (MongoDB ObjectId)
     * GET /breakdowns/{id}
     */
    @GET("breakdowns/{id}")
    suspend fun getBreakdownString(@Path("id") id: String): BreakdownResponse

    /**
     * Mettre à jour le statut d'une panne
     * PATCH /breakdowns/{id}
     */
    @PATCH("breakdowns/{id}")
    suspend fun updateStatus(@Path("id") id: Int, @Body status: Map<String, String>): BreakdownResponse

    /**
     * Accepter une demande SOS (garage owner)
     * PUT /breakdowns/{id}/accept
     */
    @PUT("breakdowns/{id}/accept")
    suspend fun acceptBreakdown(@Path("id") id: Int): BreakdownResponse

    /**
     * Accepter une demande SOS avec String ID (MongoDB ObjectId)
     * PUT /breakdowns/{id}/accept
     */
    @PUT("breakdowns/{id}/accept")
    suspend fun acceptBreakdownString(@Path("id") id: String): BreakdownResponse

    /**
     * Refuser une demande SOS (garage owner)
     * PUT /breakdowns/{id}/refuse
     */
    @PUT("breakdowns/{id}/refuse")
    suspend fun refuseBreakdown(
        @Path("id") id: Int,
        @Body reason: Map<String, String>? = null
    ): Response<Map<String, String>>

    /**
     * Refuser une demande SOS avec String ID (MongoDB ObjectId)
     * PUT /breakdowns/{id}/refuse
     */
    @PUT("breakdowns/{id}/refuse")
    suspend fun refuseBreakdownString(
        @Path("id") id: String
    ): Response<Map<String, String>>

    /**
     * Annuler une demande SOS (client)
     * PUT /breakdowns/{id}/cancel
     */
    @PUT("breakdowns/{id}/cancel")
    suspend fun cancelBreakdown(@Path("id") id: Int): BreakdownResponse

    /**
     * Marquer une panne comme terminée (garage owner)
     * PUT /breakdowns/{id}/complete
     */
    @PUT("breakdowns/{id}/complete")
    suspend fun completeBreakdown(@Path("id") id: Int): BreakdownResponse

    /**
     * Assigner un agent/garage à une panne
     * PUT /breakdowns/{id}/assign
     */
    @PUT("breakdowns/{id}/assign")
    suspend fun assignAgent(@Path("id") id: Int, @Body agent: Map<String, Int>): BreakdownResponse

    /**
     * Supprimer une panne
     * DELETE /breakdowns/{id}
     */
    @DELETE("breakdowns/{id}")
    suspend fun deleteBreakdown(@Path("id") id: Int): Response<Void>
}
