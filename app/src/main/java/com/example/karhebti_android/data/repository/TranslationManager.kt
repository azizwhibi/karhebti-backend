package com.example.karhebti_android.data.repository

import android.content.Context
import android.util.Log
import com.example.karhebti_android.data.api.Language
import com.example.karhebti_android.data.api.BatchTranslateItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TranslationManager provides a high-level interface for translation operations
 * with automatic caching, offline fallback, and language management
 */
class TranslationManager(
    private val repository: TranslationRepository,
    private val context: Context
) {

    companion object {
        private const val TAG = "TranslationManager"
        private const val PREFS_NAME = "translation_prefs"
        private const val PREF_CURRENT_LANGUAGE = "current_language"
        private const val PREF_LANGUAGE_LIST_UPDATED = "language_list_updated"

        @Volatile
        private var instance: TranslationManager? = null

        fun getInstance(
            repository: TranslationRepository,
            context: Context
        ): TranslationManager {
            return instance ?: synchronized(this) {
                instance ?: TranslationManager(repository, context).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(getCurrentLanguageCode())
    val currentLanguage = _currentLanguage.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<Language>>(emptyList())
    val availableLanguages = _availableLanguages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val translationCache = mutableMapOf<String, Map<String, String>>()

    /**
     * Initialize translation system on app startup
     */
    suspend fun initialize() {
        Log.d(TAG, "Initializing TranslationManager")
        try {
            _isLoading.value = true
            fetchAvailableLanguages()
            loadCurrentLanguageTranslations()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Get or fetch available languages
     */
    suspend fun getLanguages(): List<Language> {
        if (_availableLanguages.value.isNotEmpty()) {
            return _availableLanguages.value
        }
        fetchAvailableLanguages()
        return _availableLanguages.value
    }

    /**
     * Fetch available languages from API with caching
     */
    private suspend fun fetchAvailableLanguages() {
        try {
            val result = repository.getLanguages()
            when (result) {
                is Resource.Success -> {
                    _availableLanguages.value = result.data ?: emptyList()
                    Log.d(TAG, "Fetched ${_availableLanguages.value.size} languages")
                }
                is Resource.Error -> {
                    _error.value = result.message
                    Log.e(TAG, "Error fetching languages: ${result.message}")
                }
                is Resource.Loading -> {}
            }
        } catch (e: Exception) {
            _error.value = "Error: ${e.localizedMessage}"
            Log.e(TAG, "Exception fetching languages", e)
        }
    }

    /**
     * Change current language and load translations
     */
    suspend fun setLanguage(languageCode: String) {
        try {
            _currentLanguage.value = languageCode
            prefs.edit().putString(PREF_CURRENT_LANGUAGE, languageCode).apply()
            loadCurrentLanguageTranslations()
            Log.d(TAG, "Language changed to: $languageCode")
        } catch (e: Exception) {
            _error.value = "Error changing language: ${e.localizedMessage}"
            Log.e(TAG, "Exception setting language", e)
        }
    }

    /**
     * Load translations for current language
     */
    private suspend fun loadCurrentLanguageTranslations() {
        val languageCode = _currentLanguage.value

        // Check cache first
        if (translationCache.containsKey(languageCode)) {
            Log.d(TAG, "Using cached translations for $languageCode")
            return
        }

        // Load from local database
        val localTranslations = repository.getLocalTranslations(languageCode)
        if (localTranslations.isNotEmpty()) {
            translationCache[languageCode] = localTranslations
            Log.d(TAG, "Loaded ${localTranslations.size} translations from local DB")
            return
        }

        // Sync from API if needed
        syncTranslations(languageCode)
    }

    /**
     * Sync translations for a language from API
     */
    suspend fun syncTranslations(languageCode: String) {
        try {
            _isLoading.value = true
            val result = repository.syncOffline(languageCode)

            when (result) {
                is Resource.Success -> {
                    val count = result.data ?: 0
                    val translations = repository.getLocalTranslations(languageCode)
                    translationCache[languageCode] = translations
                    Log.d(TAG, "Synced $count translations for $languageCode")
                }
                is Resource.Error -> {
                    _error.value = result.message
                    Log.e(TAG, "Error syncing translations: ${result.message}")
                }
                is Resource.Loading -> {}
            }
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Get translation for a string using backend (Azure)
     */
    suspend fun translate(
        text: String,
        originalText: String = text,
        languageCode: String? = null
    ): String {
        val targetLanguage = languageCode ?: _currentLanguage.value
        // Always call backend for translation (no key lookup)
        return try {
            val result = repository.translateText(originalText, targetLanguage)
            when (result) {
                is Resource.Success -> result.data ?: originalText
                is Resource.Error -> {
                    Log.w(TAG, "Translation error: ${result.message}, using original text: $originalText")
                    originalText
                }
                is Resource.Loading -> originalText
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during translation", e)
            originalText
        }
    }

    /**
     * Batch translate multiple items and cache them
     */
    suspend fun batchTranslate(
        items: List<BatchTranslateItem>,
        languageCode: String? = null
    ): Map<String, String> {
        val targetLanguage = languageCode ?: _currentLanguage.value

        try {
            _isLoading.value = true
            val result = repository.batchTranslate(items, targetLanguage)

            return when (result) {
                is Resource.Success -> {
                    val translations = result.data ?: emptyMap()
                    // Update cache
                    val map = translationCache.getOrPut(targetLanguage) { mutableMapOf() } as MutableMap
                    map.putAll(translations)
                    translations
                }
                is Resource.Error -> {
                    _error.value = result.message
                    Log.e(TAG, "Batch translation error: ${result.message}")
                    emptyMap()
                }
                is Resource.Loading -> emptyMap()
            }
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Translate single text with fallback
     */
    suspend fun translateText(
        text: String,
        languageCode: String? = null
    ): String {
        val targetLanguage = languageCode ?: _currentLanguage.value

        try {
            val result = repository.translateText(text, targetLanguage)
            return when (result) {
                is Resource.Success -> result.data ?: text
                is Resource.Error -> {
                    Log.w(TAG, "Translation error: ${result.message}, returning original")
                    text
                }
                is Resource.Loading -> text
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during translation", e)
            return text
        }
    }

    /**
     * Get all translations for current language
     */
    suspend fun getCurrentLanguageTranslations(): Map<String, String> {
        return repository.getLocalTranslations(_currentLanguage.value)
    }

    /**
     * Clear cache for a language (admin only)
     */
    suspend fun clearCache(languageCode: String? = null): Boolean {
        try {
            val result = repository.clearCache(languageCode)
            return when (result) {
                is Resource.Success -> {
                    if (languageCode != null) {
                        translationCache.remove(languageCode)
                    } else {
                        translationCache.clear()
                    }
                    true
                }
                is Resource.Error -> {
                    _error.value = result.message
                    false
                }
                is Resource.Loading -> false
            }
        } catch (e: Exception) {
            _error.value = "Error clearing cache: ${e.localizedMessage}"
            return false
        }
    }

    /**
     * Get current language code from preferences
     */
    private fun getCurrentLanguageCode(): String {
        return prefs.getString(PREF_CURRENT_LANGUAGE, "fr") ?: "fr"
    }

    /**
     * Cleanup resources
     */
    fun destroy() {
        scope.cancel()
        translationCache.clear()
    }
}
