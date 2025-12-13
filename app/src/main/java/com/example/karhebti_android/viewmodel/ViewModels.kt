package com.example.karhebti_android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.karhebti_android.data.api.*
import com.example.karhebti_android.data.model.Reclamation
import com.example.karhebti_android.data.model.Garage
import com.example.karhebti_android.data.model.Service
import com.example.karhebti_android.data.notifications.FCMTokenService
import com.example.karhebti_android.data.preferences.TokenManager
import com.example.karhebti_android.data.preferences.UserData
import com.example.karhebti_android.data.repository.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect

// Data class for counters
data class AppCounters(
    val vehicles: Int = 0,
    val entretiens: Int = 0,
    val garages: Int = 0,
    val documents: Int = 0
)

// Simple sealed class pour représenter l'état d'auth dans le ViewModel
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val data: AuthResponse) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

// Auth ViewModel
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager.getInstance(application)
    private val authRepository = AuthRepository(
        authApiService = RetrofitClient.authApiService,
        context = application.applicationContext
    )

    private val _authState = MutableLiveData<AuthUiState>(AuthUiState.Idle)
    val authState: LiveData<AuthUiState> = _authState

    private val _changePasswordState = MutableStateFlow<Resource<MessageResponse>?>(null)
    val changePasswordState: StateFlow<Resource<MessageResponse>?> = _changePasswordState.asStateFlow()

    private val _verifyOtpState = MutableStateFlow<Resource<MessageResponse>?>(null)
    val verifyOtpState: StateFlow<Resource<MessageResponse>?> = _verifyOtpState.asStateFlow()

    private val _resetPasswordState = MutableStateFlow<Resource<MessageResponse>?>(null)
    val resetPasswordState: StateFlow<Resource<MessageResponse>?> = _resetPasswordState.asStateFlow()

    private val _signupInitiationState = MutableLiveData<Resource<AuthResponse>>()
    val signupInitiationState: LiveData<Resource<AuthResponse>> = _signupInitiationState

    private val _forgotPasswordState = MutableLiveData<Resource<MessageResponse>>()
    val forgotPasswordState: LiveData<Resource<MessageResponse>> = _forgotPasswordState

    init {
        tokenManager.initializeToken()
    }

    fun login(email: String, password: String) {
        _authState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.login(email, password).collect { result ->
                result.fold(
                    onSuccess = { authResponse ->
                        try {
                            android.util.Log.d("AuthViewModel", "Login successful for: $email")
                            // Sauvegarder token et user
                            tokenManager.saveToken(authResponse.accessToken)
                            val user = authResponse.user
                            tokenManager.saveUser(
                                UserData(
                                    id = user.id?.toString(),
                                    email = user.email,
                                    nom = user.nom,
                                    prenom = user.prenom,
                                    role = user.role,
                                    telephone = user.telephone ?: ""
                                )
                            )
                            android.util.Log.d("AuthViewModel", "Token and user saved successfully")
                            // Enregistrer le token FCM
                            registerFCMToken()
                            _authState.value = AuthUiState.Success(authResponse)
                        } catch (e: Exception) {
                            android.util.Log.e("AuthViewModel", "Error saving token/user: ${e.message}", e)
                            _authState.value = AuthUiState.Error("Erreur lors de la sauvegarde: ${e.message}")
                        }
                    },
                    onFailure = { e ->
                        android.util.Log.e("AuthViewModel", "Login error: ${e.message}", e)
                        _authState.value = AuthUiState.Error(e.message ?: "Erreur de connexion")
                    }
                )
            }
        }
    }

    /**
     * Enregistrer le token FCM au backend
     */
    private fun registerFCMToken() {
        val fcmTokenService = FCMTokenService(getApplication())
        fcmTokenService.registerDeviceToken()
        fcmTokenService.subscribeToTopics()
    }

    // Start the two-step signup: call POST /auth/signup to send OTP and create pending signup
    fun signupInitiate(nom: String, prenom: String, email: String, password: String, telephone: String) {
        _signupInitiationState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = authRepository.signup(nom, prenom, email, password, telephone)
                _signupInitiationState.value = result
            } catch (e: Exception) {
                _signupInitiationState.value = Resource.Error("Erreur d'inscription: ${e.localizedMessage}")
            }
        }
    }

    // Complete the signup by verifying the OTP (POST /auth/signup/verify). On success save token & user.
    fun verifySignupOtp(email: String, otpCode: String) {
        _authState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val result = authRepository.verifySignupOtp(email, otpCode)
                when (result) {
                    is Resource.Success -> {
                        // save token and user
                        val auth = result.data!!
                        tokenManager.saveToken(auth.accessToken)
                        tokenManager.saveUser(UserData(
                            id = auth.user.id?.toString(),
                            email = auth.user.email,
                            nom = auth.user.nom,
                            prenom = auth.user.prenom,
                            role = auth.user.role,
                            telephone = auth.user.telephone ?: ""
                        ))
                        _authState.value = AuthUiState.Success(auth)
                    }
                    is Resource.Error -> {
                        _authState.value = AuthUiState.Error(result.message ?: "Erreur de vérification")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                _authState.value = AuthUiState.Error("Erreur lors de la vérification du signup: ${e.localizedMessage}")
            }
        }
    }

    // Remove the old single-step signup implementation (kept for backward compatibility but no longer used)
    @Deprecated("Use signupInitiate + verifySignupOtp for two-step signup flow")
    fun signup(nom: String, prenom: String, email: String, password: String, telephone: String) {
        // Fallback that simply initiates the signup (sends OTP)
        signupInitiate(nom, prenom, email, password, telephone)
    }

    fun forgotPassword(email: String) {
        _forgotPasswordState.value = Resource.Loading()
        viewModelScope.launch {
            val result = authRepository.forgotPassword(email)
            _forgotPasswordState.value = result
        }
    }

    fun verifyOtp(email: String, otp: String) {
        _verifyOtpState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = authRepository.verifyOtp(email, otp)
                _verifyOtpState.value = result
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Verify OTP error: ${e.message}", e)
                _verifyOtpState.value = Resource.Error("Erreur lors de la vérification: ${e.localizedMessage}")
            }
        }
    }

    fun resetPassword(email: String, otp: String, newPassword: String) {
        _resetPasswordState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = authRepository.resetPassword(email, otp, newPassword)
                _resetPasswordState.value = result
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Reset password error: ${e.message}", e)
                _resetPasswordState.value = Resource.Error("Erreur lors de la réinitialisation: ${e.localizedMessage}")
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        _changePasswordState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = authRepository.changePassword(currentPassword, newPassword)
                _changePasswordState.value = result
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Change password error: ${e.message}", e)
                _changePasswordState.value = Resource.Error("Erreur lors du changement de mot de passe: ${e.localizedMessage}")
            }
        }
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = Resource.Loading()
    }

    fun resetVerifyOtpState() {
        _verifyOtpState.value = null
    }

    fun resetResetPasswordState() {
        _resetPasswordState.value = null
    }

    fun resetChangePasswordState() {
        _changePasswordState.value = null
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout().collect {
                // Quel que soit le résultat, on nettoie le token localement
                tokenManager.clearAll()
                _authState.value = AuthUiState.Idle
                // Reset all state flows to prevent UI from attempting API calls with null token
                _forgotPasswordState.value = Resource.Loading()
                _verifyOtpState.value = null
                _resetPasswordState.value = null
                _changePasswordState.value = null
            }
        }
    }

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    fun getCurrentUser(): UserData? = tokenManager.getUser()
}

