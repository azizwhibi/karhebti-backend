package com.example.karhebti_android.data

import com.google.gson.annotations.SerializedName

/**
 * Wrapper pour gérer différents formats de réponse du backend
 *
 * Cas 1: {"data": [...]}
 * Cas 2: {"breakdowns": [...]}
 * Cas 3: Directement [...]
 */
data class BreakdownsListResponse(
    @SerializedName("data")
    val data: List<BreakdownResponse>? = null,

    @SerializedName("breakdowns")
    val breakdowns: List<BreakdownResponse>? = null,

    @SerializedName("success")
    val success: Boolean? = null
) {
    /**
     * Retourne la liste des breakdowns peu importe le format
     */
    fun extractBreakdowns(): List<BreakdownResponse> {
        return data ?: breakdowns ?: emptyList()
    }
}

