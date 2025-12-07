package com.example.karhebti_android.data.websocket

import android.util.Log
import com.example.karhebti_android.data.api.DocumentResponse
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Service pour gérer les notifications WebSocket basées sur les échéances de documents
 * Envoie une notification 3 jours avant l'expiration d'un document
 */
class DocumentExpirationNotificationService {

    companion object {
        private const val DAYS_BEFORE_EXPIRATION = 3
    }

    /**
     * Vérifie si un document doit déclencher une notification d'expiration
     * @param document Le document à vérifier
     * @return true si le document expire dans les 3 prochains jours
     */
    fun shouldNotifyExpiration(document: DocumentResponse): Boolean {
        try {
            val now = Calendar.getInstance()
            val expirationDate = Calendar.getInstance().apply {
                time = document.dateExpiration
            }

            // Calculer la différence en jours
            val diffInMillis = expirationDate.timeInMillis - now.timeInMillis
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

            Log.d("DocumentExpiration",
                "Document ${document.id}: expire dans $diffInDays jours")

            // Retourner true si le document expire dans 3 jours ou moins (mais pas dépassé)
            return diffInDays in 0..DAYS_BEFORE_EXPIRATION
        } catch (e: Exception) {
            Log.e("DocumentExpiration", "Erreur lors de la vérification: ${e.message}", e)
            return false
        }
    }

    /**
     * Crée une notification d'expiration pour un document
     * @param document Le document en train d'expirer
     * @return Map contenant les données de la notification
     */
    fun createExpirationNotification(document: DocumentResponse): Map<String, Any> {
        val now = Calendar.getInstance()
        val expirationDate = Calendar.getInstance().apply {
            time = document.dateExpiration
        }

        val diffInMillis = expirationDate.timeInMillis - now.timeInMillis
        val daysUntilExpiration = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return mapOf(
            "titre" to "Document en train d'expirer",
            "message" to "${document.type} expire dans $daysUntilExpiration jour(s)",
            "type" to "document_expiration",
            "documentId" to document.id,
            "documentType" to document.type,
            "dateExpiration" to document.dateExpiration.time,
            "daysRemaining" to daysUntilExpiration,
            "voiture" to (document.voiture ?: "Non spécifiée"),
            "priority" to if (daysUntilExpiration == 0L) "high" else "medium",
            "timestamp" to System.currentTimeMillis()
        )
    }

    /**
     * Filtre une liste de documents pour obtenir ceux qui expirent bientôt
     * @param documents Liste des documents à vérifier
     * @return Liste des documents expirant dans 3 jours
     */
    fun getDocumentsExpiringWithinThreeDays(
        documents: List<DocumentResponse>
    ): List<DocumentResponse> {
        return documents.filter { shouldNotifyExpiration(it) }
            .sortedBy { it.dateExpiration }
    }

    /**
     * Crée des notifications pour tous les documents expirante bientôt
     * @param documents Liste des documents à vérifier
     * @return Liste des notifications à envoyer
     */
    fun createExpirationNotifications(
        documents: List<DocumentResponse>
    ): List<Map<String, Any>> {
        return getDocumentsExpiringWithinThreeDays(documents)
            .map { createExpirationNotification(it) }
    }

    /**
     * Format une date pour l'affichage
     */
    fun formatExpirationDate(date: Date): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    /**
     * Obtient un message d'alerte personnalisé
     */
    fun getAlertMessage(document: DocumentResponse): String {
        val now = Calendar.getInstance()
        val expirationDate = Calendar.getInstance().apply {
            time = document.dateExpiration
        }

        val diffInMillis = expirationDate.timeInMillis - now.timeInMillis
        val daysUntilExpiration = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return when {
            daysUntilExpiration == 0L -> "URGENT: ${document.type} expire AUJOURD'HUI!"
            daysUntilExpiration == 1L -> "URGENT: ${document.type} expire DEMAIN!"
            else -> "${document.type} expire dans $daysUntilExpiration jours"
        }
    }
}

