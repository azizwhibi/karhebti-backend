package com.example.karhebti_android.data.models

/**
 * Enum pour les types de documents
 * Synchronisé avec le backend
 */
enum class DocumentType(val displayName: String) {
    ASSURANCE("Assurance"),
    PERMIS_CONDUIRE("Permis de conduire"),
    CARTE_GRISE("Carte grise"),
    VISITE_TECHNIQUE("Visite technique");

    companion object {
        fun fromString(value: String): DocumentType? {
            return values().find { it.displayName == value }
        }
    }
}

