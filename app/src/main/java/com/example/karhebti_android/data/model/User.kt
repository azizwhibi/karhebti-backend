package com.example.karhebti_android.data.model

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String? = null,
    val avatarUrl: String? = null,
    val isPremium: Boolean = false,
    val memberSince: String,
    val emailVerified: Boolean = false,
    val preferences: UserPreferences = UserPreferences()
)

data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val language: String = "fr",
    val twoFactorEnabled: Boolean = false
)
