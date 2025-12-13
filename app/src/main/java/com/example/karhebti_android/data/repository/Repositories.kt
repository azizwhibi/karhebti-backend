package com.example.karhebti_android.data.repository

import com.example.karhebti_android.data.api.*
import com.example.karhebti_android.data.preferences.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T> : Resource<T>()
}

// ==================== CAR REPOSITORY ====================

class CarRepository(private val apiService: KarhebtiApiService = RetrofitClient.apiService) {

    suspend fun getMyCars(): Resource<List<CarResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMyCars()

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération des voitures")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun createCar(
        marque: String,
        modele: String,
        annee: Int,
        immatriculation: String,
        typeCarburant: String,
        kilometrage: Int? = null
    ): Resource<CarResponse> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("CarRepository", "Creating car: $marque $modele $annee $immatriculation $typeCarburant")
            // Create car without kilometrage first
            val request = CreateCarRequest(marque, modele, annee, immatriculation, typeCarburant)
            val response = apiService.createCar(request)

            if (response.isSuccessful && response.body() != null) {
                val createdCar = response.body()!!
                android.util.Log.d("CarRepository", "Success: Car created - $createdCar")

                // If kilometrage was provided, update the car with it
                if (kilometrage != null && kilometrage > 0) {
                    android.util.Log.d("CarRepository", "Updating car with kilometrage: $kilometrage")
                    val updateRequest = UpdateCarRequest(kilometrage = kilometrage)
                    val updateResponse = apiService.updateCar(createdCar.id, updateRequest)

                    if (updateResponse.isSuccessful && updateResponse.body() != null) {
                        android.util.Log.d("CarRepository", "Success: Car updated with kilometrage")
                        Resource.Success(updateResponse.body()!!)
                    } else {
                        // Car was created but kilometrage update failed - still return success
                        android.util.Log.w("CarRepository", "Car created but kilometrage update failed")
                        Resource.Success(createdCar)
                    }
                } else {
                    Resource.Success(createdCar)
                }
            } else {
                Resource.Error("Erreur lors de la création")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun updateCar(
        id: String,
        marque: String? = null,
        modele: String? = null,
        annee: Int? = null,
        typeCarburant: String? = null,
        kilometrage: Int? = null,
        statut: String? = null,
        prochainEntretien: String? = null,
        joursProchainEntretien: Int? = null,
        imageUrl: String? = null
    ): Resource<CarResponse> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("CarRepository", "Updating car: $id")
            val request = UpdateCarRequest(
                marque, modele, annee, typeCarburant,
                kilometrage, statut, prochainEntretien, joursProchainEntretien, imageUrl
            )
            val response = apiService.updateCar(id, request)

            android.util.Log.d("CarRepository", "Update response code: ${response.code()}")
            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("CarRepository", "Car updated successfully")
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Erreur lors de la modification: ${response.code()} - $errorBody"
                android.util.Log.e("CarRepository", errorMsg)
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Erreur réseau: ${e.message}"
            android.util.Log.e("CarRepository", errorMsg, e)
            Resource.Error(errorMsg)
        }
    }

    suspend fun deleteCar(id: String): Resource<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("CarRepository", "Deleting car: $id")
            val response = apiService.deleteCar(id)

            android.util.Log.d("CarRepository", "Delete response code: ${response.code()}")
            
            // Backend returns empty body (Unit/Void), so we just check if successful
            if (response.isSuccessful) {
                android.util.Log.d("CarRepository", "Car deleted successfully")
                // Create a success message for the UI
                val successMessage = MessageResponse(message = "Véhicule supprimé avec succès")
                Resource.Success(successMessage)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Erreur lors de la suppression: ${response.code()} - $errorBody"
                android.util.Log.e("CarRepository", errorMsg)
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Erreur réseau: ${e.message}"
            android.util.Log.e("CarRepository", errorMsg, e)
            Resource.Error(errorMsg)
        }
    }
}

// ==================== MAINTENANCE REPOSITORY ====================

class MaintenanceRepository(private val apiService: KarhebtiApiService = RetrofitClient.apiService) {