// Car ViewModel
class CarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CarRepository()

    private val _carsState = MutableLiveData<Resource<List<CarResponse>>>()
    val carsState: LiveData<Resource<List<CarResponse>>> = _carsState

    private val _carsStateFlow = MutableStateFlow<Resource<List<CarResponse>>?>(null)
    val carsStateFlow: StateFlow<Resource<List<CarResponse>>?> = _carsStateFlow.asStateFlow()

    private val _carCount = MutableStateFlow(0)
    val carCount: StateFlow<Int> = _carCount.asStateFlow()

    private val _createCarState = MutableLiveData<Resource<CarResponse>>()
    val createCarState: LiveData<Resource<CarResponse>> = _createCarState

    private val _updateCarState = MutableLiveData<Resource<CarResponse>>()
    val updateCarState: LiveData<Resource<CarResponse>> = _updateCarState

    private val _deleteCarState = MutableLiveData<Resource<MessageResponse>>()
    val deleteCarState: LiveData<Resource<MessageResponse>> = _deleteCarState

    fun getMyCars() {
        _carsState.value = Resource.Loading()
        _carsStateFlow.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getMyCars()
            _carsState.value = result
            _carsStateFlow.value = result
            if (result is Resource.Success) {
                _carCount.value = result.data?.size ?: 0
            }
        }
    }

    fun refresh() = getMyCars()

    fun createCar(marque: String, modele: String, annee: Int, immatriculation: String, typeCarburant: String, kilometrage: Int? = null) {
        _createCarState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.createCar(marque, modele, annee, immatriculation, typeCarburant, kilometrage)
            _createCarState.value = result

            if (result is Resource.Success) {
                getMyCars() // Refresh list
            }
        }
    }

    fun updateCar(
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
    ) {
        _updateCarState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.updateCar(
                id, marque, modele, annee, typeCarburant,
                kilometrage, statut, prochainEntretien, joursProchainEntretien, imageUrl
            )
            _updateCarState.value = result

            if (result is Resource.Success) {
                getMyCars() // Refresh list
            }
        }
    }

    fun deleteCar(id: String) {
        _deleteCarState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.deleteCar(id)
            _deleteCarState.value = result

            if (result is Resource.Success) {
                getMyCars() // Refresh list
            }
        }
    }

    fun resetDeleteState() {
        _deleteCarState.value = Resource.Loading() // Reset to loading instead of null
    }
}

