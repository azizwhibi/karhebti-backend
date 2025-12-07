package com.example.karhebti_android.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import okhttp3.RequestBody

interface KarhebtiApiService {

    // ==================== AUTH ====================

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<MessageResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<MessageResponse>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<MessageResponse>

    // ==================== NEW: OTP LOGIN ====================

    @POST("auth/otp/send")
    suspend fun sendOtpLogin(@Body request: SendOtpLoginRequest): Response<OtpResponse>

    @POST("auth/otp/verify")
    suspend fun verifyOtpLogin(@Body request: VerifyOtpLoginRequest): Response<AuthResponse>

    // ==================== NEW: EMAIL VERIFICATION ====================

    @POST("auth/email/send")
    suspend fun sendEmailVerification(@Body request: SendEmailVerificationRequest): Response<EmailVerificationResponse>

    @POST("auth/email/verify")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<EmailVerificationResponse>

    // ==================== NEW: SIGNUP VERIFY ====================

    @POST("auth/signup/verify")
    suspend fun verifySignupOtp(@Body request: VerifySignupOtpRequest): Response<AuthResponse>

    // ==================== USERS ====================

    @GET("users")
    suspend fun getAllUsers(): Response<List<UserResponse>>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): Response<UserResponse>

    @POST("users")
    suspend fun createUser(@Body request: SignupRequest): Response<UserResponse>

    @PATCH("users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body request: UpdateUserRequest
    ): Response<UserResponse>

    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Response<MessageResponse>

    @PATCH("users/{id}/role")
    suspend fun updateUserRole(
        @Path("id") id: String,
        @Body request: UpdateRoleRequest
    ): Response<UserResponse>

    // ==================== CARS ====================

    @GET("cars")
    suspend fun getMyCars(): Response<List<CarResponse>>

    @GET("cars/{id}")
    suspend fun getCar(@Path("id") id: String): Response<CarResponse>

    @POST("cars")
    suspend fun createCar(@Body request: CreateCarRequest): Response<CarResponse>

    @PATCH("cars/{id}")
    suspend fun updateCar(
        @Path("id") id: String,
        @Body request: UpdateCarRequest
    ): Response<CarResponse>

    @DELETE("cars/{id}")
    suspend fun deleteCar(@Path("id") id: String): Response<MessageResponse>

    // ==================== NEW: CAR IMAGE UPLOAD ====================

    @Multipart
    @POST("cars/{id}/image")
    suspend fun uploadCarImage(
        @Path("id") id: String,
        @Part image: MultipartBody.Part
    ): Response<CarResponse>

    // ==================== MAINTENANCES ====================

    @GET("maintenances")
    suspend fun getMaintenances(): Response<List<MaintenanceResponse>>

    @GET("maintenances/{id}")
    suspend fun getMaintenance(@Path("id") id: String): Response<MaintenanceResponse>

    @POST("maintenances")
    suspend fun createMaintenance(@Body request: CreateMaintenanceRequest): Response<MaintenanceResponse>

    @PATCH("maintenances/{id}")
    suspend fun updateMaintenance(
        @Path("id") id: String,
        @Body request: UpdateMaintenanceRequest
    ): Response<MaintenanceResponse>

    @DELETE("maintenances/{id}")
    suspend fun deleteMaintenance(@Path("id") id: String): Response<MessageResponse>

    // ==================== NEW: MAINTENANCES SEARCH/FILTER ====================

    @GET("maintenances/search/filter")
    suspend fun searchMaintenances(
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("tags[]") tags: List<String>? = null,
        @Query("minCost") minCost: Double? = null,
        @Query("maxCost") maxCost: Double? = null,
        @Query("minMileage") minMileage: Int? = null,
        @Query("maxMileage") maxMileage: Int? = null,
        @Query("sort") sort: String? = "dueAt",
        @Query("order") order: String? = "asc",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PaginatedMaintenancesResponse>

    // ==================== NEW: UPCOMING MAINTENANCES WIDGET ====================

    @GET("maintenances/upcoming/widget")
    suspend fun getUpcomingMaintenances(
        @Query("limit") limit: Int = 5,
        @Query("includePlate") includePlate: Boolean = true
    ): Response<List<UpcomingMaintenanceWidget>>

// ==================== GARAGES ====================

    @GET("garages")
    suspend fun getGarages(): Response<List<GarageResponse>>

    @GET("garages/{id}")
    suspend fun getGarage(@Path("id") id: String): Response<GarageResponse>

    @POST("garages")
    suspend fun createGarage(@Body request: CreateGarageRequest): Response<CreateGarageResponse>

    @PATCH("garages/{id}")
    suspend fun updateGarage(
        @Path("id") id: String,
        @Body request: UpdateGarageRequest
    ): Response<GarageResponse>

    @DELETE("garages/{id}")
    suspend fun deleteGarage(@Path("id") id: String): Response<MessageResponse>
    // ==================== DOCUMENTS ====================

    @GET("documents")
    suspend fun getDocuments(): Response<List<DocumentResponse>>

    @GET("documents/{id}")
    suspend fun getDocument(@Path("id") id: String): Response<DocumentResponse>

    @POST("documents")
    suspend fun createDocument(@Body request: CreateDocumentRequest): Response<DocumentResponse>

    @Multipart
    @POST("documents")
    suspend fun createDocumentMultipart(
        @Part("type") type: RequestBody,
        @Part("dateEmission") dateEmission: RequestBody,
        @Part("dateExpiration") dateExpiration: RequestBody,
        @Part("voiture") voiture: RequestBody,
        @Part fichier: MultipartBody.Part? = null
    ): Response<DocumentResponse>

    @PATCH("documents/{id}")
    suspend fun updateDocument(
        @Path("id") id: String,
        @Body request: UpdateDocumentRequest
    ): Response<DocumentResponse>

    @Multipart
    @PATCH("documents/{id}")
    suspend fun updateDocumentMultipart(
        @Path("id") id: String,
        @Part("type") type: RequestBody,
        @Part("dateEmission") dateEmission: RequestBody,
        @Part("dateExpiration") dateExpiration: RequestBody,
        @Part("voiture") voiture: RequestBody? = null,
        @Part fichier: MultipartBody.Part? = null
    ): Response<DocumentResponse>

    @DELETE("documents/{id}")
    suspend fun deleteDocument(@Path("id") id: String): Response<MessageResponse>

    // OCR Document endpoint
    @Multipart
    @POST("documents/ocr")
    suspend fun ocrDocument(
        @Part file: MultipartBody.Part,
        @Part("typeHint") typeHint: RequestBody? = null
    ): Response<OcrDocumentResponse>

    // ==================== PARTS ====================

    @GET("parts")
    suspend fun getParts(): Response<List<PartResponse>>

    @GET("parts/{id}")
    suspend fun getPart(@Path("id") id: String): Response<PartResponse>

    @POST("parts")
    suspend fun createPart(@Body request: CreatePartRequest): Response<PartResponse>

    @PATCH("parts/{id}")
    suspend fun updatePart(
        @Path("id") id: String,
        @Body request: CreatePartRequest
    ): Response<PartResponse>

    @DELETE("parts/{id}")
    suspend fun deletePart(@Path("id") id: String): Response<MessageResponse>

    // ==================== SERVICES ====================

    @GET("services")
    suspend fun getServices(): Response<List<ServiceResponse>>

    @GET("services/{id}")
    suspend fun getService(@Path("id") id: String): Response<ServiceResponse>

    @GET("services/garage/{garageId}")
    suspend fun getServicesByGarage(@Path("garageId") garageId: String): Response<List<ServiceResponse>>

    @POST("services")
    suspend fun createService(@Body request: CreateServiceRequest): Response<ServiceResponse>

    @PATCH("services/{id}")
    suspend fun updateService(
        @Path("id") id: String,
        @Body request: UpdateServiceRequest
    ): Response<ServiceResponse>

    @DELETE("services/{id}")
    suspend fun deleteService(@Path("id") id: String): Response<MessageResponse>

    // ==================== RECLAMATIONS (FEEDBACK) ====================

    @GET("reclamations")
    suspend fun getReclamations(): Response<List<ReclamationResponse>>

    @GET("reclamations/{id}")
    suspend fun getReclamation(@Path("id") id: String): Response<ReclamationResponse>

    @GET("reclamations/garage/{garageId}")
    suspend fun getReclamationsByGarage(@Path("garageId") garageId: String): Response<List<ReclamationResponse>>

    @GET("reclamations/service/{serviceId}")
    suspend fun getReclamationsByService(@Path("serviceId") serviceId: String): Response<List<ReclamationResponse>>

    @POST("reclamations")
    suspend fun createReclamation(@Body request: CreateReclamationRequest): Response<ReclamationResponse>

    @PATCH("reclamations/{id}")
    suspend fun updateReclamation(
        @Path("id") id: String,
        @Body request: UpdateReclamationRequest
    ): Response<ReclamationResponse>

    @DELETE("reclamations/{id}")
    suspend fun deleteReclamation(@Path("id") id: String): Response<MessageResponse>

    // ==================== AI FEATURES ====================

    @POST("ai/report-road-issue")
    suspend fun reportRoadIssue(@Body request: ReportRoadIssueRequest): Response<RoadIssueResponse>

    @GET("ai/danger-zones")
    suspend fun getDangerZones(
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("rayon") rayon: Double? = null
    ): Response<List<DangerZone>>

    @POST("ai/maintenance-recommendations")
    suspend fun getMaintenanceRecommendations(
        @Body request: MaintenanceRecommendationRequest
    ): Response<MaintenanceRecommendationResponse>

    @GET("ai/garage-recommendation")
    suspend fun getGarageRecommendations(
        @Query("typePanne") typePanne: String? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("rayon") rayon: Double? = null
    ): Response<List<GarageRecommendation>>

    // ==================== TRANSLATION API ====================

    @POST("api/translation/translate")
    suspend fun translateText(@Body request: TranslateRequest): Response<TranslateResponse>

    @POST("api/translation/batch")
    suspend fun batchTranslate(@Body request: BatchTranslateRequest): Response<BatchTranslateResponse>

    @GET("api/translation/languages")
    suspend fun getLanguages(): Response<LanguagesResponse>

    @GET("api/translation/cached/{languageCode}")
    suspend fun getCachedTranslations(
        @Path("languageCode") languageCode: String
    ): Response<CachedTranslationsResponse>

    @DELETE("api/translation/cache")
    suspend fun clearTranslationCache(
        @Query("languageCode") languageCode: String? = null
    ): Response<MessageResponse>

    // ==================== MARKETPLACE / SWIPE FEATURE ====================

    // Cars - Marketplace
    @GET("cars/marketplace/available")
    suspend fun getAvailableCars(): Response<List<MarketplaceCarResponse>>

    @POST("cars/{id}/list-for-sale")
    suspend fun listCarForSale(
        @Path("id") id: String,
        @Body request: ListCarForSaleRequest
    ): Response<MarketplaceCarResponse>

    @POST("cars/{id}/unlist")
    suspend fun unlistCar(@Path("id") id: String): Response<MarketplaceCarResponse>

    // Swipes
    @POST("swipes")
    suspend fun createSwipe(@Body request: CreateSwipeRequest): Response<SwipeResponse>

    @POST("swipes/{id}/accept")
    suspend fun acceptSwipe(@Path("id") id: String): Response<SwipeStatusResponse>

    @POST("swipes/{id}/decline")
    suspend fun declineSwipe(@Path("id") id: String): Response<SwipeStatusResponse>

    @GET("swipes/my-swipes")
    suspend fun getMySwipes(): Response<MySwipesResponse>

    @GET("swipes/pending")
    suspend fun getPendingSwipes(): Response<List<SwipeResponse>>

    // Conversations
    @GET("conversations")
    suspend fun getConversations(): Response<List<ConversationResponse>>

    @GET("conversations/{id}")
    suspend fun getConversation(@Path("id") id: String): Response<ConversationResponse>

    @GET("conversations/{id}/messages")
    suspend fun getMessages(@Path("id") id: String): Response<List<ChatMessage>>

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") id: String,
        @Body request: SendMessageRequest
    ): Response<ChatMessage>

    @POST("conversations/{id}/mark-read")
    suspend fun markConversationAsRead(@Path("id") id: String): Response<MessageResponse>

    // ==================== NOTIFICATIONS ====================

    @GET("notifications")
    suspend fun getNotifications(): Response<List<NotificationResponse>>

    @GET("notifications/mes-notifications")
    suspend fun getMyNotifications(): Response<List<NotificationResponse>>

    @GET("notifications/non-lues")
    suspend fun getUnreadNotifications(): Response<List<NotificationResponse>>

    @PATCH("notifications/{id}/marquer-lu")
    suspend fun markNotificationAsRead(@Path("id") id: String): Response<NotificationResponse>

    @PATCH("notifications/marquer-toutes-lues")
    suspend fun markAllNotificationsAsRead(): Response<MessageResponse>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: String): Response<MessageResponse>

    // ==================== RESERVATIONS ====================

    @GET("reservations")
    suspend fun getReservations(
        @Query("userId") userId: String? = null,
        @Query("garageId") garageId: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<ReservationListResponse>

    @GET("reservations/me")
    suspend fun getMyReservations(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<ReservationListResponse>

    @GET("reservations/{id}")
    suspend fun getReservation(@Path("id") id: String): Response<ReservationResponse>

    @POST("reservations")
    suspend fun createReservation(@Body request: CreateReservationRequest): Response<ReservationResponse>

    @PATCH("reservations/{id}")
    suspend fun updateReservation(
        @Path("id") id: String,
        @Body request: UpdateReservationRequest
    ): Response<ReservationResponse>

    @DELETE("reservations/{id}")
    suspend fun deleteReservation(@Path("id") id: String): Response<MessageResponse>

    @PATCH("reservations/{id}/status")
    suspend fun updateReservationStatus(
        @Path("id") id: String,
        @Body request: UpdateReservationStatusRequest
    ): Response<ReservationResponse>

    @GET("reservations/garage/{garageId}")
    suspend fun getGarageReservations(
        @Path("garageId") garageId: String,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<ReservationListResponse>

    // ==================== OSM ====================

    @GET("osm/search")
    suspend fun searchAddress(
        @Query("query") query: String
    ): Response<List<LocationSuggestion>>

    @GET("osm/reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Response<LocationSuggestion>

    // ==================== REPAIR BAYS ====================

    @GET("repair-bays/garage/{garageId}")
    suspend fun getRepairBaysByGarage(
        @Path("garageId") garageId: String
    ): Response<List<RepairBayResponse>>

    @GET("repair-bays/garage/{garageId}/available")
    suspend fun getAvailableRepairBays(
        @Path("garageId") garageId: String,
        @Query("date") date: String,
        @Query("heureDebut") heureDebut: String,
        @Query("heureFin") heureFin: String
    ): Response<List<RepairBayResponse>>

    @POST("repair-bays")
    suspend fun createRepairBay(
        @Body request: CreateRepairBayRequest
    ): Response<RepairBayResponse>

    @PATCH("repair-bays/{id}")
    suspend fun updateRepairBay(
        @Path("id") id: String,
        @Body request: CreateRepairBayRequest
    ): Response<RepairBayResponse>

    @DELETE("repair-bays/{id}")
    suspend fun deleteRepairBay(
        @Path("id") id: String
    ): Response<MessageResponse>
}