    suspend fun getMaintenances(): Resource<List<MaintenanceResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMaintenances()

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération des entretiens")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun getMaintenanceById(id: String): Resource<MaintenanceResponse> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("MaintenanceRepository", "Fetching maintenance by ID: $id")
            val response = apiService.getMaintenance(id)
            android.util.Log.d("MaintenanceRepository", "Response code: ${response.code()}")
            android.util.Log.d("MaintenanceRepository", "Response body: ${response.body()}")

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("MaintenanceRepository", "Successfully fetched maintenance")
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Erreur: ${response.code()} - ${response.message()} - $errorBody"
                android.util.Log.e("MaintenanceRepository", errorMsg)
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Erreur réseau: ${e.message}"
            android.util.Log.e("MaintenanceRepository", errorMsg, e)
            e.printStackTrace()
            Resource.Error(errorMsg)
        }
    }

    suspend fun createMaintenance(
        type: String,
        date: String,
        cout: Double,
        garage: String,
        voiture: String
    ): Resource<MaintenanceResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CreateMaintenanceRequest(type = type, title = type, date, dueAt = date, cout, garage, voiture)
            val response = apiService.createMaintenance(request)

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la création de l'entretien")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun updateMaintenance(id: String, request: UpdateMaintenanceRequest): Resource<MaintenanceResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateMaintenance(id, request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la mise à jour")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun deleteMaintenance(id: String): Resource<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteMaintenance(id)

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la suppression")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }
}

// ==================== DOCUMENT REPOSITORY ====================

class DocumentRepository(private val apiService: KarhebtiApiService = RetrofitClient.apiService) {

    suspend fun getDocuments(): Resource<List<DocumentResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDocuments()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération des documents")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun getDocumentById(id: String): Resource<DocumentResponse> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("DocumentRepository", "=== Getting document by ID ===")
            android.util.Log.d("DocumentRepository", "Document ID: $id")

            // WORKAROUND pour erreur 500 du backend
            // Récupérer tous les documents et filtrer celui qu'on veut
            android.util.Log.d("DocumentRepository", "Using workaround: getting all documents and filtering")

            val response = apiService.getDocuments()