// Maintenance ViewModel
class MaintenanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MaintenanceRepository()

    private val _maintenancesState = MutableLiveData<Resource<List<MaintenanceResponse>>>()
    val maintenancesState: LiveData<Resource<List<MaintenanceResponse>>> = _maintenancesState

    private val _maintenancesStateFlow = MutableStateFlow<Resource<List<MaintenanceResponse>>?>(null)
    val maintenancesStateFlow: StateFlow<Resource<List<MaintenanceResponse>>?> = _maintenancesStateFlow.asStateFlow()

    private val _maintenanceCount = MutableStateFlow(0)
    val maintenanceCount: StateFlow<Int> = _maintenanceCount.asStateFlow()

    private val _maintenanceState = MutableLiveData<Resource<MaintenanceResponse>>()
    val maintenanceState: LiveData<Resource<MaintenanceResponse>> = _maintenanceState

    private val _createMaintenanceState = MutableLiveData<Resource<MaintenanceResponse>>()
    val createMaintenanceState: LiveData<Resource<MaintenanceResponse>> = _createMaintenanceState

    private val _updateMaintenanceState = MutableLiveData<Resource<MaintenanceResponse>>()
    val updateMaintenanceState: LiveData<Resource<MaintenanceResponse>> = _updateMaintenanceState

    private val _deleteMaintenanceState = MutableLiveData<Resource<MessageResponse>?>(null)
    val deleteMaintenanceState: LiveData<Resource<MessageResponse>?> = _deleteMaintenanceState

    fun getMaintenances() {
        _maintenancesState.value = Resource.Loading()
        _maintenancesStateFlow.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getMaintenances()
            _maintenancesState.value = result
            _maintenancesStateFlow.value = result
            if (result is Resource.Success) {
                _maintenanceCount.value = result.data?.size ?: 0
            }
        }
    }

    fun refresh() = getMaintenances()

    fun createMaintenance(type: String, date: String, cout: Double, garage: String, voiture: String) {
        _createMaintenanceState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.createMaintenance(type, date, cout, garage, voiture)
            _createMaintenanceState.value = result

            if (result is Resource.Success) {
                getMaintenances() // Refresh list
            }
        }
    }

    fun getMaintenanceById(id: String) {
        _maintenanceState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getMaintenanceById(id)
            _maintenanceState.value = result
        }
    }

    fun updateMaintenanceDate(id: String, date: String) {
        _updateMaintenanceState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.updateMaintenance(id, UpdateMaintenanceRequest(date = date))
            _updateMaintenanceState.value = result
        }
    }

    fun deleteMaintenance(id: String) {
        _deleteMaintenanceState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.deleteMaintenance(id)
            _deleteMaintenanceState.value = result
        }
    }
}

