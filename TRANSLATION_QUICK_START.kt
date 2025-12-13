// Quick Integration Guide - Translation System

// ============================================================
// STEP 1: Update your Room Database
// ============================================================

// In your AppDatabase.kt or equivalent

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.karhebti_android.data.database.*

@Database(
    entities = [
        TranslationEntity::class,
        LanguageCacheEntity::class,
        LanguageListCacheEntity::class,
        // ... your existing entities
    ],
    version = 2  // Increment version if migrating
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao
    abstract fun languageCacheDao(): LanguageCacheDao
    abstract fun languageListCacheDao(): LanguageListCacheDao

    // ... your existing DAOs

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "karhebti_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


// ============================================================
// STEP 2: Initialize TranslationManager in your Application
// ============================================================

// In your KarhebtiApplication.kt or MainActivity.kt

import com.example.karhebti_android.data.repository.TranslationRepository
import com.example.karhebti_android.data.repository.TranslationManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class KarhebtiApplication : Application() {

    companion object {
        private var translationManager: TranslationManager? = null

        fun getTranslationManager(context: Context): TranslationManager {
            if (translationManager == null) {
                val database = AppDatabase.getDatabase(context)
                val repository = TranslationRepository(
                    apiService = RetrofitClient.apiService,
                    translationDao = database.translationDao(),
                    languageCacheDao = database.languageCacheDao(),
                    languageListCacheDao = database.languageListCacheDao()
                )
                translationManager = TranslationManager.getInstance(
                    repository = repository,
                    context = context
                )
            }
            return translationManager!!
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize RetrofitClient
        RetrofitClient.initialize(this)

        // Initialize TokenManager
        TokenManager.getInstance(this).initializeToken()

        // Initialize TranslationManager
        val translationManager = getTranslationManager(this)
        GlobalScope.launch {
            translationManager.initialize()
        }
    }
}


// ============================================================
// STEP 3: Use in Compose Screens
// ============================================================

// In your UI screens

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.karhebti_android.data.repository.TranslationManager

@Composable
fun MyScreen() {
    val context = LocalContext.current
    val translationManager = remember {
        (context.applicationContext as KarhebtiApplication)
            .getTranslationManager(context)
    }

    val currentLanguage by translationManager.currentLanguage.collectAsStateWithLifecycle()
    val isLoading by translationManager.isLoading.collectAsStateWithLifecycle()

    // Use translated text with fallback
    var welcomeText by remember { mutableStateOf("Welcome") }

    LaunchedEffect(currentLanguage) {
        welcomeText = translationManager.translate(
            key = "welcome",
            originalText = "Welcome"
        )
    }

    Column {
        Text(welcomeText)
    }
}


// ============================================================
// STEP 4: Replace Settings Screen Usage
// ============================================================

// In your Navigation or MainActivity

import com.example.karhebti_android.ui.screens.SettingsScreenWithTranslation

navController.navigate("settings") {
    // Use SettingsScreenWithTranslation instead of SettingsScreen
    // It includes dynamic language selection dialog
}


// ============================================================
// SAMPLE: Translating App Strings
// ============================================================

// Create a translations map for your app

data class AppStrings(
    val appName: String = "Ma Voiture",
    val welcomeMessage: String = "Bienvenue",
    val homeTitle: String = "Accueil",
    val settingsTitle: String = "Paramètres",
    val logoutLabel: String = "Déconnexion",
    val profileTitle: String = "Profil",
    val emailLabel: String = "Email",
    val phoneLabel: String = "Téléphone",
    val languageLabel: String = "Langue",
    val notificationsLabel: String = "Notifications",
    val securityLabel: String = "Sécurité",
    val changePasswordLabel: String = "Changer mot de passe",
    val supportLabel: String = "Support"
)

// Usage in Compose
@Composable
fun LocalizedApp(translationManager: TranslationManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var appStrings by remember { mutableStateOf(AppStrings()) }

    LaunchedEffect(Unit) {
        scope.launch {
            val items = listOf(
                BatchTranslateItem("app_name", "My Vehicle"),
                BatchTranslateItem("welcome", "Welcome"),
                BatchTranslateItem("home", "Home"),
                BatchTranslateItem("settings", "Settings"),
                // ... more strings
            )

            val translations = translationManager.batchTranslate(items)

            appStrings = AppStrings(
                appName = translations["app_name"] ?: "My Vehicle",
                welcomeMessage = translations["welcome"] ?: "Welcome",
                homeTitle = translations["home"] ?: "Home",
                settingsTitle = translations["settings"] ?: "Settings",
                // ... map all strings
            )
        }
    }

    // Now use appStrings throughout your app
    Column {
        Text(appStrings.appName)
        Text(appStrings.welcomeMessage)
        // ...
    }
}


// ============================================================
// STEP 5: Handle Language Changes
// ============================================================

@Composable
fun LanguageChangeHandler(translationManager: TranslationManager) {
    val scope = rememberCoroutineScope()

    // When user selects a new language
    fun onLanguageSelected(languageCode: String) {
        scope.launch {
            translationManager.setLanguage(languageCode)

            // Sync translations for offline use
            translationManager.syncTranslations(languageCode)

            // UI will automatically update via Flow
        }
    }

    return onLanguageSelected
}


// ============================================================
// STEP 6: Test with Demo Screen
// ============================================================

// Add to your navigation to test all features

import com.example.karhebti_android.ui.screens.TranslationDemoScreen

// In your NavHost or navigation setup:
composable("translation_demo") {
    TranslationDemoScreen(
        onBackClick = { navController.popBackStack() }
    )
}

// Features demonstrated:
// - Fetching languages
// - Translating single text
// - Batch translation
// - Offline sync


// ============================================================
// STEP 7: Running Tests
// ============================================================

// Terminal commands:

// Run all translation tests
// ./gradlew test --tests TranslationRepositoryTest

// Run specific test
// ./gradlew test --tests TranslationRepositoryTest.testBatchTranslate_Success

// Run with logging
// ./gradlew test --tests TranslationRepositoryTest -i

// Run all tests
// ./gradlew test


// ============================================================
// COMMON PATTERNS
// ============================================================

// Pattern 1: Simple text translation with fallback
suspend fun getTranslatedText(
    translationManager: TranslationManager,
    key: String,
    defaultText: String
): String {
    return translationManager.translate(
        key = key,
        originalText = defaultText
    )
}

// Usage:
val translated = getTranslatedText(tm, "welcome", "Welcome")


// Pattern 2: Batch translate UI labels
suspend fun translateUILabels(
    translationManager: TranslationManager,
    labels: Map<String, String>
): Map<String, String> {
    val items = labels.map { (key, text) ->
        BatchTranslateItem(key, text)
    }
    return translationManager.batchTranslate(items)
}

// Usage:
val uiLabels = mapOf(
    "home" to "Home",
    "settings" to "Settings",
    "logout" to "Logout"
)
val translated = translateUILabels(tm, uiLabels)


// Pattern 3: Handle language selection in settings
@Composable
fun LanguageSettingsItem(
    translationManager: TranslationManager,
    currentLanguage: String
) {
    val scope = rememberCoroutineScope()

    SettingsItem(
        icon = Icons.Default.Language,
        title = "Langue",
        subtitle = currentLanguage.uppercase(),
        onClick = {
            // Show language picker dialog
            scope.launch {
                val languages = translationManager.getLanguages()
                // Show dialog with languages
            }
        }
    )
}


// Pattern 4: Offline fallback
@Composable
fun OfflineAwareText(
    translationManager: TranslationManager,
    key: String,
    defaultText: String
) {
    var displayText by remember { mutableStateOf(defaultText) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key) {
        scope.launch {
            try {
                displayText = translationManager.translate(
                    key = key,
                    originalText = defaultText
                )
            } catch (e: Exception) {
                // Offline or error - use default text
                displayText = defaultText
            }
        }
    }

    Text(displayText)
}


// Pattern 5: Preload translations on app start
suspend fun preloadAppTranslations(
    translationManager: TranslationManager,
    languageCode: String
) {
    try {
        // Download all cached translations for offline use
        translationManager.syncTranslations(languageCode)
        Log.d("Translation", "Preloaded translations for $languageCode")
    } catch (e: Exception) {
        Log.e("Translation", "Failed to preload translations", e)
    }
}


// ============================================================
// DEBUGGING & LOGGING
// ============================================================

// Enable detailed logging in TranslationRepository

// In your Retrofit setup (ApiConfig.kt):
val logging = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY  // Full request/response logging
}

val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(logging)
    .addInterceptor(authInterceptor)
    .build()

// Check translation repository logs
// adb logcat | grep "TranslationRepository"
// adb logcat | grep "TranslationManager"


// ============================================================
// PERFORMANCE TIPS
// ============================================================

// 1. Use batchTranslate for multiple items instead of loop
// GOOD:
val translations = translationManager.batchTranslate(items)

// BAD:
val translations = items.associateBy({ it.key }) {
    translationManager.translateText(it.text)  // Multiple API calls!
}

// 2. Cache translations in your UI state
@Composable
fun MyScreen() {
    val translationManager = getTranslationManager(context)
    val cachedTranslations = remember { mutableMapOf<String, String>() }

    fun getTranslation(key: String): String {
        return cachedTranslations.getOrPut(key) {
            // Fetch on-demand
            runBlocking {
                translationManager.translate(key, key)
            }
        }
    }
}

// 3. Sync offline translations once per language
// GOOD:
scope.launch {
    translationManager.setLanguage("fr")
    translationManager.syncTranslations("fr")  // Once
}

// BAD:
scope.launch {
    repeat(10) {
        translationManager.syncTranslations("fr")  // 10 times!
    }
}


// ============================================================
// TROUBLESHOOTING CHECKLIST
// ============================================================

/*
❌ Translations not loading?
   ✅ Check network: adb shell nc -zv 10.0.2.2 3000
   ✅ Verify API endpoint: POST /api/translation/translate
   ✅ Check auth token: Log interceptor should show Authorization header
   ✅ Review error logs: adb logcat | grep Translation

❌ Batch translation slow?
   ✅ Check item count: Should not exceed 500 per request
   ✅ Verify chunking: Repository splits automatically
   ✅ Monitor network: Look for timeout errors

❌ Offline translations missing?
   ✅ Call syncOffline() after language selection
   ✅ Check database: Query translations table
   ✅ Verify cache expiry: 24-hour cache on language list

❌ Memory issues with many translations?
   ✅ Use pagination: translationDao.getTranslationsByLanguagePaginated()
   ✅ Clear old cache: Clear cache for unused languages
   ✅ Monitor memory: Use Android Profiler
*/