            android.util.Log.d("DocumentRepository", "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val documents = response.body()!!
                android.util.Log.d("DocumentRepository", "Total documents retrieved: ${documents.size}")

                val document = documents.find { it.id == id }

                if (document != null) {
                    android.util.Log.d("DocumentRepository", "Document found: ${document.type}")
                    Resource.Success(document)
                } else {
                    val errorMsg = "Document avec ID $id non trouvé dans la liste"
                    android.util.Log.e("DocumentRepository", errorMsg)
                    Resource.Error(errorMsg)
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Erreur ${response.code()}: ${errorBody ?: "Impossible de récupérer les documents"}"
                android.util.Log.e("DocumentRepository", "ERROR: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Erreur réseau: ${e.message}"
            android.util.Log.e("DocumentRepository", errorMsg, e)
            e.printStackTrace()
            Resource.Error(errorMsg)
        }
    }

    suspend fun createDocument(request: CreateDocumentRequest, filePath: String? = null): Resource<DocumentResponse> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("DocumentRepository", "=== Creating document ===")
            android.util.Log.d("DocumentRepository", "Type: ${request.type}")
            android.util.Log.d("DocumentRepository", "DateEmission: ${request.dateEmission}")
            android.util.Log.d("DocumentRepository", "DateExpiration: ${request.dateExpiration}")
            android.util.Log.d("DocumentRepository", "Voiture: ${request.voiture}")
            android.util.Log.d("DocumentRepository", "Fichier: ${request.fichier}")
            android.util.Log.d("DocumentRepository", "FilePath: $filePath")

            // Pour l'instant, utiliser l'endpoint JSON normal
            // TODO: Utiliser multipart quand le backend sera configuré
            if (filePath.isNullOrBlank()) {
                android.util.Log.d("DocumentRepository", "Creating document without file")
            } else {
                android.util.Log.d("DocumentRepository", "Creating document with file: $filePath (stored locally)")
                val file = File(filePath)
                if (file.exists()) {
                    android.util.Log.d("DocumentRepository", "File size: ${file.length()} bytes")
                } else {
                    android.util.Log.e("DocumentRepository", "File does not exist: $filePath")
                }
            }

            // Utiliser l'endpoint JSON normal avec le chemin du fichier
            val response = apiService.createDocument(request)

            android.util.Log.d("DocumentRepository", "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("DocumentRepository", "Document created successfully")
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Erreur ${response.code()}: ${errorBody ?: "Inconnue"}"
                android.util.Log.e("DocumentRepository", "ERROR DETAILS: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Erreur réseau: ${e.message}"
            android.util.Log.e("DocumentRepository", errorMsg, e)
            e.printStackTrace()
            Resource.Error(errorMsg)
        }
    }

    suspend fun updateDocument(id: String, request: UpdateDocumentRequest, filePath: String? = null): Resource<DocumentResponse> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("DocumentRepository", "=== Updating document ===")
            android.util.Log.d("DocumentRepository", "Document ID: $id")
            android.util.Log.d("DocumentRepository", "Type: ${request.type}")
            android.util.Log.d("DocumentRepository", "DateEmission: ${request.dateEmission}")
            android.util.Log.d("DocumentRepository", "DateExpiration: ${request.dateExpiration}")
            android.util.Log.d("DocumentRepository", "FilePath: $filePath")

            // Utiliser l'endpoint JSON normal (comme pour createDocument)
            // TODO: Utiliser multipart quand le backend sera configuré
            if (!filePath.isNullOrBlank()) {
                val file = File(filePath)
                if (file.exists()) {
                    android.util.Log.d("DocumentRepository", "File size: ${file.length()} bytes (stored locally)")
                } else {
                    android.util.Log.e("DocumentRepository", "File does not exist: $filePath")
                }
            }

            val response = apiService.updateDocument(id, request)

            android.util.Log.d("DocumentRepository", "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                android.util.Log.d("DocumentRepository", "Document updated successfully")
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Erreur ${response.code()}: ${errorBody ?: "Erreur de mise à jour"}"
                android.util.Log.e("DocumentRepository", "ERROR: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Erreur réseau: ${e.message}"
            android.util.Log.e("DocumentRepository", errorMsg, e)
            e.printStackTrace()
            Resource.Error(errorMsg)
        }
    }

    suspend fun deleteDocument(id: String): Resource<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteDocument(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la suppression du document")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun ocrDocument(filePath: String, typeHint: String? = null): Resource<OcrDocumentData> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("DocumentRepository", "=== OCR Document ===")
            android.util.Log.d("DocumentRepository", "FilePath: $filePath")
            android.util.Log.d("DocumentRepository", "TypeHint: $typeHint")

            val file = File(filePath)
            if (!file.exists()) {
                android.util.Log.e("DocumentRepository", "File does not exist: $filePath")
                return@withContext Resource.Error("Le fichier n'existe pas")
            }

            android.util.Log.d("DocumentRepository", "File size: ${file.length()} bytes")

            // Préparer le multipart body
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val typeHintBody = typeHint?.let {
                it.toRequestBody("text/plain".toMediaTypeOrNull())
            }

            android.util.Log.d("DocumentRepository", "Calling OCR API...")
            val response = apiService.ocrDocument(filePart, typeHintBody)

            android.util.Log.d("DocumentRepository", "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val ocrResponse = response.body()!!
                android.util.Log.d("DocumentRepository", "OCR successful")
                android.util.Log.d("DocumentRepository", "Type detected: ${ocrResponse.data.type}")
                android.util.Log.d("DocumentRepository", "DateEmission: ${ocrResponse.data.dateEmission}")
                android.util.Log.d("DocumentRepository", "DateExpiration: ${ocrResponse.data.dateExpiration}")
                Resource.Success(ocrResponse.data)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = "Erreur OCR ${response.code()}: ${errorBody ?: "Erreur d'extraction"}"
                android.util.Log.e("DocumentRepository", "ERROR: $errorMsg")
                Resource.Error(errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Erreur réseau OCR: ${e.message}"
            android.util.Log.e("DocumentRepository", errorMsg, e)
            e.printStackTrace()
            Resource.Error(errorMsg)
        }
    }
}

// ==================== PART REPOSITORY ====================

class PartRepository(private val apiService: KarhebtiApiService = RetrofitClient.apiService) {

    suspend fun getParts(): Resource<List<PartResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getParts()

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération des pièces")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun createPart(
        nom: String,
        type: String,
        dateInstallation: String,
        kilometrageRecommande: Int,
        voiture: String
    ): Resource<PartResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CreatePartRequest(nom, type, dateInstallation, kilometrageRecommande, voiture)
            val response = apiService.createPart(request)

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la création de la pièce")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun deletePart(id: String): Resource<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deletePart(id)

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la suppression")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }
}

// ==================== AI REPOSITORY ====================

class AIRepository(private val apiService: KarhebtiApiService = RetrofitClient.apiService) {

