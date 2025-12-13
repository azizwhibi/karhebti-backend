package com.example.karhebti_android.data.model

import java.util.Date

data class Reclamation(
    val id: String,
    val titre: String,
    val message: String,
    val type: String,
    val garage: Garage? = null,
    val service: Service? = null,
    val createdAt: Date? = null
)
