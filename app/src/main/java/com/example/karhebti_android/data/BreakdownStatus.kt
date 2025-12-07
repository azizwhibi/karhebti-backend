package com.example.karhebti_android.data

/**
 * Enum représentant les différents statuts d'une panne
 * Compatible avec le backend NestJS
 */
enum class BreakdownStatus(val value: String) {
    PENDING("pending"),           // En attente
    IN_PROGRESS("in_progress"),   // En cours
    ASSIGNED("assigned"),         // Assigné à un garage
    RESOLVED("resolved"),         // Résolu
    CANCELLED("cancelled");       // Annulé

    companion object {
        fun fromString(value: String): BreakdownStatus? {
            return values().find { it.value.equals(value, ignoreCase = true) }
        }
    }
}

/**
 * Extension pour obtenir le libellé en français
 */
fun BreakdownStatus.toFrench(): String {
    return when (this) {
        BreakdownStatus.PENDING -> "En attente"
        BreakdownStatus.IN_PROGRESS -> "En cours"
        BreakdownStatus.ASSIGNED -> "Assigné"
        BreakdownStatus.RESOLVED -> "Résolu"
        BreakdownStatus.CANCELLED -> "Annulé"
    }
}
