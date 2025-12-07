package com.example.karhebti_android.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.karhebti_android.data.ocr.ExtractedDocumentData
import com.example.karhebti_android.data.ocr.OCRApiService
import com.example.karhebti_android.data.ocr.OCRResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel pour gérer le scan OCR des documents
 */
class OCRViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "OCRViewModel"
    }

    private val ocrService = OCRApiService()

    // États pour le scan OCR
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _extractedData = MutableStateFlow<ExtractedDocumentData?>(null)
    val extractedData: StateFlow<ExtractedDocumentData?> = _extractedData.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    /**
     * Scanner un document à partir d'une image
     */
    fun scanDocument(imageFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔍 Début du scan: ${imageFile.name}")

                _scanState.value = ScanState.Scanning
                _progress.value = 0.3f // 30% - Préparation

                // Appeler l'API OCR
                val result = ocrService.scanDocument(imageFile)
                _progress.value = 0.7f // 70% - Scan terminé

                when (result) {
                    is OCRResult.Success -> {
                        _progress.value = 1f // 100% - Extraction terminée
                        _extractedData.value = result.data
                        _scanState.value = ScanState.Success(result.data)
                        Log.d(TAG, "✅ Scan réussi")
                    }
                    is OCRResult.Error -> {
                        _progress.value = 0f
                        _scanState.value = ScanState.Error(result.message)
                        Log.e(TAG, "❌ Erreur scan: ${result.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors du scan: ${e.message}", e)
                _progress.value = 0f
                _scanState.value = ScanState.Error("Erreur: ${e.message}")
            }
        }
    }

    /**
     * Réinitialiser l'état du scan
     */
    fun resetScan() {
        _scanState.value = ScanState.Idle
        _extractedData.value = null
        _progress.value = 0f
    }

    /**
     * Mettre à jour les données extraites manuellement
     */
    fun updateExtractedData(data: ExtractedDocumentData) {
        _extractedData.value = data
    }
}

/**
 * États possibles du scan OCR
 */
sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Success(val data: ExtractedDocumentData) : ScanState()
    data class Error(val message: String) : ScanState()
}

