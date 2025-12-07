package com.example.karhebti_android.data.api

import java.io.Serializable

// Pending signup data stored in SavedStateHandle between navigation steps
data class SignupData(
    val nom: String,
    val prenom: String,
    val email: String,
    val telephone: String,
    val password: String
) : Serializable

