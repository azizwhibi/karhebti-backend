package com.example.karhebti_android.data.ocr

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Service OCR pour extraire les informations des documents scannés
 * Utilise OCR.space API (gratuit jusqu'à 25 000 requêtes/mois)
 */
class OCRApiService {

    companion object {
        private const val TAG = "OCRApiService"

        // API OCR.space - Clé API gratuite (à remplacer par votre clé)
        private const val OCR_API_KEY = "K88448389788957" // Clé de démonstration
        private const val OCR_API_URL = "https://api.ocr.space/parse/image"

        // Types de documents reconnus
        const val DOC_TYPE_CARTE_GRISE = "Carte Grise"
        const val DOC_TYPE_PERMIS_CONDUIRE = "Permis de Conduire"
        const val DOC_TYPE_ASSURANCE = "Assurance"
        const val DOC_TYPE_CONTROLE_TECHNIQUE = "Contrôle Technique"
        const val DOC_TYPE_VIGNETTE = "Vignette"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Scanner un document et extraire les informations
     */
    suspend fun scanDocument(imageFile: File): OCRResult {
        return try {
            Log.d(TAG, "🔍 Démarrage du scan OCR pour: ${imageFile.name}")

            // Créer la requête multipart
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("apikey", OCR_API_KEY)
                .addFormDataPart("language", "fre") // Français
                .addFormDataPart("isOverlayRequired", "false")
                .addFormDataPart("detectOrientation", "true")
                .addFormDataPart("scale", "true")
                .addFormDataPart("isTable", "true")
                .addFormDataPart("OCREngine", "2") // Moteur 2 : Meilleur pour les chiffres et zones de texte
                .addFormDataPart(
                    "file",
                    imageFile.name,
                    imageFile.asRequestBody("image/*".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url(OCR_API_URL)
                .post(requestBody)
                .build()

            // Exécuter la requête
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "❌ Erreur OCR API: ${response.code}")
                return OCRResult.Error("Erreur lors du scan: ${response.code}")
            }

            Log.d(TAG, "✅ Réponse OCR reçue")

            // Parser la réponse JSON
            val jsonResponse = JSONObject(responseBody)
            val parsedResults = jsonResponse.optJSONArray("ParsedResults")

            if (parsedResults == null || parsedResults.length() == 0) {
                return OCRResult.Error("Aucun texte détecté dans l'image")
            }

            val firstResult = parsedResults.getJSONObject(0)
            val extractedText = firstResult.optString("ParsedText", "")

            if (extractedText.isEmpty()) {
                return OCRResult.Error("Texte extrait vide")
            }

            Log.d(TAG, "📄 Texte extrait (${extractedText.length} caractères)")

            // Extraire les informations du document
            val extractedData = extractDocumentInfo(extractedText)

            OCRResult.Success(extractedData)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors du scan OCR: ${e.message}", e)
            OCRResult.Error("Erreur: ${e.message}")
        }
    }

    /**
     * Extraire les informations structurées du texte OCR
     */
    private fun extractDocumentInfo(text: String): ExtractedDocumentData {
        Log.d(TAG, "🔍 Extraction des informations du texte...")
        Log.d(TAG, "📝 Texte brut reçu:\n$text")

        val lines = text.lines().filter { it.isNotBlank() }
        val textLower = text.lowercase()

        // --- PATTERNS ---
        
        // Immatriculation
        val immatriculationPatterns = listOf(
            Regex("""\d{1,3}\s?TU\s?\d{1,4}""", RegexOption.IGNORE_CASE),
            Regex("""\d{1,3}\s?TUN\s?\d{1,4}""", RegexOption.IGNORE_CASE),
            Regex("""[A-Z]{2}-\d{3}-[A-Z]{2}"""),
            Regex("""\d{1,4}\s?[A-Z]{2,3}\s?\d{2,3}""")
        )

        // Dates : 
        // 1. Standard : JJ/MM/AAAA (avec divers séparateurs)
        // 2. Compact YYYYMMDD : 20120222
        // 3. Compact DDMMYYYY : 22022012
        val datePatternStandard = Regex("""\b(\d{1,2})[-/. ](\d{1,2})[-/. ](\d{2,4})\b""")
        val datePatternCompactYearFirst = Regex("""\b(19|20)(\d{2})(\d{2})(\d{2})\b""") // YYYYMMDD
        val datePatternCompactDayFirst = Regex("""\b(\d{2})(\d{2})(19|20)(\d{2})\b""") // DDMMYYYY

        // Numéro de document
        // 1. Avec préfixe
        val numeroPatternPrefix = Regex("""(N[°o]?|Num|Ref)\s*[:.]?\s*([A-Z0-9]{5,20})""", RegexOption.IGNORE_CASE)
        // 2. Isolé (8 chiffres ou plus) - Risqué mais utile si l'OCR rate le préfixe
        val numeroPatternIsolated = Regex("""\b\d{8,15}\b""")

        // --- EXTRACTION ---

        var documentType = ""
        var documentNumber = ""
        var issuedDate = ""
        var expiryDate = ""
        var holderName = ""
        var immatriculation = ""

        // 1. Détection du Type
        documentType = when {
            textLower.contains("carte grise") || textLower.contains("certificat d'immatriculation") || textLower.contains("immatriculation") -> DOC_TYPE_CARTE_GRISE
            textLower.contains("permis de conduire") || textLower.contains("driving licence") || textLower.contains("permis") -> DOC_TYPE_PERMIS_CONDUIRE
            textLower.contains("assurance") || textLower.contains("police") || textLower.contains("carte internationale") -> DOC_TYPE_ASSURANCE
            textLower.contains("contrôle technique") || textLower.contains("visite technique") || textLower.contains("procès-verbal") -> DOC_TYPE_CONTROLE_TECHNIQUE
            textLower.contains("vignette") || textLower.contains("taxe") || textLower.contains("quittance") -> DOC_TYPE_VIGNETTE
            else -> "Document"
        }

        // 2. Extraction Immatriculation
        for (pattern in immatriculationPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                immatriculation = match.value.trim().uppercase()
                break
            }
        }

        // 3. Extraction Numéro Document
        val matchNumPrefix = numeroPatternPrefix.find(text)
        if (matchNumPrefix != null) {
            documentNumber = matchNumPrefix.groupValues[2].trim()
        } else {
            // Essai pattern isolé
            val matchNumIsolated = numeroPatternIsolated.find(text)
            // On vérifie que ce n'est pas une date (ne commence pas par 19 ou 20 si 8 chiffres)
            if (matchNumIsolated != null) {
                val num = matchNumIsolated.value
                if (!num.startsWith("19") && !num.startsWith("20")) {
                    documentNumber = num
                }
            }
        }

        // 4. Extraction Dates
        val extractedDates = mutableListOf<String>()
        
        // Standard
        datePatternStandard.findAll(text).forEach { 
            val (day, month, year) = it.destructured
            val fullYear = if (year.length == 2) "20$year" else year
            extractedDates.add("$day/$month/$fullYear")
        }
        
        // Compact YYYYMMDD (ex: 20120222)
        datePatternCompactYearFirst.findAll(text).forEach {
            val (century, year, month, day) = it.destructured
            extractedDates.add("$day/$month/$century$year")
        }

        // Compact DDMMYYYY
        datePatternCompactDayFirst.findAll(text).forEach {
            val (day, month, century, year) = it.destructured
            extractedDates.add("$day/$month/$century$year")
        }

        Log.d(TAG, "📅 Dates trouvées: $extractedDates")

        when (documentType) {
            DOC_TYPE_CARTE_GRISE -> {
                if (extractedDates.isNotEmpty()) {
                    issuedDate = extractedDates.first()
                    expiryDate = "" 
                }
            }
            DOC_TYPE_VIGNETTE, DOC_TYPE_ASSURANCE -> {
                if (extractedDates.size >= 2) {
                    issuedDate = extractedDates.first()
                    expiryDate = extractedDates.last()
                } else if (extractedDates.isNotEmpty()) {
                    issuedDate = extractedDates.first()
                }
            }
            else -> {
                if (extractedDates.isNotEmpty()) {
                    issuedDate = extractedDates.first()
                    if (extractedDates.size > 1) expiryDate = extractedDates.last()
                }
            }
        }

        // 5. Extraction Nom
        if (lines.size >= 2) {
            val nameRegex = Regex("""^[A-Z]{3,}\s+[A-Z]{3,}(\s+[A-Z]{3,})?$""")
            holderName = lines.take(10).find { line ->
                val cleanLine = line.trim()
                !cleanLine.contains(Regex("""\d""")) &&
                cleanLine.length > 5 &&
                (nameRegex.matches(cleanLine) || cleanLine.startsWith("M. ") || cleanLine.startsWith("MME "))
            } ?: ""
        }

        Log.d(TAG, """
            ✅ Informations extraites (V2):
            - Type: $documentType
            - Numéro: $documentNumber
            - Immatriculation: $immatriculation
            - Date émission: $issuedDate
            - Date expiration: $expiryDate
            - Titulaire: $holderName
        """.trimIndent())

        return ExtractedDocumentData(
            documentType = documentType,
            documentNumber = documentNumber,
            issuedDate = issuedDate,
            expiryDate = expiryDate,
            holderName = holderName,
            immatriculation = immatriculation,
            rawText = text
        )
    }
}

/**
 * Résultat du scan OCR
 */
sealed class OCRResult {
    data class Success(val data: ExtractedDocumentData) : OCRResult()
    data class Error(val message: String) : OCRResult()
}

/**
 * Données extraites du document
 */
data class ExtractedDocumentData(
    val documentType: String,
    val documentNumber: String,
    val issuedDate: String,
    val expiryDate: String,
    val holderName: String,
    val immatriculation: String,
    val rawText: String
)

