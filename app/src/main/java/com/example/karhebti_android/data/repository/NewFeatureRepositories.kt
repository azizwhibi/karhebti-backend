package com.example.karhebti_android.data.newrepo

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.karhebti_android.data.api.*
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.data.database.AppDatabase
import com.example.karhebti_android.data.database.UpcomingMaintenanceEntity
import com.example.karhebti_android.util.ImageUploadValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class CarImageRepository(
    private val apiService: KarhebtiApiService = RetrofitClient.apiService
) {
    companion object {
        private const val TAG = "CarImageRepository"
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5MB
    }

    /**
     * Upload car image with comprehensive validation and error handling
     * @param carId ID of the car
     * @param imageUri URI of the image to upload
     * @param context Android context
     * @return Resource with CarResponse containing updated car with imageUrl
     */
    suspend fun uploadCarImage(
        carId: String,
        imageUri: Uri,
        context: Context
    ): Resource<CarResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting image upload for car: $carId")

            // Verify car ID is not empty
            if (carId.isBlank()) {
                Log.e(TAG, "Car ID is empty or blank!")
                return@withContext Resource.Error<CarResponse>(
                    "ID du véhicule invalide"
                )
            }

            // Step 1: Validate image
            val validationResult = ImageUploadValidator.validateImage(context, imageUri)
            if (!validationResult.isValid) {
                Log.w(TAG, "Image validation failed: ${validationResult.error}")
                return@withContext Resource.Error<CarResponse>(
                    validationResult.error ?: "Image validation failed"
                )
            }

            Log.d(TAG, "Image validated successfully. Size: ${validationResult.fileSize} bytes, MIME: ${validationResult.mimeType}")

            // Step 2: Create temporary file from URI
            val tempFile = try {
                createTempFileFromUri(context, imageUri)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create temp file: ${e.message}")
                return@withContext Resource.Error<CarResponse>(
                    "Impossible de lire le fichier image: ${e.localizedMessage}"
                )
            }

            if (tempFile == null || !ImageUploadValidator.isValidImageFile(tempFile)) {
                Log.e(TAG, "Temp file is invalid or doesn't exist")
                tempFile?.delete()
                return@withContext Resource.Error<CarResponse>(
                    "Fichier image invalide ou corrompu"
                )
            }

            Log.d(TAG, "Temp file created: ${tempFile.absolutePath}, size: ${tempFile.length()}")

            // Step 3: Prepare multipart body with correct MIME type
            val mimeType = validationResult.mimeType ?: "image/jpeg"  // Use validated MIME type or default to JPEG
            Log.d(TAG, "Using MIME type: $mimeType")

            val requestBody = try {
                tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create request body: ${e.message}")
                tempFile.delete()
                return@withContext Resource.Error<CarResponse>(
                    "Erreur de préparation du fichier: ${e.localizedMessage}"
                )
            }

            val multipartBody = MultipartBody.Part.createFormData(
                "image",
                tempFile.name,
                requestBody
            )

            Log.d(TAG, "Multipart body created. Uploading to: POST /cars/$carId/image")

            // Step 4: Upload to API
            val response = try {
                apiService.uploadCarImage(carId, multipartBody)
            } catch (e: Exception) {
                Log.e(TAG, "API call failed: ${e.message}", e)
                tempFile.delete()
                return@withContext Resource.Error<CarResponse>(
                    "Erreur réseau: ${e.localizedMessage}"
                )
            }

            // Step 5: Clean up temp file
            tempFile.delete()
            Log.d(TAG, "Temp file deleted")

            // Step 6: Handle API response
            return@withContext if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Image uploaded successfully! Car ID: ${response.body()!!.id}, Image URL: ${response.body()!!.imageUrl}")
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = when {
                    response.code() == 400 -> "Please enter a valid car image"
                    response.code() == 401 -> "Non authentifié. Veuillez vous reconnecter."
                    response.code() == 403 -> "Accès refusé. Vous n'avez pas les droits d'accès."
                    response.code() == 404 -> {
                        Log.e(TAG, "Route 404 - Le serveur backend n'a pas l'endpoint /cars/$carId/image")
                        "L'endpoint d'upload n'est pas disponible sur le serveur. Contactez l'administrateur."
                    }
                    response.code() == 413 -> "Fichier trop volumineux (max 5MB)"
                    response.code() == 415 -> "Type de fichier non supporté"
                    else -> "Erreur serveur: ${response.code()} - ${response.message()}"
                }
                Log.e(TAG, "API returned error: $errorMsg. Body: $errorBody")
                Resource.Error<CarResponse>(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during upload: ${e.message}", e)
            Resource.Error<CarResponse>(
                "Erreur inattendue: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Create a temporary file from a URI content
     */
    private suspend fun createTempFileFromUri(context: Context, uri: Uri): File? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: run {
                        Log.e(TAG, "Cannot open input stream for URI: $uri")
                        return@withContext null
                    }

                val tempFile = File(context.cacheDir, "car_image_${System.currentTimeMillis()}.jpg")

                inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempFile.length() > MAX_FILE_SIZE) {
                    Log.e(TAG, "Temp file exceeds max size: ${tempFile.length()}")
                    tempFile.delete()
                    return@withContext null
                }

                tempFile
            } catch (e: Exception) {
                Log.e(TAG, "Error creating temp file from URI: ${e.message}", e)
                null
            }
        }

    /**
     * Deletes a car's image by updating the car with null imageUrl
     * Note: This is a convenience method; actual deletion may need backend support
     */
    suspend fun deleteCarImage(carId: String): Resource<CarResponse> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting image for car: $carId")
                val request = UpdateCarRequest(imageUrl = null)
                val response = apiService.updateCar(carId, request)

                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "Image deleted successfully")
                    Resource.Success(response.body()!!)
                } else {
                    Log.e(TAG, "Failed to delete image: ${response.message()}")
                    Resource.Error("Erreur lors de la suppression de l'image: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting image: ${e.message}", e)
                Resource.Error("Erreur: ${e.localizedMessage}")
            }
        }
}

