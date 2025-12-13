package com.example.karhebti_android.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header

/**
 * AuthApiService: Handles authentication-related API calls
 *
 * Endpoints:
 * POST /auth/login - Login with email and motDePasse
 * POST /auth/refresh - Refresh access token using refresh_token (optional)
 * POST /auth/logout - Logout user (optional)
 */
interface AuthApiService {

    /**
     * POST /auth/login
     *
     * Request body: { "email": "...", "motDePasse": "..." }
     *
     * Response (200):
     * {
     *   "success": true,
     *   "message": "Login successful",
     *   "access_token": "eyJ...",
     *   "user": {
     *     "_id": "...",
     *     "email": "...",
     *     "nom": "...",
     *     "prenom": "...",
     *     "role": "user",
     *     "telephone": "..."
     *   }
     * }
     *
     * Response (400):
     * { "success": false, "message": "Invalid credentials" }
     *
     * Response (401):
     * { "success": false, "message": "Unauthorized" }
     */
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    /**
     * POST /auth/logout
     * Logout user (optional - server may just ignore for stateless JWT)
     */
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    /**
     * POST /auth/refresh
     * Refresh access token using refresh_token
     */
    @POST("auth/refresh")
    suspend fun refreshToken(
        @Header("Authorization") token: String
    ): Response<AuthResponse>
}