// Document ViewModel
class DocumentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DocumentRepository()

    // Document States
    private val _documentsState = MutableLiveData<Resource<List<DocumentResponse>>>()
    val documentsState: LiveData<Resource<List<DocumentResponse>>> = _documentsState

    private val _documentDetailState = MutableLiveData<Resource<DocumentResponse>>()
    val documentDetailState: LiveData<Resource<DocumentResponse>> = _documentDetailState

    private val _documentsStateFlow = MutableStateFlow<Resource<List<DocumentResponse>>?>(null)
    val documentsStateFlow: StateFlow<Resource<List<DocumentResponse>>?> = _documentsStateFlow.asStateFlow()

    private val _documentCount = MutableStateFlow(0)
    val documentCount: StateFlow<Int> = _documentCount.asStateFlow()

    private val _createDocumentState = MutableLiveData<Resource<DocumentResponse>>()
    val createDocumentState: LiveData<Resource<DocumentResponse>> = _createDocumentState

    private val _updateDocumentState = MutableLiveData<Resource<DocumentResponse>>()
    val updateDocumentState: LiveData<Resource<DocumentResponse>> = _updateDocumentState

    fun getDocuments() {
        _documentsState.value = Resource.Loading()
        _documentsStateFlow.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getDocuments()
            _documentsState.value = result
            _documentsStateFlow.value = result
            if (result is Resource.Success) {
                _documentCount.value = result.data?.size ?: 0
                // Vérifier les documents expirante et logger les alertes
                checkExpiringDocuments(result.data ?: emptyList())
            }
        }
    }

    private fun checkExpiringDocuments(documents: List<DocumentResponse>) {
        val expirationService = com.example.karhebti_android.data.websocket.DocumentExpirationNotificationService()
        val expiringDocuments = expirationService.getDocumentsExpiringWithinThreeDays(documents)

        if (expiringDocuments.isNotEmpty()) {
            android.util.Log.w("DocumentViewModel", "🚨 ${expiringDocuments.size} document(s) expire(nt) dans 3 jours")
            expiringDocuments.forEach { doc ->
                val alertMessage = expirationService.getAlertMessage(doc)
                android.util.Log.w("DocumentViewModel", alertMessage)
            }
        }
    }

    fun getDocumentById(id: String) {
        android.util.Log.d("DocumentViewModel", "getDocumentById called with ID: $id")
        _documentDetailState.value = Resource.Loading()
        viewModelScope.launch {
            android.util.Log.d("DocumentViewModel", "Fetching document from repository...")
            val result = repository.getDocumentById(id)
            android.util.Log.d("DocumentViewModel", "Result type: ${result::class.simpleName}")
            _documentDetailState.value = result
        }
    }

    fun refresh() = getDocuments()

    fun createDocument(request: CreateDocumentRequest) {
        _createDocumentState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.createDocument(request)
            _createDocumentState.value = result
            if (result is Resource.Success) {
                getDocuments() // Refresh list
            }
        }
    }

    fun updateDocument(id: String, request: UpdateDocumentRequest) {
        _updateDocumentState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.updateDocument(id, request)
            _updateDocumentState.value = result
            if (result is Resource.Success) {
                getDocuments() // Refresh list
                getDocumentById(id) // Refresh detail view
            }
        }
    }

    fun createDocument(request: CreateDocumentRequest, filePath: String? = null) {
        _createDocumentState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.createDocument(request, filePath)
            _createDocumentState.value = result
            if (result is Resource.Success) {
                getDocuments() // Refresh list
            }
        }
    }

    fun updateDocument(id: String, request: UpdateDocumentRequest, filePath: String? = null) {
        _updateDocumentState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.updateDocument(id, request, filePath)
            _updateDocumentState.value = result
            if (result is Resource.Success) {
                getDocuments() // Refresh list
                getDocumentById(id) // Refresh detail view
            }
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            val result = repository.deleteDocument(id)
            if (result is Resource.Success) {
                getDocuments() // Refresh list
            }
        }
    }
}

// Part ViewModel
class PartViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PartRepository()

    private val _partsState = MutableLiveData<Resource<List<PartResponse>>>()
    val partsState: LiveData<Resource<List<PartResponse>>> = _partsState

    private val _createPartState = MutableLiveData<Resource<PartResponse>>()
    val createPartState: LiveData<Resource<PartResponse>> = _createPartState

    fun getParts() {
        _partsState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getParts()
            _partsState.value = result
        }
    }

    fun createPart(nom: String, type: String, dateInstallation: String, kilometrageRecommande: Int, voiture: String) {
        _createPartState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.createPart(nom, type, dateInstallation, kilometrageRecommande, voiture)
            _createPartState.value = result

            if (result is Resource.Success) {
                getParts() // Refresh list
            }
        }
    }

    fun deletePart(id: String) {
        viewModelScope.launch {
            val result = repository.deletePart(id)
            if (result is Resource.Success) {
                getParts() // Refresh list
            }
        }
    }
}

// AI ViewModel
class AIViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AIRepository()

    private val _roadIssueState = MutableLiveData<Resource<RoadIssueResponse>>()
    val roadIssueState: LiveData<Resource<RoadIssueResponse>> = _roadIssueState

    private val _dangerZonesState = MutableLiveData<Resource<List<DangerZone>>>()
    val dangerZonesState: LiveData<Resource<List<DangerZone>>> = _dangerZonesState

    private val _maintenanceRecommendationsState = MutableLiveData<Resource<MaintenanceRecommendationResponse>>()
    val maintenanceRecommendationsState: LiveData<Resource<MaintenanceRecommendationResponse>> = _maintenanceRecommendationsState

    fun reportRoadIssue(latitude: Double, longitude: Double, typeAnomalie: String, description: String) {
        _roadIssueState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.reportRoadIssue(latitude, longitude, typeAnomalie, description)
            _roadIssueState.value = result
        }
    }

    fun getDangerZones(latitude: Double? = null, longitude: Double? = null, rayon: Double? = null) {
        _dangerZonesState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getDangerZones(latitude, longitude, rayon)
            _dangerZonesState.value = result
        }
    }

    fun getMaintenanceRecommendations(carId: String, currentKilometrage: Int) {
        _maintenanceRecommendationsState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getMaintenanceRecommendations(carId, currentKilometrage)
            _maintenanceRecommendationsState.value = result
        }
    }
}

