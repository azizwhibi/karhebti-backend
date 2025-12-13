package com.example.karhebti_android.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.karhebti_android.data.api.*
import com.example.karhebti_android.data.newrepo.EmailVerificationRepository
import com.example.karhebti_android.data.newrepo.OtpLoginRepository
import com.example.karhebti_android.data.newrepo.CarImageRepository
import com.example.karhebti_android.data.newrepo.MaintenanceRepositoryImpl
import com.example.karhebti_android.data.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*


// ViewModel for Email Verification
class EmailVerificationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EmailVerificationRepository()

    private val _verificationState = MutableStateFlow<Resource<EmailVerificationResponse>?>(null)
    val verificationState: StateFlow<Resource<EmailVerificationResponse>?> = _verificationState.asStateFlow()

    private val _resendState = MutableStateFlow<Resource<EmailVerificationResponse>?>(null)
    val resendState: StateFlow<Resource<EmailVerificationResponse>?> = _resendState.asStateFlow()

    fun verifyEmail(email: String, code: String) {
        _verificationState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.verifyEmail(email, code)
            _verificationState.value = result
        }
    }

    fun resendVerificationCode(email: String) {
        _resendState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.sendVerificationCode(email)
            _resendState.value = result
        }
    }
}

// ViewModel for OTP Login
class OtpLoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OtpLoginRepository()

    private val _otpSendState = MutableStateFlow<Resource<OtpResponse>?>(null)
    val otpSendState: StateFlow<Resource<OtpResponse>?> = _otpSendState.asStateFlow()

    private val _otpVerifyState = MutableStateFlow<Resource<AuthResponse>?>(null)
    val otpVerifyState: StateFlow<Resource<AuthResponse>?> = _otpVerifyState.asStateFlow()

    fun sendOtpCode(identifier: String) {
        _otpSendState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.sendOtpCode(identifier)
            _otpSendState.value = result
        }
    }

    fun verifyOtpLogin(identifier: String, code: String) {
        _otpVerifyState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.verifyOtpLogin(identifier, code)
            _otpVerifyState.value = result
        }
    }
}

// ViewModel for Car Image Upload
class CarImageViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CarImageRepository()

    private val _uploadState = MutableStateFlow<Resource<CarResponse>?>(null)
    val uploadState: StateFlow<Resource<CarResponse>?> = _uploadState.asStateFlow()

    private val _validationState = MutableStateFlow<String?>(null)
    val validationState: StateFlow<String?> = _validationState.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    fun uploadCarImage(carId: String, imageUri: Uri) {
        _uploadState.value = Resource.Loading()
        _isUploading.value = true
        _validationState.value = null

        viewModelScope.launch {
            val result = repository.uploadCarImage(carId, imageUri, getApplication())
            _uploadState.value = result
            _isUploading.value = false
        }
    }

    fun validateImage(imageUri: Uri): Boolean {
        val context = getApplication<android.app.Application>()
        val validationResult = com.example.karhebti_android.util.ImageUploadValidator.validateImage(context, imageUri)

        _validationState.value = if (!validationResult.isValid) {
            validationResult.error
        } else {
            null
        }

        return validationResult.isValid
    }

    fun resetUploadState() {
        _uploadState.value = null
        _validationState.value = null
        _isUploading.value = false
    }

    fun deleteCarImage(carId: String) {
        _uploadState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.deleteCarImage(carId)
            _uploadState.value = result
        }
    }
}