    suspend fun reportRoadIssue(
        latitude: Double,
        longitude: Double,
        typeAnomalie: String,
        description: String
    ): Resource<RoadIssueResponse> = withContext(Dispatchers.IO) {
        try {
            // ReportRoadIssueRequest(type, description, latitude, longitude)
            val request = ReportRoadIssueRequest(typeAnomalie, description, latitude, longitude)
            val response = apiService.reportRoadIssue(request)

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors du signalement")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun getDangerZones(
        latitude: Double? = null,
        longitude: Double? = null,
        rayon: Double? = null
    ): Resource<List<DangerZone>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getDangerZones(latitude, longitude, rayon)

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération des zones dangereuses")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun getMaintenanceRecommendations(
        voitureId: String,
        mileage: Int,
        lastMaintenanceDate: String? = null
    ): Resource<MaintenanceRecommendationResponse> = withContext(Dispatchers.IO) {
        try {
            val request = MaintenanceRecommendationRequest(
                carId = voitureId,
                mileage = mileage,
                lastMaintenanceDate = lastMaintenanceDate
            )
            val response = apiService.getMaintenanceRecommendations(request)

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération des recommandations")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }
}

// ==================== RECLAMATION REPOSITORY ====================

class ReclamationRepository(private val apiService: KarhebtiApiService = RetrofitClient.apiService) {

    suspend fun getReclamations(): Resource<List<ReclamationResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getReclamations()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération des réclamations")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun getReclamationById(id: String): Resource<ReclamationResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getReclamation(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération de la réclamation")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun getMyReclamations(): Resource<List<ReclamationResponse>> = withContext(Dispatchers.IO) {
        try {
            // Le backend filtre automatiquement par utilisateur connecté via JWT
            val response = apiService.getReclamations()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération de mes réclamations")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun getReclamationsByGarage(garageId: String): Resource<List<ReclamationResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getReclamationsByGarage(garageId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération des réclamations du garage")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun getReclamationsByService(serviceId: String): Resource<List<ReclamationResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getReclamationsByService(serviceId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération des réclamations du service")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun createReclamation(
        type: String,
        titre: String,
        message: String,
        garageId: String? = null,
        serviceId: String? = null
    ): Resource<ReclamationResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CreateReclamationRequest(type, titre, message, garageId, serviceId)
            val response = apiService.createReclamation(request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la création de la réclamation")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun updateReclamation(id: String, titre: String?, message: String?): Resource<ReclamationResponse> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateReclamationRequest(titre, message)
            val response = apiService.updateReclamation(id, request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la mise à jour de la réclamation")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun deleteReclamation(id: String): Resource<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteReclamation(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la suppression de la réclamation")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }
}

// ==================== USER REPOSITORY ====================

class UserRepository(private val apiService: KarhebtiApiService = RetrofitClient.apiService) {

    suspend fun getAllUsers(): Resource<List<UserResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAllUsers()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération des utilisateurs")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun getUser(id: String): Resource<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUser(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération de l'utilisateur")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun updateUser(id: String, nom: String?, prenom: String?, telephone: String?): Resource<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateUserRequest(nom, prenom, telephone)
            val response = apiService.updateUser(id, request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la mise à jour de l'utilisateur")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun deleteUser(id: String): Resource<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteUser(id)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la suppression de l'utilisateur")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }

    suspend fun updateUserRole(id: String, role: String): Resource<UserResponse> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateRoleRequest(role)
            val response = apiService.updateUserRole(id, request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la mise à jour du rôle")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.message}")
        }
    }
}


class GarageRepository(private val apiService: KarhebtiApiService = RetrofitClient.apiService) {

