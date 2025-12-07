package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.karhebti_android.data.api.BatchTranslateItem
import com.example.karhebti_android.data.api.RetrofitClient
import com.example.karhebti_android.data.database.AppDatabase
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.data.repository.TranslationRepository
import com.example.karhebti_android.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Demo screen showcasing translation features:
 * - Fetching available languages
 * - Translating a single string
 * - Batch translating multiple items
 * - Offline fallback behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationDemoScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appContext = context.applicationContext
    val db = AppDatabase.getInstance(appContext)

    // Translation repository
    val translationRepository = remember {
        TranslationRepository(
            apiService = RetrofitClient.apiService,
            translationDao = db.translationDao(),
            languageCacheDao = db.languageCacheDao(),
            languageListCacheDao = db.languageListCacheDao()
        )
    }

    // Demo state
    var selectedLanguage by remember { mutableStateOf("fr") }
    var textToTranslate by remember { mutableStateOf("Hello world") }
    var translatedText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var demoResult by remember { mutableStateOf("") }

    // Available languages
    var languages by remember { mutableStateOf(emptyList<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Démo Traduction") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Fetch Languages
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "1. Récupérer les langues disponibles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                error = null
                                try {
                                    val result = translationRepository.getLanguages()
                                    when (result) {
                                        is Resource.Success -> {
                                            val langs = result.data ?: emptyList()
                                            languages = langs.map { it.code }
                                            demoResult = "Langues trouvées: ${langs.map { it.nativeName }.joinToString(", ")}"
                                        }
                                        is Resource.Error -> {
                                            error = result.message
                                            demoResult = "Erreur: ${result.message}"
                                        }
                                        is Resource.Loading -> {}
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Récupérer les langues")
                        }
                    }

                    if (languages.isNotEmpty()) {
                        Text(
                            "Langues trouvées: ${languages.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 2: Translate Single Text
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "2. Traduire un texte unique",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = textToTranslate,
                        onValueChange = { textToTranslate = it },
                        label = { Text("Texte à traduire") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                error = null
                                try {
                                    val result = translationRepository.translateText(
                                        text = textToTranslate,
                                        targetLanguage = selectedLanguage
                                    )
                                    when (result) {
                                        is Resource.Success -> {
                                            translatedText = result.data ?: "Pas de traduction"
                                            demoResult = "Traduction réussie: $translatedText"
                                        }
                                        is Resource.Error -> {
                                            error = result.message
                                            demoResult = "Erreur: ${result.message}"
                                        }
                                        is Resource.Loading -> {}
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && textToTranslate.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Traduire en $selectedLanguage")
                        }
                    }

                    if (translatedText.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccentGreen.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "Résultat: $translatedText",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Section 3: Batch Translation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "3. Traduction par lot (Batch)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        "Traduit plusieurs textes avec clés pour offline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                error = null
                                try {
                                    val items = listOf(
                                        BatchTranslateItem("app_name", "My Car"),
                                        BatchTranslateItem("welcome", "Welcome"),
                                        BatchTranslateItem("settings", "Settings"),
                                        BatchTranslateItem("logout", "Logout")
                                    )

                                    val result = translationRepository.batchTranslate(
                                        items = items,
                                        targetLanguage = selectedLanguage
                                    )

                                    when (result) {
                                        is Resource.Success -> {
                                            val translations = result.data ?: emptyMap()
                                            demoResult = buildString {
                                                appendLine("Traductions par lot réussies (${translations.size} éléments):")
                                                translations.forEach { (key, text) ->
                                                    appendLine("  $key: $text")
                                                }
                                            }
                                        }
                                        is Resource.Error -> {
                                            error = result.message
                                            demoResult = "Erreur: ${result.message}"
                                        }
                                        is Resource.Loading -> {}
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.ViewWeek,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Traduction par lot")
                        }
                    }
                }
            }

            // Section 4: Offline Sync
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "4. Synchronisation Offline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        "Télécharge toutes les traductions en cache pour l'utilisation hors ligne",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                error = null
                                try {
                                    val result = translationRepository.syncOffline(selectedLanguage)
                                    when (result) {
                                        is Resource.Success -> {
                                            val count = result.data ?: 0
                                            demoResult = "Synchronisation réussie: $count traductions téléchargées pour $selectedLanguage"
                                        }
                                        is Resource.Error -> {
                                            error = result.message
                                            demoResult = "Erreur: ${result.message}"
                                        }
                                        is Resource.Loading -> {}
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Synchroniser offline")
                        }
                    }
                }
            }

            // Demo Result Display
            if (demoResult.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (error != null) AlertRed.copy(alpha = 0.1f) else AccentGreen.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Résultat de la démo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (error != null) AlertRed else AccentGreen
                        )
                        Text(
                            demoResult,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