class MaintenanceRepositoryImpl(
    private val apiService: KarhebtiApiService = RetrofitClient.apiService,
    private val database: AppDatabase
) {
    constructor(context: Context) : this(
        RetrofitClient.apiService,
        AppDatabase.getInstance(context)
    )

    private val dao = database.upcomingMaintenanceDao()

    suspend fun searchMaintenances(
        search: String? = null,
        status: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        tags: List<String>? = null,
        minCost: Double? = null,
        maxCost: Double? = null,
        minMileage: Int? = null,
        maxMileage: Int? = null,
        sort: String? = "dueAt",
        order: String? = "asc",
        page: Int = 1,
        limit: Int = 20
    ): Resource<PaginatedMaintenancesResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchMaintenances(
                search, status, dateFrom, dateTo, tags,
                minCost, maxCost, minMileage, maxMileage,
                sort, order, page, limit
            )

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Failed to search maintenances: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Error searching maintenances: ${e.localizedMessage}")
        }
    }

    suspend fun getUpcomingMaintenances(
        limit: Int = 5,
        includePlate: Boolean = true
    ): Resource<List<UpcomingMaintenanceWidget>> = withContext(Dispatchers.IO) {
        try {
            // Fetch from API
            val response = apiService.getUpcomingMaintenances(limit, includePlate)

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!

                // Cache in Room
                val entities = data.map {
                    UpcomingMaintenanceEntity(
                        id = it.id,
                        title = it.title,
                        voitureId = it.voiture,
                        plate = it.plate,
                        dueAt = it.dueAt,
                        status = it.status,
                        lastUpdated = Date()
                    )
                }
                dao.deleteAll()
                dao.insertAll(entities)

                Resource.Success(data)
            } else {
                // Return cached data on error
                val cached = dao.getUpcomingMaintenancesList(limit)
                if (cached.isNotEmpty()) {
                    val widgets = cached.map {
                        UpcomingMaintenanceWidget(
                            id = it.id,
                            title = it.title,
                            voiture = it.voitureId,
                            plate = it.plate,
                            dueAt = it.dueAt,
                            status = it.status
                        )
                    }
                    Resource.Success(widgets)
                } else {
                    Resource.Error("Failed to fetch maintenances: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            // Return cached data on exception
            try {
                val cached = dao.getUpcomingMaintenancesList(limit)
                if (cached.isNotEmpty()) {
                    val widgets = cached.map {
                        UpcomingMaintenanceWidget(
                            id = it.id,
                            title = it.title,
                            voiture = it.voitureId,
                            plate = it.plate,
                            dueAt = it.dueAt,
                            status = it.status
                        )
                    }
                    Resource.Success(widgets)
                } else {
                    Resource.Error("Error fetching maintenances: ${e.localizedMessage}")
                }
            } catch (dbError: Exception) {
                Resource.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    fun getUpcomingMaintenancesFlow(limit: Int = 5): Flow<List<UpcomingMaintenanceEntity>> {
        return dao.getUpcomingMaintenances(limit)
    }
}

class EmailVerificationRepository(
    private val apiService: KarhebtiApiService = RetrofitClient.apiService
) {
    suspend fun sendVerificationCode(email: String): Resource<EmailVerificationResponse> =
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("EmailVerification", "Sending verification code to: $email")
                val request = SendEmailVerificationRequest(email)
                val response = apiService.sendEmailVerification(request)

                android.util.Log.d("EmailVerification", "Response code: ${response.code()}")
                android.util.Log.d("EmailVerification", "Response message: ${response.message()}")

                if (response.isSuccessful && response.body() != null) {
                    android.util.Log.d("EmailVerification", "Verification code sent successfully")
                    Resource.Success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("EmailVerification", "Error: ${response.code()} - $errorBody")
                    Resource.Error("Failed to send verification code: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("EmailVerification", "Exception: ${e.message}", e)
                Resource.Error("Error: ${e.localizedMessage}")
            }
        }

    suspend fun verifyEmail(email: String, code: String): Resource<EmailVerificationResponse> =
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("EmailVerification", "Verifying email: $email with code: $code")
                val request = VerifyEmailRequest(email, code)
                val response = apiService.verifyEmail(request)

                android.util.Log.d("EmailVerification", "Verify response code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    android.util.Log.d("EmailVerification", "Email verified successfully")
                    Resource.Success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("EmailVerification", "Verify error: ${response.code()} - $errorBody")
                    Resource.Error("Invalid or expired verification code: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("EmailVerification", "Verify exception: ${e.message}", e)
                Resource.Error("Error: ${e.localizedMessage}")
            }
        }
}

class OtpLoginRepository(
    private val apiService: KarhebtiApiService = RetrofitClient.apiService
) {
    suspend fun sendOtpCode(identifier: String): Resource<OtpResponse> =
        withContext(Dispatchers.IO) {
            try {
                val request = SendOtpLoginRequest(identifier)
                val response = apiService.sendOtpLogin(request)

                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(response.body()!!)
                } else {
                    Resource.Error("Failed to send OTP: ${response.message()}")
                }
            } catch (e: Exception) {
                Resource.Error("Error: ${e.localizedMessage}")
            }
        }

    suspend fun verifyOtpLogin(identifier: String, code: String): Resource<AuthResponse> =
        withContext(Dispatchers.IO) {
            try {
                val request = VerifyOtpLoginRequest(identifier, code)
                val response = apiService.verifyOtpLogin(request)

                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(response.body()!!)
                } else {
                    Resource.Error("Invalid or expired OTP code")
                }
            } catch (e: Exception) {
                Resource.Error("Error: ${e.localizedMessage}")
            }
        }
}