    suspend fun getGarages(): Resource<List<GarageResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getGarages()

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération des garages")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun getGarageRecommendations(
        typePanne: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        rayon: Double? = null
    ): Resource<List<GarageRecommendation>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getGarageRecommendations(typePanne, latitude, longitude, rayon)

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la récupération des recommandations")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun createGarage(
        nom: String,
        adresse: String,
        telephone: String,
        noteUtilisateur: Double = 0.0,
        heureOuverture: String? = null,
        heureFermeture: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        numberOfBays: Int? = null // ✅ NOUVEAU paramètre
    ): Resource<GarageResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CreateGarageRequest(
                nom = nom,
                adresse = adresse,
                telephone = telephone,
                noteUtilisateur = noteUtilisateur,
                heureOuverture = heureOuverture,
                heureFermeture = heureFermeture,
                latitude = latitude,
                longitude = longitude,
                numberOfBays = numberOfBays // ✅ Ajouter
            )
            val response = apiService.createGarage(request)
            if (response.isSuccessful && response.body() != null) {
                // ✅ Extraire l'objet garage de la réponse CreateGarageResponse
                val createGarageResponse = response.body()!!
                android.util.Log.d("GarageRepository", "Garage créé: ${createGarageResponse.garage.id}, RepairBays: ${createGarageResponse.repairBays?.size ?: 0}")
                Resource.Success(createGarageResponse.garage)
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error("Erreur: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            android.util.Log.e("GarageRepository", "Exception lors de la création du garage", e)
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }


    suspend fun updateGarage(
        garageId: String,
        nom: String? = null,
        adresse: String? = null,
        telephone: String? = null,
        noteUtilisateur: Double? = null,
        heureOuverture: String? = null,
        heureFermeture: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        numberOfBays: Int? = null
    ): Resource<GarageResponse> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateGarageRequest(
                nom = nom,
                adresse = adresse,
                telephone = telephone,
                noteUtilisateur = noteUtilisateur,
                heureOuverture = heureOuverture,
                heureFermeture = heureFermeture,
                latitude = latitude,
                longitude = longitude,
                numberOfBays = numberOfBays
            )
            val response = apiService.updateGarage(garageId, request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error("Erreur: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun deleteGarage(garageId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteGarage(garageId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Erreur lors de la suppression")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau")
        }
    }
}