// User ViewModel (for admin)
class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserRepository()

    private val _usersState = MutableLiveData<Resource<List<UserResponse>>>()
    val usersState: LiveData<Resource<List<UserResponse>>> = _usersState

    private val _updateUserState = MutableLiveData<Resource<UserResponse>>()
    val updateUserState: LiveData<Resource<UserResponse>> = _updateUserState

    fun getAllUsers() {
        _usersState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getAllUsers()
            _usersState.value = result
        }
    }

    fun updateUser(id: String, nom: String? = null, prenom: String? = null, telephone: String? = null) {
        _updateUserState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.updateUser(id, nom, prenom, telephone)
            _updateUserState.value = result

            if (result is Resource.Success) {
                getAllUsers() // Refresh list
            }
        }
    }


    fun updateUserRole(id: String, role: String) {
        viewModelScope.launch {
            val result = repository.updateUserRole(id, role)
            if (result is Resource.Success) {
                getAllUsers() // Refresh list
            }
        }
    }
}

// Reclamation (Feedback) ViewModel
class ReclamationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReclamationRepository()

    private val _reclamationsState = MutableLiveData<Resource<List<Reclamation>>>()
    val reclamationsState: LiveData<Resource<List<Reclamation>>> = _reclamationsState

    private val _reclamationDetailState = MutableLiveData<Resource<Reclamation>>()
    val reclamationDetailState: LiveData<Resource<Reclamation>> = _reclamationDetailState

    private val _myReclamationsState = MutableLiveData<Resource<List<Reclamation>>>()
    val myReclamationsState: LiveData<Resource<List<Reclamation>>> = _myReclamationsState

    private val _createReclamationState = MutableLiveData<Resource<ReclamationResponse>>()
    val createReclamationState: LiveData<Resource<ReclamationResponse>> = _createReclamationState

    private val _updateReclamationState = MutableLiveData<Resource<ReclamationResponse>>()
    val updateReclamationState: LiveData<Resource<ReclamationResponse>> = _updateReclamationState

    private val _deleteReclamationState = MutableLiveData<Resource<MessageResponse>>()
    val deleteReclamationState: LiveData<Resource<MessageResponse>> = _deleteReclamationState

    private val _reclamationsStateFlow = MutableStateFlow<Resource<List<Reclamation>>?>(null)
    val reclamationsStateFlow: StateFlow<Resource<List<Reclamation>>?> = _reclamationsStateFlow.asStateFlow()

    private fun mapToReclamation(response: ReclamationResponse): Reclamation {
        return Reclamation(
            id = response.id,
            titre = response.titre,
            message = response.message,
            type = response.type,
            garage = response.garage?.let {
                Garage(
                    id = it.id,
                    nom = it.nom,
                    adresse = it.adresse,
                    latitude = 0.0,
                    longitude = 0.0,
                    distance = 0.0,
                    rating = 0.0f,
                    reviewCount = 0,
                    phoneNumber = it.telephone ?: "",
                    isOpen = false,
                    openUntil = null,
                    services = emptyList(),
                    imageUrl = null
                )
            },
            service = response.service?.let { Service(it.id, it.type) },
            createdAt = response.createdAt
        )
    }

    fun getAllReclamations() {
        _reclamationsState.value = Resource.Loading()
        _reclamationsStateFlow.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getReclamations()
            @Suppress("UNCHECKED_CAST")
            val mappedResult: Resource<List<Reclamation>> = when (result) {
                is Resource.Success -> Resource.Success(result.data?.map { mapToReclamation(it) } ?: emptyList())
                is Resource.Error -> result as Resource<List<Reclamation>>
                is Resource.Loading -> result as Resource<List<Reclamation>>
            }
            _reclamationsState.value = mappedResult
            _reclamationsStateFlow.value = mappedResult
        }
    }

    fun getReclamationById(id: String) {
        _reclamationDetailState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.getReclamationById(id)
                android.util.Log.d("ReclamationViewModel", "getReclamationById result: $result")
                @Suppress("UNCHECKED_CAST")
                val mappedResult: Resource<Reclamation> = when (result) {
                    is Resource.Success -> result.data?.let { Resource.Success(mapToReclamation(it)) } as? Resource<Reclamation> ?: Resource.Error("No data")
                    is Resource.Error -> result as Resource<Reclamation>
                    is Resource.Loading -> result as Resource<Reclamation>
                }
                _reclamationDetailState.value = mappedResult
            } catch (e: Exception) {
                android.util.Log.e("ReclamationViewModel", "Error in getReclamationById: ${e.message}", e)
                _reclamationDetailState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }

    fun getMyReclamations() {
        _myReclamationsState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.getMyReclamations()
                android.util.Log.d("ReclamationViewModel", "getMyReclamations result: $result")
                @Suppress("UNCHECKED_CAST")
                val mappedResult: Resource<List<Reclamation>> = when (result) {
                    is Resource.Success -> Resource.Success(result.data?.map { mapToReclamation(it) } ?: emptyList())
                    is Resource.Error -> result as Resource<List<Reclamation>>
                    is Resource.Loading -> result as Resource<List<Reclamation>>
                }
                _myReclamationsState.value = mappedResult
            } catch (e: Exception) {
                android.util.Log.e("ReclamationViewModel", "Error in getMyReclamations: ${e.message}", e)
                _myReclamationsState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }

    fun getReclamationsByGarage(garageId: String) {
        _reclamationsState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getReclamationsByGarage(garageId)
            @Suppress("UNCHECKED_CAST")
            val mappedResult: Resource<List<Reclamation>> = when (result) {
                is Resource.Success -> Resource.Success(result.data?.map { mapToReclamation(it) } ?: emptyList())
                is Resource.Error -> result as Resource<List<Reclamation>>
                is Resource.Loading -> result as Resource<List<Reclamation>>
            }
            _reclamationsState.value = mappedResult
        }
    }

    fun getReclamationsByService(serviceId: String) {
        _reclamationsState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getReclamationsByService(serviceId)
            @Suppress("UNCHECKED_CAST")
            val mappedResult: Resource<List<Reclamation>> = when (result) {
                is Resource.Success -> Resource.Success(result.data?.map { mapToReclamation(it) } ?: emptyList())
                is Resource.Error -> result as Resource<List<Reclamation>>
                is Resource.Loading -> result as Resource<List<Reclamation>>
            }
            _reclamationsState.value = mappedResult
        }
    }

    fun createReclamation(
        type: String,
        titre: String,
        message: String,
        garageId: String? = null,
        serviceId: String? = null
    ) {
        _createReclamationState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.createReclamation(
                type = type,
                titre = titre,
                message = message,
                garageId = garageId,
                serviceId = serviceId
            )
            _createReclamationState.value = result

            if (result is Resource.Success) {
                getMyReclamations() // Refresh user's reclamations
            }
        }
    }

    fun updateReclamation(id: String, titre: String? = null, message: String? = null) {
        _updateReclamationState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.updateReclamation(id, titre, message)
            _updateReclamationState.value = result

            if (result is Resource.Success) {
                getMyReclamations() // Refresh list
            }
        }
    }

    fun deleteReclamation(id: String) {
        _deleteReclamationState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.deleteReclamation(id)
            _deleteReclamationState.value = result

            if (result is Resource.Success) {
                getMyReclamations() // Refresh list
            }
        }
    }

    fun refresh() {
        getMyReclamations()
    }
}

