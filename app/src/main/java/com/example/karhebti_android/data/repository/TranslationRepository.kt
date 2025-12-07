package com.example.karhebti_android.data.repository

import android.util.Log
import com.example.karhebti_android.data.api.*
import com.example.karhebti_android.data.database.*
import com.example.karhebti_android.data.database.TranslationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Response
import kotlin.math.min

class TranslationRepository(
    private val apiService: KarhebtiApiService = RetrofitClient.apiService,
    private val translationDao: TranslationDao,
    private val languageCacheDao: LanguageCacheDao,
    private val languageListCacheDao: LanguageListCacheDao
) {

    companion object {
        private const val TAG = "TranslationRepository"
        private const val MAX_BATCH_SIZE = 500
        private const val LANGUAGE_CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 1000L // 1 second
    }

    /**
     * Get available languages with caching
     */
    suspend fun getLanguages(): Resource<List<Language>> = withContext(Dispatchers.IO) {
        try {
            val cacheData = languageListCacheDao.getCache()
            val now = System.currentTimeMillis()

            // Check if cache is still valid
            if (cacheData != null && cacheData.cacheExpiryAt > now) {
                Log.d(TAG, "Using cached languages list")
                val cachedLanguages = languageCacheDao.getAllLanguagesSync()
                return@withContext Resource.Success(
                    cachedLanguages.map {
                        Language(it.languageCode, it.name, it.nativeName)
                    }
                )
            }

            // Fetch from API with retry logic
            val response = retryWithExponentialBackoff {
                apiService.getLanguages()
            }

            if (response.isSuccessful && response.body() != null) {
                val languages = response.body()!!.languages

                // Cache languages
                val languageEntities = languages.map {
                    LanguageCacheEntity(
                        languageCode = it.code,
                        name = it.name,
                        nativeName = it.nativeName,
                        cachedAt = now
                    )
                }
                languageCacheDao.deleteAllLanguages()
                languageCacheDao.insertLanguages(languageEntities)

                // Update cache expiry
                val expiryTime = now + LANGUAGE_CACHE_EXPIRY_MS
                languageListCacheDao.insertCache(
                    LanguageListCacheEntity(
                        lastFetchedAt = now,
                        cacheExpiryAt = expiryTime
                    )
                )

                Log.d(TAG, "Fetched ${languages.size} languages from API")
                Resource.Success(languages)
            } else {
                Resource.Error("Erreur API: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching languages", e)
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    /**
     * Translate a single string
     */
    suspend fun translateText(
        text: String,
        targetLanguage: String,
        sourceLanguage: String? = null
    ): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val request = TranslateRequest(
                text = listOf(text),
                targetLanguage = targetLanguage,
                sourceLanguage = sourceLanguage
            )

            val response = retryWithExponentialBackoff {
                apiService.translateText(request)
            }

            if (response.isSuccessful && response.body() != null) {
                val translations = response.body()!!.translations
                if (translations.isNotEmpty()) {
                    Resource.Success(translations[0])
                } else {
                    Resource.Error("Translation response empty")
                }
            } else {
                Resource.Error("Erreur API: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error translating text", e)
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    /**
     * Batch translate multiple strings with key mapping
     * Handles chunking for requests > 500 items
     */
    suspend fun batchTranslate(
        items: List<BatchTranslateItem>,
        targetLanguage: String,
        sourceLanguage: String? = null
    ): Resource<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            if (items.isEmpty()) {
                return@withContext Resource.Success(emptyMap())
            }

            // Chunk into max 500 items per request
            val chunks = items.chunked(MAX_BATCH_SIZE)
            val results = mutableMapOf<String, String>()

            for ((index, chunk) in chunks.withIndex()) {
                Log.d(TAG, "Processing chunk ${index + 1}/${chunks.size} with ${chunk.size} items")

                val request = BatchTranslateRequest(
                    items = chunk,
                    targetLanguage = targetLanguage,
                    sourceLanguage = sourceLanguage
                )

                val response = retryWithExponentialBackoff {
                    apiService.batchTranslate(request)
                }

                if (response.isSuccessful && response.body() != null) {
                    results.putAll(response.body()!!.translations)
                } else {
                    Log.w(TAG, "Chunk $index failed with code ${response.code()}")
                    return@withContext Resource.Error("Erreur API: ${response.code()}")
                }
            }

            // Save to local database
            val entities = results.map { (key: String, text: String) ->
                TranslationEntity(
                    key = key,
                    languageCode = targetLanguage,
                    originalText = items.find { it.key == key }?.text ?: "",
                    translatedText = text,
                    updatedAt = System.currentTimeMillis()
                )
            }
            translationDao.insertTranslations(entities)

            Log.d(TAG, "Successfully translated ${results.size} items")
            Resource.Success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Error in batch translation", e)
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    /**
     * Get cached translations for offline use
     */
    suspend fun getCachedTranslations(languageCode: String): Resource<Map<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                val response = retryWithExponentialBackoff {
                    apiService.getCachedTranslations(languageCode)
                }

                if (response.isSuccessful && response.body() != null) {
                    val cached = response.body()!!
                    Log.d(TAG, "Downloaded ${cached.count} cached translations for $languageCode")
                    Resource.Success(cached.translations)
                } else {
                    Resource.Error("Erreur API: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching cached translations", e)
                Resource.Error("Erreur réseau: ${e.localizedMessage}")
            }
        }

    /**
     * Sync offline translations for a specific language
     * Downloads all cached translations and stores locally
     */
    suspend fun syncOffline(languageCode: String): Resource<Int> = withContext(Dispatchers.IO) {
        try {
            val cachedResult = getCachedTranslations(languageCode)

            return@withContext when (cachedResult) {
                is Resource.Success -> {
                    val translations = cachedResult.data ?: emptyMap()

                    // Save to local database
                    val entities = translations.map { (key: String, text: String) ->
                        TranslationEntity(
                            key = key,
                            languageCode = languageCode,
                            originalText = key, // Use key as fallback original
                            translatedText = text,
                            updatedAt = System.currentTimeMillis()
                        )
                    }

                    translationDao.deleteTranslationsByLanguage(languageCode)
                    translationDao.insertTranslations(entities)

                    Log.d(TAG, "Synced "+entities.size+" translations for "+languageCode)
                    Resource.Success(entities.size)
                }
                is Resource.Error -> Resource.Error<Int>(cachedResult.message ?: "Unknown error")
                is Resource.Loading -> Resource.Loading<Int>()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing offline translations", e)
            Resource.Error("Erreur réseau: ${e.localizedMessage}")
        }
    }

    /**
     * Get translation from local database
     */
    suspend fun getLocalTranslation(key: String, languageCode: String): String? {
        return translationDao.getTranslatedText(key, languageCode)
    }

    /**
     * Get all translations for a language from local database
     */
    suspend fun getLocalTranslations(languageCode: String): Map<String, String> {
        val entities = translationDao.getTranslationsByLanguagePaginated(
            languageCode = languageCode,
            limit = Int.MAX_VALUE,
            offset = 0
        )
        return entities.associate { it: TranslationEntity -> it.key to it.translatedText }
    }

    /**
     * Clear cache for a specific language (admin only)
     */
    suspend fun clearCache(languageCode: String? = null): Resource<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = retryWithExponentialBackoff {
                    apiService.clearTranslationCache(languageCode)
                }

                if (response.isSuccessful && response.body() != null) {
                    // Also clear local cache
                    if (languageCode != null) {
                        translationDao.deleteTranslationsByLanguage(languageCode)
                    } else {
                        translationDao.deleteAllTranslations()
                    }

                    Log.d(TAG, "Cache cleared for language: $languageCode")
                    Resource.Success("Cache cleared successfully")
                } else {
                    Resource.Error("Erreur API: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cache", e)
                Resource.Error("Erreur réseau: ${e.localizedMessage}")
            }
        }

    /**
     * Retry logic with exponential backoff for 429 and 5xx errors
     */
    private suspend inline fun <T> retryWithExponentialBackoff(
        crossinline block: suspend () -> Response<T>
    ): Response<T> {
        var currentBackoff = INITIAL_BACKOFF_MS

        repeat(MAX_RETRIES) { attempt ->
            try {
                val response = block()

                // Retry on 429 (Too Many Requests) or 5xx errors
                if (response.code() == 429 || response.code() in 500..599) {
                    if (attempt < MAX_RETRIES - 1) {
                        Log.w(TAG, "Rate limited or server error (${response.code()}), retrying in ${currentBackoff}ms...")
                        delay(currentBackoff)
                        currentBackoff = (currentBackoff * 2).coerceAtMost(30000L) // Cap at 30 seconds
                        return@repeat
                    }
                }

                return response
            } catch (e: Exception) {
                if (attempt < MAX_RETRIES - 1) {
                    Log.w(TAG, "Request failed, retrying in ${currentBackoff}ms...", e)
                    delay(currentBackoff)
                    currentBackoff = (currentBackoff * 2).coerceAtMost(30000L)
                } else {
                    throw e
                }
            }
        }

        return block()
    }
}
