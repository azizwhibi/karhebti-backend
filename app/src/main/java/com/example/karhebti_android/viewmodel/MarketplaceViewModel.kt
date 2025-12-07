package com.example.karhebti_android.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.karhebti_android.data.api.*
import com.example.karhebti_android.data.preferences.TokenManager
import com.example.karhebti_android.data.repository.MarketplaceRepository
import com.example.karhebti_android.data.repository.Resource
import kotlinx.coroutines.launch

class MarketplaceViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenManager = TokenManager(application)
    private val repository: MarketplaceRepository by lazy {
        MarketplaceRepository(
            apiService = RetrofitClient.apiService,
            notificationApiService = RetrofitClient.notificationApiService,
            token = tokenManager.getToken() ?: ""
        )
    }

    // Available cars for browsing
    private val _availableCars = MutableLiveData<Resource<List<MarketplaceCarResponse>>>()
    val availableCars: LiveData<Resource<List<MarketplaceCarResponse>>> = _availableCars

    // Swipe result
    private val _swipeResult = MutableLiveData<Resource<SwipeResponse>>()
    val swipeResult: LiveData<Resource<SwipeResponse>> = _swipeResult

    // My swipes
    private val _mySwipes = MutableLiveData<Resource<MySwipesResponse>>()
    val mySwipes: LiveData<Resource<MySwipesResponse>> = _mySwipes

    // Pending swipes (received from buyers)
    private val _pendingSwipes = MutableLiveData<Resource<List<SwipeResponse>>>()
    val pendingSwipes: LiveData<Resource<List<SwipeResponse>>> = _pendingSwipes

    // Swipe response result
    private val _swipeResponseResult = MutableLiveData<Resource<SwipeStatusResponse>>()
    val swipeResponseResult: LiveData<Resource<SwipeStatusResponse>> = _swipeResponseResult

    // Notifications
    private val _notifications = MutableLiveData<Resource<List<NotificationResponse>>>()
    val notifications: LiveData<Resource<List<NotificationResponse>>> = _notifications

    // Real-time notification
    private val _realtimeNotification = MutableLiveData<NotificationResponse>()
    val realtimeNotification: LiveData<NotificationResponse> = _realtimeNotification

    // Unread count
    private val _unreadCount = MutableLiveData<Int>()
    val unreadCount: LiveData<Int> = _unreadCount

    // Car listing result
    private val _listCarResult = MutableLiveData<Resource<MarketplaceCarResponse>>()
    val listCarResult: LiveData<Resource<MarketplaceCarResponse>> = _listCarResult

    // ==================== CARS ====================

    fun loadAvailableCars() {
        viewModelScope.launch {
            _availableCars.value = Resource.Loading()
            _availableCars.value = repository.getAvailableCars()
        }
    }

    fun listCarForSale(carId: String, price: Double, description: String?) {
        viewModelScope.launch {
            _listCarResult.value = Resource.Loading()
            _listCarResult.value = repository.listCarForSale(carId, price, description)
        }
    }

    fun unlistCar(carId: String) {
        viewModelScope.launch {
            _listCarResult.value = Resource.Loading()
            _listCarResult.value = repository.unlistCar(carId)
        }
    }

    // ==================== SWIPES ====================

    fun swipeLeft(carId: String) {
        viewModelScope.launch {
            _swipeResult.value = Resource.Loading()
            _swipeResult.value = repository.createSwipe(carId, "left")
        }
    }

    fun swipeRight(carId: String) {
        viewModelScope.launch {
            _swipeResult.value = Resource.Loading()
            _swipeResult.value = repository.createSwipe(carId, "right")
        }
    }

    fun acceptSwipe(swipeId: String) {
        viewModelScope.launch {
            _swipeResponseResult.value = Resource.Loading()
            _swipeResponseResult.value = repository.acceptSwipe(swipeId)
        }
    }

    fun declineSwipe(swipeId: String) {
        viewModelScope.launch {
            _swipeResponseResult.value = Resource.Loading()
            _swipeResponseResult.value = repository.declineSwipe(swipeId)
        }
    }

    fun loadMySwipes() {
        viewModelScope.launch {
            _mySwipes.value = Resource.Loading()
            _mySwipes.value = repository.getMySwipes()
        }
    }

    fun loadPendingSwipes() {
        viewModelScope.launch {
            _pendingSwipes.value = Resource.Loading()
            _pendingSwipes.value = repository.getPendingSwipes()
        }
    }

    // ==================== NOTIFICATIONS ====================

    fun loadNotifications() {
        viewModelScope.launch {
            _notifications.value = Resource.Loading()
            _notifications.value = repository.getNotifications()
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            val result = repository.getUnreadCount()
            if (result is Resource.Success) {
                _unreadCount.value = result.data?.unreadCount ?: 0
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
            loadNotifications()
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
            loadNotifications()
        }
    }
}