// ViewModel for Entretiens with Search/Filter
class EntretiensFilterViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = MaintenanceRepositoryImpl(application)

    private val _maintenancesState = MutableStateFlow<Resource<PaginatedMaintenancesResponse>?>(null)
    val maintenancesState: StateFlow<Resource<PaginatedMaintenancesResponse>?> = _maintenancesState.asStateFlow()

    // Filter state - persisted in SavedStateHandle
    var searchQuery: String
        get() = savedStateHandle["searchQuery"] ?: ""
        set(value) { savedStateHandle["searchQuery"] = value }

    var selectedStatus: String?
        get() = savedStateHandle["selectedStatus"]
        set(value) { savedStateHandle["selectedStatus"] = value }

    var dateFrom: String?
        get() = savedStateHandle["dateFrom"]
        set(value) { savedStateHandle["dateFrom"] = value }

    var dateTo: String?
        get() = savedStateHandle["dateTo"]
        set(value) { savedStateHandle["dateTo"] = value }

    var selectedTags: List<String>
        get() = savedStateHandle.get<List<String>>("selectedTags") ?: emptyList()
        set(value) { savedStateHandle["selectedTags"] = value }

    var minCost: Double?
        get() = savedStateHandle["minCost"]
        set(value) { savedStateHandle["minCost"] = value }

    var maxCost: Double?
        get() = savedStateHandle["maxCost"]
        set(value) { savedStateHandle["maxCost"] = value }

    var minMileage: Int?
        get() = savedStateHandle["minMileage"]
        set(value) { savedStateHandle["minMileage"] = value }

    var maxMileage: Int?
        get() = savedStateHandle["maxMileage"]
        set(value) { savedStateHandle["maxMileage"] = value }

    var sortField: String
        get() = savedStateHandle["sortField"] ?: "dueAt"
        set(value) { savedStateHandle["sortField"] = value }

    var sortOrder: String
        get() = savedStateHandle["sortOrder"] ?: "asc"
        set(value) { savedStateHandle["sortOrder"] = value }

    var currentPage: Int
        get() = savedStateHandle["currentPage"] ?: 1
        set(value) { savedStateHandle["currentPage"] = value }

    fun searchMaintenances(page: Int = 1) {
        currentPage = page
        _maintenancesState.value = Resource.Loading()

        // Log parameters for debugging
        Log.d("EntretiensFilterVM", "searchMaintenances called: search='${searchQuery}', status='${selectedStatus}', dateFrom='${dateFrom}', dateTo='${dateTo}', tags='${selectedTags}', minCost='${minCost}', maxCost='${maxCost}', minMileage='${minMileage}', maxMileage='${maxMileage}', sort='${sortField}', order='${sortOrder}', page=$page")

        viewModelScope.launch {
            val result = repository.searchMaintenances(
                search = searchQuery.takeIf { it.isNotBlank() },
                status = selectedStatus,
                dateFrom = dateFrom,
                dateTo = dateTo,
                tags = selectedTags.takeIf { it.isNotEmpty() },
                minCost = minCost,
                maxCost = maxCost,
                minMileage = minMileage,
                maxMileage = maxMileage,
                sort = sortField,
                order = sortOrder,
                page = page
            )
            Log.d("EntretiensFilterVM", "searchMaintenances result: $result")

            // If backend returned success but the search param may not cover all fields,
            // perform a client-side fallback filter across common fields so the UI search
            // behaves as expected for the user.
            if (result is Resource.Success && !searchQuery.isNullOrBlank()) {
                val pageData = result.data
                val q = searchQuery.lowercase(Locale.getDefault())
                val filteredList = pageData?.data?.filter { m ->
                    val combined = listOfNotNull(
                        m.title,
                        m.type,
                        m.notes,
                        m.tags.joinToString(" "),
                        m.status,
                        m.cout.toString(),
                        m.mileage?.toString(),
                        m.ownerId
                    ).joinToString(" ").lowercase(Locale.getDefault())

                    combined.contains(q)
                } ?: emptyList()

                val newPage = PaginatedMaintenancesResponse(
                    data = filteredList,
                    page = 1,
                    limit = filteredList.size.coerceAtLeast(1),
                    total = filteredList.size,
                    totalPages = 1
                )

                _maintenancesState.value = Resource.Success(newPage)
            } else {
                _maintenancesState.value = result
            }
        }
    }

    fun clearFilters() {
        searchQuery = ""
        selectedStatus = null
        dateFrom = null
        dateTo = null
        selectedTags = emptyList()
        minCost = null
        maxCost = null
        minMileage = null
        maxMileage = null
        sortField = "dueAt"
        sortOrder = "asc"
        currentPage = 1
        searchMaintenances()
    }
}

// ViewModel for Widget Data
@Suppress("unused")
class UpcomingMaintenancesWidgetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MaintenanceRepositoryImpl(application)

    private val _upcomingState = MutableStateFlow<Resource<List<UpcomingMaintenanceWidget>>?>(null)
    val upcomingState: StateFlow<Resource<List<UpcomingMaintenanceWidget>>?> = _upcomingState.asStateFlow()

    val upcomingMaintenancesFlow = repository.getUpcomingMaintenancesFlow(5)

    fun refreshUpcomingMaintenances(limit: Int = 5, includePlate: Boolean = true) {
        _upcomingState.value = Resource.Loading()
        viewModelScope.launch {
            val result = repository.getUpcomingMaintenances(limit, includePlate)
            _upcomingState.value = result
        }
    }
}
