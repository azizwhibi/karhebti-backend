package com.example.karhebti_android.data

// Data class pour la création d'une panne (SOS)
// NOTE: Le backend idéalement extrait l'utilisateur depuis le JWT. Toutefois certains
// backends valident encore la présence de `userId` dans le payload. Pour assurer
// compatibilité, on expose un champ optionnel `userId: String?` — le frontend ne
// l'enverra que si on peut l'extraire proprement du token.
data class CreateBreakdownRequest(
    val vehicleId: String? = null, // Optionnel

    val type: String,
    val description: String? = null,
    val latitude: Double,
    val longitude: Double,

    val photo: String? = null, // URL ou chemin serveur. Ne pas envoyer content:// URIs

    val userId: String? = null // facultatif - rempli depuis le token si disponible
)
