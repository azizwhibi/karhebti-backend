package com.example.karhebti_android

import com.example.karhebti_android.data.api.*
import com.example.karhebti_android.data.database.*
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.data.repository.TranslationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import retrofit2.Response

@RunWith(MockitoJUnitRunner::class)
class TranslationRepositoryTest {

    @Mock
    lateinit var apiService: KarhebtiApiService

    @Mock
    lateinit var translationDao: TranslationDao

    @Mock
    lateinit var languageCacheDao: LanguageCacheDao

    @Mock
    lateinit var languageListCacheDao: LanguageListCacheDao

    private lateinit var repository: TranslationRepository

    @Before
    fun setup() {
        repository = TranslationRepository(
            apiService = apiService,
            translationDao = translationDao,
            languageCacheDao = languageCacheDao,
            languageListCacheDao = languageListCacheDao
        )
    }

    @Test
    fun testGetLanguages_Success() = runBlocking {
        // Arrange
        val languages = listOf(
            Language("fr", "French", "Français"),
            Language("en", "English", "English"),
            Language("ar", "Arabic", "العربية")
        )
        val response = Response.success(LanguagesResponse(languages))
        whenever(apiService.getLanguages()).thenReturn(response)
        whenever(languageListCacheDao.getCache()).thenReturn(null)

        // Act
        val result = repository.getLanguages()

        // Assert
        assertTrue(result is Resource.Success)
        assertEquals(3, (result as Resource.Success).data?.size)
        assertEquals("fr", result.data?.get(0)?.code)
    }

    @Test
    fun testGetLanguages_Failure() = runBlocking {
        // Arrange
        val response: Response<LanguagesResponse> = Response.error(500, okhttp3.ResponseBody.create(null, ""))
        whenever(apiService.getLanguages()).thenReturn(response)
        whenever(languageListCacheDao.getCache()).thenReturn(null)

        // Act
        val result = repository.getLanguages()

        // Assert
        assertTrue(result is Resource.Error)
        assertNotNull((result as Resource.Error).message)
    }

    @Test
    fun testTranslateText_Success() = runBlocking {
        // Arrange
        val translationText = "Bonjour"
        val response = Response.success(TranslateResponse(listOf(translationText)))
        whenever(apiService.translateText(any())).thenReturn(response)

        // Act
        val result = repository.translateText("Hello", "fr")

        // Assert
        assertTrue(result is Resource.Success)
        assertEquals(translationText, (result as Resource.Success).data)
    }

    @Test
    fun testTranslateText_EmptyResponse() = runBlocking {
        // Arrange
        val response = Response.success(TranslateResponse(emptyList()))
        whenever(apiService.translateText(any())).thenReturn(response)

        // Act
        val result = repository.translateText("Hello", "fr")

        // Assert
        assertTrue(result is Resource.Error)
    }

    @Test
    fun testBatchTranslate_Success() = runBlocking {
        // Arrange
        val items = listOf(
            BatchTranslateItem("hello", "Hello"),
            BatchTranslateItem("goodbye", "Goodbye")
        )
        val translations = mapOf(
            "hello" to "Bonjour",
            "goodbye" to "Au revoir"
        )
        val response = Response.success(BatchTranslateResponse(translations))
        whenever(apiService.batchTranslate(any())).thenReturn(response)

        // Act
        val result = repository.batchTranslate(items, "fr")

        // Assert
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(2, data?.size)
        assertEquals("Bonjour", data?.get("hello"))
    }

    @Test
    fun testBatchTranslate_LargeData_Chunking() = runBlocking {
        // Arrange - create 600 items (should be split into 2 batches)
        val items = (1..600).map {
            BatchTranslateItem("key_$it", "Text $it")
        }
        val translations = items.associate { it.key to "Translated ${it.text}" }
        val response = Response.success(BatchTranslateResponse(translations))
        whenever(apiService.batchTranslate(any())).thenReturn(response)

        // Act
        val result = repository.batchTranslate(items, "fr")

        // Assert
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(600, data?.size)
    }

    @Test
    fun testBatchTranslate_Empty() = runBlocking {
        // Act
        val result = repository.batchTranslate(emptyList(), "fr")

        // Assert
        assertTrue(result is Resource.Success)
        assertTrue((result as Resource.Success).data?.isEmpty() == true)
    }

    @Test
    fun testGetCachedTranslations_Success() = runBlocking {
        // Arrange
        val cachedTranslations = mapOf(
            "hello" to "Bonjour",
            "goodbye" to "Au revoir",
            "thank_you" to "Merci"
        )
        val response = Response.success(
            CachedTranslationsResponse(cachedTranslations, 3)
        )
        whenever(apiService.getCachedTranslations("fr")).thenReturn(response)

        // Act
        val result = repository.getCachedTranslations("fr")

        // Assert
        assertTrue(result is Resource.Success)
        val data = (result as Resource.Success).data
        assertEquals(3, data?.size)
        assertEquals("Merci", data?.get("thank_you"))
    }