// OSM ViewModel
class OsmViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OsmRepository()

    private val _searchResults = MutableLiveData<Resource<List<OsmLocationSuggestion>>>()
    val searchResults: LiveData<Resource<List<OsmLocationSuggestion>>> = _searchResults

    private val _selectedLocation = MutableLiveData<OsmLocationSuggestion?>()
    val selectedLocation: LiveData<OsmLocationSuggestion?> = _selectedLocation

    private val _reverseGeocodeResult = MutableLiveData<Resource<OsmLocationSuggestion>>()
    val reverseGeocodeResult: LiveData<Resource<OsmLocationSuggestion>> = _reverseGeocodeResult

    fun searchAddress(query: String) {
        if (query.length < 3) {
            _searchResults.value = Resource.Success(emptyList())
            return
        }

        _searchResults.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = repository.searchAddress(query)
                _searchResults.value = result
            } catch (e: Exception) {
                _searchResults.value = Resource.Error("Erreur de recherche: ${e.message}")
            }
        }
    }

    fun reverseGeocode(lat: Double, lon: Double) {
        _reverseGeocodeResult.value = Resource.Loading()

        viewModelScope.launch {
            try {
                val result = repository.reverseGeocode(lat, lon)
                _reverseGeocodeResult.value = result
            } catch (e: Exception) {
                _reverseGeocodeResult.value = Resource.Error("Erreur de géocodage: ${e.message}")
            }
        }
    }

    fun selectLocation(location: OsmLocationSuggestion) {
        _selectedLocation.value = location
    }

    fun clearSelection() {
        _selectedLocation.value = null
    }

    fun clearSearch() {
        _searchResults.value = Resource.Success(emptyList())
    }
}

class ReservationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReservationRepository()

    private val _reservationsState = MutableLiveData<Resource<List<ReservationResponse>>>()
    val reservationsState: LiveData<Resource<List<ReservationResponse>>> = _reservationsState

    private val _createReservationState = MutableLiveData<Resource<ReservationResponse>>()
    val createReservationState: LiveData<Resource<ReservationResponse>> = _createReservationState

    private val _updateReservationState = MutableLiveData<Resource<ReservationResponse>>()
    val updateReservationState: LiveData<Resource<ReservationResponse>> = _updateReservationState

    private val _deleteReservationState = MutableLiveData<Resource<MessageResponse>>()
    val deleteReservationState: LiveData<Resource<MessageResponse>> = _deleteReservationState

    private val _updateStatusState = MutableLiveData<Resource<ReservationResponse>>()
    val updateStatusState: LiveData<Resource<ReservationResponse>> = _updateStatusState

    // For users to get their own reservations
    fun getMyReservations() {
        _reservationsState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getMyReservations()
            _reservationsState.value = result
        }
    }

    // For garage owners to get all reservations (with filtering)
    fun getReservations(garageId: String? = null) {
        _reservationsState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getReservations()
            _reservationsState.value = result
        }
    }

    fun createReservation(
        garageId: String,
        date: String,
        heureDebut: String,
        heureFin: String,
        status: String = "en_attente",
        services: List<String>? = null,
        commentaires: String? = null
    ) {
        _createReservationState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.createReservation(
                garageId = garageId,
                date = date,
                heureDebut = heureDebut,
                heureFin = heureFin,
                status = status,
                services = services,
                commentaires = commentaires
            )
            _createReservationState.value = result
            if (result is Resource.Success) {
                getMyReservations() // Refresh with user's reservations
            }
        }
    }

    fun updateReservation(
        id: String,
        date: String? = null,
        heureDebut: String? = null,
        heureFin: String? = null,
        status: String? = null,
        services: List<String>? = null,
        commentaires: String? = null,
        isPaid: Boolean? = null
    ) {
        _updateReservationState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.updateReservation(
                    reservationId = id,
                    date = date,
                    heureDebut = heureDebut,
                    heureFin = heureFin,
                    status = status,
                    services = services,
                    commentaires = commentaires,
                    isPaid = isPaid
                )
                _updateReservationState.value = result
                if (result is Resource.Success<ReservationResponse>) {
                    getMyReservations() // Refresh with user's reservations
                }
            } catch (e: Exception) {
                _updateReservationState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }

    fun updateReservationStatus(
        id: String,
        status: String
    ) {
        _updateStatusState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.updateReservationStatus(id, status)
                _updateStatusState.value = result
                if (result is Resource.Success) {
                    getReservations() // Refresh all reservations for garage owner
                }
            } catch (e: Exception) {
                _updateStatusState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }

    fun deleteReservation(id: String) {
        _deleteReservationState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.deleteReservation(id)
            _deleteReservationState.value = result
            if (result is Resource.Success) {
                getMyReservations() // Refresh with user's reservations
            }
        }
    }
}

class ServiceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ServiceRepository()
    private val _createServiceState = MutableLiveData<Resource<ServiceResponse>>()
    val createServiceState: LiveData<Resource<ServiceResponse>> = _createServiceState
    private val _servicesState = MutableLiveData<Resource<List<ServiceResponse>>>()
    val servicesState: LiveData<Resource<List<ServiceResponse>>> = _servicesState
    private val _updateServiceState = MutableLiveData<Resource<ServiceResponse>>()
    val updateServiceState: LiveData<Resource<ServiceResponse>> = _updateServiceState
    private val _deleteServiceState = MutableLiveData<Resource<MessageResponse>>()
    val deleteServiceState: LiveData<Resource<MessageResponse>> = _deleteServiceState

    fun createService(
        type: String,
        coutMoyen: Double,
        dureeEstimee: Int,
        garageId: String
    ) {
        _createServiceState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.createService(type, coutMoyen, dureeEstimee, garageId)
                _createServiceState.value = result
            } catch (e: Exception) {
                _createServiceState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }
    suspend fun createServiceSuspend(
        type: String,
        coutMoyen: Double,
        dureeEstimee: Int,
        garageId: String
    ): Resource<ServiceResponse> {
        return try {
            repository.createService(type, coutMoyen, dureeEstimee, garageId)
        } catch (e: Exception) {
            Resource.Error("Erreur: ${e.message}")
        }
    }

    fun getServicesByGarage(garageId: String) {
        _servicesState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.getServicesByGarage(garageId)
                _servicesState.value = result
            } catch (e: Exception) {
                _servicesState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }
    fun updateService(
        serviceId: String,
        garageId: String,
        type: String,
        coutMoyen: Double,
        dureeEstimee: Int
    ) {
        _updateServiceState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.updateService(serviceId, type, coutMoyen, dureeEstimee)
                _updateServiceState.value = result
                if (result is Resource.Success) getServicesByGarage(garageId)
            } catch (e: Exception) {
                _updateServiceState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }
    fun deleteService(serviceId: String, garageId: String) {
        _deleteServiceState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.deleteService(serviceId)
                _deleteServiceState.value = result
                if (result is Resource.Success) getServicesByGarage(garageId)
            } catch (e: Exception) {
                _deleteServiceState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }
}

class GarageViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GarageRepository()

    private val _garagesState = MutableLiveData<Resource<List<GarageResponse>>>()
    val garagesState: LiveData<Resource<List<GarageResponse>>> = _garagesState

    private val _recommendationsState = MutableLiveData<Resource<List<GarageRecommendation>>>()
    val recommendationsState: LiveData<Resource<List<GarageRecommendation>>> = _recommendationsState

    private val _createGarageState = MutableLiveData<Resource<GarageResponse>>()
    val createGarageState: LiveData<Resource<GarageResponse>> = _createGarageState

    private val _updateGarageState = MutableLiveData<Resource<GarageResponse>>()
    val updateGarageState: LiveData<Resource<GarageResponse>> = _updateGarageState

    private val _deleteGarageState = MutableLiveData<Resource<Unit>>()
    val deleteGarageState: LiveData<Resource<Unit>> = _deleteGarageState

    fun getGarages() {
        _garagesState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.getGarages()
                _garagesState.value = result
            } catch (e: Exception) {
                _garagesState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }

    fun getRecommendations(
        typePanne: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        rayon: Double? = null
    ) {
        _recommendationsState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.getGarageRecommendations(typePanne, latitude, longitude, rayon)
                _recommendationsState.value = result
            } catch (e: Exception) {
                _recommendationsState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }

    fun createGarage(
        nom: String,
        adresse: String,
        telephone: String,
        noteUtilisateur: Double = 0.0,
        heureOuverture: String? = null,
        heureFermeture: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        numberOfBays: Int = 1 // ✅ NOUVEAU paramètre avec valeur par défaut
    ) {
        _createGarageState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.createGarage(
                    nom = nom,
                    adresse = adresse,
                    telephone = telephone,
                    noteUtilisateur = noteUtilisateur,
                    heureOuverture = heureOuverture,
                    heureFermeture = heureFermeture,
                    latitude = latitude,
                    longitude = longitude,
                    numberOfBays = numberOfBays // ✅ Passer le paramètre
                )
                _createGarageState.value = result
                if (result is Resource.Success) {
                    getGarages()
                }
            } catch (e: Exception) {
                _createGarageState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }

    fun updateGarage(
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
    ) {
        _updateGarageState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.updateGarage(
                    garageId = garageId,
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
                _updateGarageState.value = result

                if (result is Resource.Success) {
                    getGarages()
                }
            } catch (e: Exception) {
                _updateGarageState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }

    fun deleteGarage(garageId: String) {
        _deleteGarageState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.deleteGarage(garageId)
                _deleteGarageState.value = result
                if (result is Resource.Success) {
                    getGarages()
                }
            } catch (e: Exception) {
                _deleteGarageState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }
    // ✅ Ajouter cette méthode pour réinitialiser l'état de création
    fun resetCreateGarageState() {
        _createGarageState.value = Resource.Loading()
    }
}


class RepairBayViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RepairBayRepository()

    private val _repairBaysState = MutableLiveData<Resource<List<RepairBayResponse>>>()
    val repairBaysState: LiveData<Resource<List<RepairBayResponse>>> = _repairBaysState

    private val _availableBaysState = MutableLiveData<Resource<List<RepairBayResponse>>>()
    val availableBaysState: LiveData<Resource<List<RepairBayResponse>>> = _availableBaysState

    private val _createRepairBayState = MutableLiveData<Resource<RepairBayResponse>>()
    val createRepairBayState: LiveData<Resource<RepairBayResponse>> = _createRepairBayState

    fun getRepairBaysByGarage(garageId: String) {
        _repairBaysState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.getRepairBaysByGarage(garageId)
                _repairBaysState.value = result
            } catch (e: Exception) {
                _repairBaysState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }

    fun getAvailableRepairBays(
        garageId: String,
        date: String,
        heureDebut: String,
        heureFin: String
    ) {
        _availableBaysState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.getAvailableRepairBays(
                    garageId = garageId,
                    date = date,
                    heureDebut = heureDebut,
                    heureFin = heureFin
                )
                _availableBaysState.value = result
            } catch (e: Exception) {
                _availableBaysState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }

    // ✅ NOUVEAU: Créer une baie de réparation
    fun createRepairBay(
        garageId: String,
        bayNumber: Int,
        name: String,
        heureOuverture: String,
        heureFermeture: String,
        isActive: Boolean = true
    ) {
        _createRepairBayState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val result = repository.createRepairBay(
                    garageId = garageId,
                    bayNumber = bayNumber,
                    name = name,
                    heureOuverture = heureOuverture,
                    heureFermeture = heureFermeture,
                    isActive = isActive
                )
                _createRepairBayState.value = result
                if (result is Resource.Success) {
                    // Rafraîchir la liste des baies
                    getRepairBaysByGarage(garageId)
                }
            } catch (e: Exception) {
                _createRepairBayState.value = Resource.Error("Erreur: ${e.message}")
            }
        }
    }

    // Réinitialiser l'état des créneaux disponibles
    fun clearAvailableBays() {
        _availableBaysState.value = Resource.Success(emptyList())
    }
}