class ServiceRepository(private val apiService: KarhebtiApiService = RetrofitClient.apiService) {
    suspend fun createService(
        type: String,
        coutMoyen: Double,
        dureeEstimee: Int,
        garageId: String
    ): Resource<ServiceResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CreateServiceRequest(type, coutMoyen, dureeEstimee, garageId)
            val response = apiService.createService(request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error("Erreur: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun getServicesByGarage(garageId: String): Resource<List<ServiceResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getServicesByGarage(garageId)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error("Erreur API: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun updateService(
        serviceId: String,
        type: String,
        coutMoyen: Double,
        dureeEstimee: Int
    ): Resource<ServiceResponse> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateServiceRequest(type, coutMoyen, dureeEstimee)
            val response = apiService.updateService(serviceId, request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Erreur lors de la mise à jour du service")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun deleteService(serviceId: String): Resource<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteService(serviceId)
            if (response.isSuccessful) {
                val body = response.body()
                // Some servers return empty body for 204/200 DELETE, handle both:
                Resource.Success(body ?: MessageResponse("Service supprimé avec succès"))
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error("Erreur lors de la suppression du service: ${errorBody ?: response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }
}
class ReservationRepository(private val apiService: KarhebtiApiService = RetrofitClient.apiService) {

    suspend fun getReservations(): Resource<List<ReservationResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getReservations()
            if (response.isSuccessful && response.body() != null) {
                // The API returns a ReservationListResponse wrapper
                val body = response.body()!!
                Resource.Success(body.reservations) // Extract the reservations list
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error("Erreur lors de la récupération des réservations: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }
    suspend fun getMyReservations(): Resource<List<ReservationResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMyReservations()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Resource.Success(body.reservations) // Extract the reservations list
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error("Erreur lors de la récupération de vos réservations: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun createReservation(
        garageId: String,
        date: String,
        heureDebut: String,
        heureFin: String,
        status: String = "en_attente",
        services: List<String>? = null,
        commentaires: String? = null
    ): Resource<ReservationResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CreateReservationRequest(
                garageId = garageId,
                date = date,
                heureDebut = heureDebut,
                heureFin = heureFin,
                status = status,
                services = services,
                commentaires = commentaires
            )
            val response = apiService.createReservation(request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error("Erreur: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun updateReservationStatus(
        reservationId: String,
        status: String
    ): Resource<ReservationResponse> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateReservationStatusRequest(status)
            val response = apiService.updateReservationStatus(reservationId, request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error("Erreur: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun updateReservation(
        reservationId: String,
        date: String? = null,
        heureDebut: String? = null,
        heureFin: String? = null,
        status: String? = null,
        services: List<String>? = null,
        commentaires: String? = null,
        isPaid: Boolean? = null
    ): Resource<ReservationResponse> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateReservationRequest(
                date = date,
                heureDebut = heureDebut,
                heureFin = heureFin,
                services = services,
                commentaires = commentaires,
                status = status,
                isPaid = isPaid
            )
            val response = apiService.updateReservation(reservationId, request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error("Erreur: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun deleteReservation(id: String): Resource<MessageResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteReservation(id)
            if (response.isSuccessful) {
                Resource.Success(MessageResponse("Réservation supprimée"))
            } else {
                Resource.Error("Erreur lors de la suppression")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau")
        }
    }
}

class OsmRepository(private val apiService: KarhebtiApiService = RetrofitClient.apiService) {

    suspend fun searchAddress(query: String): Resource<List<OsmLocationSuggestion>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.searchAddress(query)

            if (response.isSuccessful && response.body() != null) {
                val suggestions = response.body()!!.map { location ->
                    OsmLocationSuggestion(
                        displayName = location.display_name,
                        latitude = location.lat.toDouble(),
                        longitude = location.lon.toDouble(),
                        address = AddressDetails(
                            road = location.address?.road,
                            city = location.address?.city,
                            country = location.address?.country,
                            postcode = location.address?.postcode
                        )
                    )
                }
                Resource.Success(suggestions)
            } else {
                Resource.Error("Erreur lors de la recherche d'adresse")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): Resource<OsmLocationSuggestion> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.reverseGeocode(lat, lon)

            if (response.isSuccessful && response.body() != null) {
                val location = response.body()!!
                val suggestion = OsmLocationSuggestion(
                    displayName = location.display_name,
                    latitude = location.lat.toDouble(),
                    longitude = location.lon.toDouble(),
                    address = AddressDetails(
                        road = location.address?.road,
                        city = location.address?.city,
                        country = location.address?.country,
                        postcode = location.address?.postcode
                    )
                )
                Resource.Success(suggestion)
            } else {
                Resource.Error("Erreur lors du géocodage inverse")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }
}
class RepairBayRepository(private val apiService: KarhebtiApiService = RetrofitClient.apiService) {

    suspend fun getRepairBaysByGarage(garageId: String): Resource<List<RepairBayResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRepairBaysByGarage(garageId)
                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(response.body()!!)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Resource.Error("Erreur: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                Resource.Error("Erreur réseau: ${e.localizedMessage}")
            }
        }

    suspend fun getAvailableRepairBays(
        garageId: String,
        date: String,
        heureDebut: String,
        heureFin: String
    ): Resource<List<RepairBayResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAvailableRepairBays(
                garageId = garageId,
                date = date,
                heureDebut = heureDebut,
                heureFin = heureFin
            )
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error("Erreur: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun createRepairBay(
        garageId: String,
        bayNumber: Int,
        name: String,
        heureOuverture: String,
        heureFermeture: String,
        isActive: Boolean = true
    ): Resource<RepairBayResponse> = withContext(Dispatchers.IO) {
        try {
            val request = CreateRepairBayRequest(
                garageId = garageId,
                bayNumber = bayNumber,
                name = name,
                heureOuverture = heureOuverture,
                heureFermeture = heureFermeture,
                isActive = isActive
            )
            val response = apiService.createRepairBay(request)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Resource.Error("Erreur: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    suspend fun deleteRepairBay(bayId: String): Resource<MessageResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteRepairBay(bayId)
                if (response.isSuccessful) {
                    Resource.Success(MessageResponse("Créneau supprimé"))
                } else {
                    Resource.Error("Erreur lors de la suppression")
                }
            } catch (e: Exception) {
                Resource.Error("Erreur réseau: ${e.localizedMessage}")
            }
        }
}