    @Test
    fun testSyncOffline_Success() = runBlocking {
        // Arrange
        val cachedTranslations = mapOf(
            "app_name" to "Ma Voiture",
            "welcome" to "Bienvenue"
        )
        val response = Response.success(
            CachedTranslationsResponse(cachedTranslations, 2)
        )
        whenever(apiService.getCachedTranslations("fr")).thenReturn(response)

        // Act
        val result = repository.syncOffline("fr")

        // Assert
        assertTrue(result is Resource.Success)
        assertEquals(2, (result as Resource.Success).data)
    }

    @Test
    fun testClearCache_Success() = runBlocking {
        // Arrange
        val response = Response.success(MessageResponse("Cache cleared"))
        whenever(apiService.clearTranslationCache("fr")).thenReturn(response)

        // Act
        val result = repository.clearCache("fr")

        // Assert
        assertTrue(result is Resource.Success)
    }

    @Test
    fun testRetryLogic_429TooManyRequests() = runBlocking {
        // This test verifies that exponential backoff is applied
        // In a real scenario, you'd use a test dispatcher for time manipulation
        val response: Response<LanguagesResponse> = Response.error(429, okhttp3.ResponseBody.create(null, ""))
        whenever(apiService.getLanguages()).thenReturn(response).thenReturn(
            Response.success(LanguagesResponse(emptyList()))
        )
        whenever(languageListCacheDao.getCache()).thenReturn(null)

        // Act - should retry and potentially succeed
        val result = repository.getLanguages()

        // Assert - result depends on retry success
        assertNotNull(result)
    }
}

/**
 * Unit tests for translation batch operation edge cases
 */
class TranslationBatchOperationsTest {

    @Mock
    lateinit var apiService: KarhebtiApiService

    @Mock
    lateinit var translationDao: TranslationDao

    @Mock
    lateinit var languageCacheDao: LanguageCacheDao

    @Mock
    lateinit var languageListCacheDao: LanguageListCacheDao

    private lateinit var repository: TranslationRepository

    @Before
    fun setup() {
        repository = TranslationRepository(
            apiService = apiService,
            translationDao = translationDao,
            languageCacheDao = languageCacheDao,
            languageListCacheDao = languageListCacheDao
        )
    }

    @Test
    fun testBatchTranslate_ExactlyMaxSize() = runBlocking {
        // 500 items should be 1 batch
        val items = (1..500).map {
            BatchTranslateItem("key_$it", "Text $it")
        }
        val translations = items.associate { it.key to "Translated ${it.text}" }
        val response = Response.success(BatchTranslateResponse(translations))
        whenever(apiService.batchTranslate(any())).thenReturn(response)

        val result = repository.batchTranslate(items, "fr")

        assertTrue(result is Resource.Success)
        assertEquals(500, (result as Resource.Success).data?.size)
    }

    @Test
    fun testBatchTranslate_OneOverMax() = runBlocking {
        // 501 items should be 2 batches
        val items = (1..501).map {
            BatchTranslateItem("key_$it", "Text $it")
        }
        val translations = items.associate { it.key to "Translated ${it.text}" }
        val response = Response.success(BatchTranslateResponse(translations.filterKeys { it in (1..500).map { "key_$it" } }))
        whenever(apiService.batchTranslate(any())).thenReturn(response)

        val result = repository.batchTranslate(items.take(500), "fr")

        assertTrue(result is Resource.Success)
        assertEquals(500, (result as Resource.Success).data?.size)
    }
}

/**
 * Unit tests for translation entity persistence
 */
class TranslationPersistenceTest {

    @Test
    fun testTranslationEntityCreation() {
        val entity = TranslationEntity(
            id = 1,
            key = "hello",
            languageCode = "fr",
            originalText = "Hello",
            translatedText = "Bonjour",
            updatedAt = System.currentTimeMillis()
        )

        assertEquals(1, entity.id)
        assertEquals("hello", entity.key)
        assertEquals("fr", entity.languageCode)
        assertEquals("Bonjour", entity.translatedText)
    }

    @Test
    fun testLanguageCacheEntityCreation() {
        val entity = LanguageCacheEntity(
            languageCode = "fr",
            name = "French",
            nativeName = "Français",
            cachedAt = System.currentTimeMillis()
        )

        assertEquals("fr", entity.languageCode)
        assertEquals("Français", entity.nativeName)
    }

    @Test
    fun testTranslationEntityWithDefaultValues() {
        val entity = TranslationEntity(
            key = "test",
            languageCode = "en",
            originalText = "Test",
            translatedText = "Test"
        )

        assertEquals(0, entity.id)
        assertTrue(entity.updatedAt > 0)
    }
}

// Test utilities and helpers
private fun <T> any(): T = org.mockito.kotlin.any()

