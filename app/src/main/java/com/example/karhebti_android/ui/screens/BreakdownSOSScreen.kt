@file:Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "UNUSED_VALUE")

package com.example.karhebti_android.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import coil.compose.AsyncImage
import com.example.karhebti_android.data.preferences.TokenManager
import com.example.karhebti_android.network.BreakdownsApi
import com.example.karhebti_android.repository.BreakdownsRepository
import com.example.karhebti_android.ui.components.OpenStreetMapView
import com.example.karhebti_android.ui.theme.RedSOS
import com.example.karhebti_android.utils.LocationSettingsHelper
import com.example.karhebti_android.viewmodel.BreakdownUiState
import com.example.karhebti_android.viewmodel.BreakdownViewModel
import com.example.karhebti_android.viewmodel.BreakdownViewModelFactory
import com.google.android.gms.location.*
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakdownSOSScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSOSSuccess: (breakdownId: String?, type: String, lat: Double, lon: Double) -> Unit = { _, _, _, _ -> }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // États du flux SOS
    var currentStep by remember { mutableStateOf(SOSStep.CHECKING_PERMISSION) }
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    // Setup ViewModel avec AuthInterceptor
    val retrofit = remember {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(com.example.karhebti_android.data.api.AuthInterceptor(context))
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api = remember { retrofit.create(BreakdownsApi::class.java) }
    val repo = remember { BreakdownsRepository(api) }
    val factory = remember { BreakdownViewModelFactory(repo) }
    val viewModel: BreakdownViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()
    var lastRequestJson by remember { mutableStateOf<String?>(null) }
    var lastError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // États du formulaire
    var type by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showTypeMenu by remember { mutableStateOf(false) }

    // États de localisation
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var isManualLocation by remember { mutableStateOf(false) }

    val types = listOf("PNEU", "BATTERIE", "MOTEUR", "CARBURANT", "REMORQUAGE", "AUTRE")

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Image picker
    var photoUri by remember { mutableStateOf<String?>(null) }
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> photoUri = uri?.toString() }
    )

    // Vérification permission GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                if (LocationSettingsHelper.isGPSEnabled(context)) {
                    currentStep = SOSStep.FETCHING_LOCATION
                    fetchLocation(
                        context = context,
                        fusedLocationClient = fusedLocationClient,
                        onLocation = { lat, lon ->
                            latitude = lat
                            longitude = lon
                            locationError = null
                            currentStep = SOSStep.SHOWING_MAP
                        },
                        onError = { err ->
                            locationError = err
                            currentStep = SOSStep.GPS_ERROR
                        }
                    )
                } else {
                    currentStep = SOSStep.GPS_DISABLED
                }
            } else {
                currentStep = SOSStep.PERMISSION_DENIED
            }
        }
    )

    // Launcher pour les paramètres de localisation
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (LocationSettingsHelper.isGPSEnabled(context)) {
            currentStep = SOSStep.FETCHING_LOCATION
            fetchLocation(
                context = context,
                fusedLocationClient = fusedLocationClient,
                onLocation = { lat, lon ->
                    latitude = lat
                    longitude = lon
                    locationError = null
                    currentStep = SOSStep.SHOWING_MAP
                },
                onError = { err ->
                    locationError = err
                    currentStep = SOSStep.GPS_ERROR
                }
            )
        } else {
            currentStep = SOSStep.GPS_DISABLED
        }
    }

    // Vérification initiale au lancement
    LaunchedEffect(Unit) {
        val hasPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            if (LocationSettingsHelper.isGPSEnabled(context)) {
                currentStep = SOSStep.FETCHING_LOCATION
                fetchLocation(
                    context = context,
                    fusedLocationClient = fusedLocationClient,
                    onLocation = { lat, lon ->
                        latitude = lat
                        longitude = lon
                        locationError = null
                        currentStep = SOSStep.SHOWING_MAP
                    },
                    onError = { err ->
                        locationError = err
                        currentStep = SOSStep.GPS_ERROR
                    }
                )
            } else {
                currentStep = SOSStep.GPS_DISABLED
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Gérer le succès de l'envoi
    LaunchedEffect(uiState) {
        if (uiState is BreakdownUiState.Success) {
            val response = (uiState as BreakdownUiState.Success).data as com.example.karhebti_android.data.BreakdownResponse
            onSOSSuccess(response.id, type, latitude ?: 0.0, longitude ?: 0.0)
        }
    }

    // Dialogue de confirmation
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = RedSOS,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Confirmer la demande SOS")
            },
            text = {
                Column {
                    Text("Vous êtes sur le point d'envoyer une demande d'assistance.")
                    Spacer(Modifier.height(8.dp))
                    Text("• Type: $type", fontWeight = FontWeight.Medium)
                    if (description.isNotBlank()) {
                        Text("• Description: $description")
                    }
                    Text("• Position: ${latitude?.format(4)}, ${longitude?.format(4)}")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Un technicien sera notifié et se dirigera vers votre position.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false

                        val tokenNow = TokenManager.getInstance(context).getToken()
                        if (tokenNow.isNullOrBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Erreur : utilisateur non identifié. Veuillez vous reconnecter.")
                            }
                            return@Button
                        }

                        val normalizedType = type
                        val photoLocal = photoUri
                        val normalizedPhoto = if (photoLocal != null && (photoLocal.startsWith("http") || photoLocal.startsWith("/uploads") || photoLocal.startsWith("file://"))) {
                            photoLocal
                        } else {
                            null
                        }

                        val request = com.example.karhebti_android.data.CreateBreakdownRequest(
                            vehicleId = null,
                            type = normalizedType,
                            description = description.takeIf { it.isNotBlank() },
                            latitude = latitude!!,
                            longitude = longitude!!,
                            photo = normalizedPhoto
                        )

                        lastRequestJson = try { Gson().toJson(request) } catch (_: Exception) { null }
                        viewModel.declareBreakdown(request)
                    }
                ) {
                    Text("Confirmer et envoyer")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SOS - Assistance routière") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.History, contentDescription = "Historique")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentStep) {
                SOSStep.CHECKING_PERMISSION -> {
                    LoadingStep(message = "Vérification des permissions...")
                }

                SOSStep.PERMISSION_DENIED -> {
                    ErrorStep(
                        icon = Icons.Default.LocationOff,
                        title = "Permission refusée",
                        message = "L'accès à la localisation est nécessaire pour utiliser le service SOS. Veuillez accorder la permission dans les paramètres de l'application.",
                        actionLabel = "Réessayer",
                        onAction = {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        onCancel = onBackClick
                    )
                }

                SOSStep.GPS_DISABLED -> {
                    ErrorStep(
                        icon = Icons.Default.GpsOff,
                        title = "GPS désactivé",
                        message = "Le GPS doit être activé pour utiliser le service SOS. Veuillez l'activer dans les paramètres.",
                        actionLabel = "Ouvrir les paramètres",
                        onAction = {
                            locationSettingsLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        },
                        onCancel = onBackClick
                    )
                }

                SOSStep.FETCHING_LOCATION -> {
                    LoadingStep(message = "Récupération de votre position...")
                }

                SOSStep.GPS_ERROR -> {
                    ErrorStep(
                        icon = Icons.Default.TouchApp,
                        title = "🗺️ Choisissez votre position",
                        message = "Le GPS n'est pas disponible, mais pas de problème !\n\n" +
                                "✅ SOLUTION RAPIDE :\n" +
                                "Appuyez sur le bouton ci-dessous pour ouvrir la carte.\n\n" +
                                "📍 Vous pourrez ensuite :\n" +
                                "• Toucher n'importe où sur la carte\n" +
                                "• Le marqueur rouge se déplacera instantanément\n" +
                                "• Vos coordonnées seront mises à jour\n\n" +
                                "💡 C'est aussi simple que ça !\n\n" +
                                "──────────────────\n\n" +
                                "ℹ️ Conseil : Si vous utilisez un émulateur,\n" +
                                "vous pouvez aussi définir votre position dans :\n" +
                                "Menu (⋮) > Location",
                        actionLabel = "📍 Réessayer GPS",
                        onAction = {
                            currentStep = SOSStep.FETCHING_LOCATION
                            fetchLocation(
                                context = context,
                                fusedLocationClient = fusedLocationClient,
                                onLocation = { lat, lon ->
                                    latitude = lat
                                    longitude = lon
                                    locationError = null
                                    currentStep = SOSStep.SHOWING_MAP
                                },
                                onError = { err ->
                                    locationError = err
                                    currentStep = SOSStep.GPS_ERROR
                                }
                            )
                        },
                        onSkipToManual = {
                            // Use default position (Tunis, Tunisia) and let user select manually
                            latitude = 36.8065
                            longitude = 10.1815
                            isManualLocation = true
                            locationError = null
                            currentStep = SOSStep.SHOWING_MAP
                        },
                        onCancel = onBackClick
                    )
                }

                SOSStep.SHOWING_MAP -> {
                    val currentToken = readAnyToken(context)
                    val tokenMasked = currentToken?.let { t ->
                        if (t.length <= 10) t else t.take(6) + "..." + t.takeLast(4)
                    }

                    SOSFormContent(
                        latitude = latitude,
                        longitude = longitude,
                        type = type,
                        description = description,
                        photoUri = photoUri,
                        types = types,
                        showTypeMenu = showTypeMenu,
                        isManualLocation = isManualLocation,
                        onTypeChange = { type = it },
                        onTypeMenuChange = { showTypeMenu = it },
                        onDescriptionChange = { description = it },
                        onPhotoClick = { pickImageLauncher.launch("image/*") },
                        onSendClick = { showConfirmDialog = true },
                        onRefreshLocation = {
                            isManualLocation = false
                            currentStep = SOSStep.FETCHING_LOCATION
                            fetchLocation(
                                context = context,
                                fusedLocationClient = fusedLocationClient,
                                onLocation = { lat, lon ->
                                    latitude = lat
                                    longitude = lon
                                    locationError = null
                                    currentStep = SOSStep.SHOWING_MAP
                                },
                                onError = { err ->
                                    locationError = err
                                    currentStep = SOSStep.GPS_ERROR
                                }
                            )
                        },
                        onLocationSelected = { lat, lon ->
                            latitude = lat
                            longitude = lon
                            isManualLocation = true
                        },
                        sendEnabled = type.isNotBlank() && latitude != null && longitude != null,
                        userId = TokenManager.getInstance(context).getUser()?.id,
                        tokenPresent = !currentToken.isNullOrBlank(),
                        tokenMasked = tokenMasked,
                        lastRequestJson = lastRequestJson,
                        lastError = lastError
                    )
                }
            }

            // Afficher le loader pendant l'envoi
            if (uiState is BreakdownUiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Envoi de la demande SOS...")
                        }
                    }
                }
            }
        }
    }

    // Gestion des erreurs
    LaunchedEffect(uiState) {
        when (uiState) {
            is BreakdownUiState.Error -> {
                val msg = (uiState as BreakdownUiState.Error).message
                lastError = msg
                snackbarHostState.showSnackbar("Erreur : $msg")
            }
            else -> {}
        }
    }
}

enum class SOSStep {
    CHECKING_PERMISSION,
    PERMISSION_DENIED,
    GPS_DISABLED,
    FETCHING_LOCATION,
    GPS_ERROR,
    SHOWING_MAP
}

@Composable
fun LoadingStep(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    onCancel: () -> Unit,
    onSkipToManual: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = RedSOS,
                modifier = Modifier.size(80.dp)
            )

            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionLabel, modifier = Modifier.padding(vertical = 4.dp))
            }

            // Add "Skip to manual selection" button for GPS errors
            onSkipToManual?.let { skipAction ->
                Button(
                    onClick = skipAction,
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "👆 JE CHOISIS MA POSITION",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "sur la carte",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "👆 Recommandé : Plus rapide et précis",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(0.8f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Annuler")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSFormContent(
    latitude: Double?,
    longitude: Double?,
    type: String,
    description: String,
    photoUri: String?,
    types: List<String>,
    showTypeMenu: Boolean,
    isManualLocation: Boolean,
    onTypeChange: (String) -> Unit,
    onTypeMenuChange: (Boolean) -> Unit,
    tokenPresent: Boolean,
    tokenMasked: String?,
    onDescriptionChange: (String) -> Unit,
    onPhotoClick: () -> Unit,
    onSendClick: () -> Unit,
    onRefreshLocation: () -> Unit,
    onLocationSelected: (Double, Double) -> Unit,
    sendEnabled: Boolean,
    userId: String?,
    lastRequestJson: String?,
    lastError: String?
) {
    val scrollState = rememberScrollState()
    var showValidation by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Bouton SOS principal
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(RedSOS, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "SOS",
                tint = Color.White,
                modifier = Modifier.size(50.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Carte OpenStreetMap
        if (latitude != null && longitude != null) {
            // 🎯 INSTRUCTION CARD - More prominent guidance
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated icon
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "TOUCHEZ LA CARTE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "pour choisir votre position exacte",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "📍",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Le marqueur rouge se déplacera\ninstantanément où vous touchez",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Indication du mode de localisation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isManualLocation)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (isManualLocation) Icons.Default.CheckCircle else Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = if (isManualLocation)
                            MaterialTheme.colorScheme.onTertiaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isManualLocation)
                                "Position manuelle"
                            else
                                "Position GPS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isManualLocation)
                                MaterialTheme.colorScheme.onTertiaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            if (isManualLocation)
                                "✅ Vous avez choisi cette position"
                            else
                                "📡 Détectée automatiquement",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isManualLocation)
                                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box {
                    OpenStreetMapView(
                        latitude = latitude,
                        longitude = longitude,
                        zoom = 15.0,
                        markerTitle = if (isManualLocation) "📍 Position choisie" else "📍 Votre position GPS",
                        onLocationSelected = onLocationSelected
                    )

                    // Hint overlay for first-time users
                    if (!isManualLocation) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "Touchez ici pour ajuster",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Lat: ${latitude.format(4)}, Lon: ${longitude.format(4)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onRefreshLocation
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Actualiser avec GPS",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Type de problème
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = type,
                onValueChange = {},
                readOnly = true,
                label = { Text("Type de problème *") },
                placeholder = { Text("Choisir...") },
                trailingIcon = {
                    IconButton(onClick = { onTypeMenuChange(!showTypeMenu) }) {
                        Icon(
                            imageVector = if (showTypeMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isError = showValidation && type.isBlank()
            )

            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .clickable {
                        Log.d("BreakdownSOS", "Type field tapped - opening menu")
                        onTypeMenuChange(true)
                    }
            )

            if (showTypeMenu) {
                AlertDialog(
                    onDismissRequest = { onTypeMenuChange(false) },
                    title = { Text("Sélectionner le type de panne") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            types.forEach { typeOption ->
                                TextButton(
                                    onClick = {
                                        Log.d("BreakdownSOS", "Type selected from dialog: $typeOption")
                                        onTypeChange(typeOption)
                                        onTypeMenuChange(false)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(typeOption, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { onTypeMenuChange(false) }) { Text("Fermer") }
                    }
                )
            }

            if (showValidation && type.isBlank()) {
                Text(
                    text = "⚠️ Veuillez sélectionner un type de panne",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description (optionnel)") },
            placeholder = { Text("Décrivez le problème...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            minLines = 2
        )

        Spacer(Modifier.height(16.dp))

        // Photo
        OutlinedButton(
            onClick = onPhotoClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (photoUri == null) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ajouter une photo (optionnel)")
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Photo sélectionnée",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Photo sélectionnée", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Bouton Envoyer
        Button(
            onClick = {
                if (!sendEnabled) {
                    showValidation = true
                    coroutineScope.launch { scrollState.animateScrollTo(0) }
                } else {
                    onSendClick()
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (sendEnabled) RedSOS else Color.Gray,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Envoyer la demande SOS", modifier = Modifier.padding(vertical = 4.dp))
        }

        if (showValidation && !sendEnabled) {
            val missingFields = buildList {
                if (type.isBlank()) add("Type de panne")
                if (latitude == null || longitude == null) add("Localisation GPS")
            }

            Text(
                text = "⚠️ Champs manquants : ${missingFields.joinToString(", ")}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        LaunchedEffect(type, latitude, longitude) {
            if (type.isNotBlank() && latitude != null && longitude != null) showValidation = false
        }

        Spacer(Modifier.height(32.dp))

        // Debug panel
        if (lastRequestJson != null || lastError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    lastRequestJson?.let { req ->
                        Text("Dernière requête:", style = MaterialTheme.typography.labelSmall)
                        Text(req, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                    }
                    lastError?.let { err ->
                        Text("Dernière erreur:", style = MaterialTheme.typography.labelSmall)
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text("État du token:", style = MaterialTheme.typography.labelSmall)
                    Text(
                        if (tokenPresent) "Présent" else "Absent",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (tokenPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    tokenMasked?.let {
                        Text("Token masqué:", style = MaterialTheme.typography.labelSmall)
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun readAnyToken(context: Context): String? {
    try {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                "secret_shared_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val tokenEnc = encryptedPrefs.getString("jwt_token", null)
            if (!tokenEnc.isNullOrBlank()) {
                return tokenEnc
            }
        } catch (e: Exception) {
            Log.w("BreakdownSOS", "readAnyToken: encrypted prefs read failed: ${e.message}")
        }

        try {
            val plainPrefs = context.getSharedPreferences("secret_shared_prefs", Context.MODE_PRIVATE)
            val tokenPlain = plainPrefs.getString("jwt_token", null)
            if (!tokenPlain.isNullOrBlank()) {
                return tokenPlain
            }
        } catch (e: Exception) {
            Log.w("BreakdownSOS", "readAnyToken: plain prefs read failed: ${e.message}")
        }

        val tmToken = TokenManager.getInstance(context).getToken()
        if (!tmToken.isNullOrBlank()) {
            return tmToken
        }

        return null
    } catch (e: Exception) {
        Log.e("BreakdownSOS", "readAnyToken: unexpected error: ${e.message}", e)
        return TokenManager.getInstance(context).getToken()
    }
}

@SuppressLint("MissingPermission")
private fun fetchLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocation: (Double, Double) -> Unit,
    onError: (String) -> Unit
) {
    val hasPermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) {
        onError("Permission de localisation non accordée")
        return
    }

    // Check if GPS is enabled
    if (!LocationSettingsHelper.isGPSEnabled(context)) {
        onError("GPS désactivé. Veuillez l'activer dans les paramètres.")
        return
    }

    Log.d("BreakdownSOS", "Starting location request...")

    var timeoutHandler: android.os.Handler? = null
    var hasReceivedLocation = false

    // Create a timeout handler (15 seconds)
    timeoutHandler = android.os.Handler(Looper.getMainLooper())
    val timeoutRunnable = Runnable {
        if (!hasReceivedLocation) {
            Log.e("BreakdownSOS", "Location request timed out")
            // Try to get last known location as fallback
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null && !hasReceivedLocation) {
                    Log.w("BreakdownSOS", "Using last known location: lat=${lastLoc.latitude}, lon=${lastLoc.longitude}")
                    hasReceivedLocation = true
                    onLocation(lastLoc.latitude, lastLoc.longitude)
                } else {
                    onError("Impossible d'obtenir votre position. Sur émulateur : utilisez Extended Controls > Location pour définir une position. Sur appareil réel : assurez-vous d'être à l'extérieur avec le GPS activé.")
                }
            }
        }
    }
    timeoutHandler.postDelayed(timeoutRunnable, 15000) // 15 seconds timeout

    // Try multiple strategies in parallel for best results

    // Strategy 1: Get last known location immediately (fastest, but may be old)
    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
        if (lastLoc != null && !hasReceivedLocation) {
            val age = System.currentTimeMillis() - lastLoc.time
            Log.d("BreakdownSOS", "Last known location: lat=${lastLoc.latitude}, lon=${lastLoc.longitude}, age=${age}ms")

            // If location is recent (less than 2 minutes old), use it immediately
            if (age < 120000) {
                hasReceivedLocation = true
                timeoutHandler?.removeCallbacks(timeoutRunnable)
                Log.d("BreakdownSOS", "Using recent last known location")
                onLocation(lastLoc.latitude, lastLoc.longitude)
            }
        }
    }

    // Strategy 2: Request fresh location with balanced priority (works better in emulators)
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 2000)
        .setMinUpdateIntervalMillis(1000)
        .setMaxUpdateDelayMillis(5000)
        .setMaxUpdates(3) // Get a few updates to improve accuracy
        .build()

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val loc = locationResult.lastLocation
            if (loc != null && !hasReceivedLocation) {
                Log.d("BreakdownSOS", "Fresh location received: lat=${loc.latitude}, lon=${loc.longitude}, accuracy=${loc.accuracy}m")
                hasReceivedLocation = true
                timeoutHandler?.removeCallbacks(timeoutRunnable)
                fusedLocationClient.removeLocationUpdates(this)
                onLocation(loc.latitude, loc.longitude)
            }
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
            super.onLocationAvailability(availability)
            Log.d("BreakdownSOS", "Location availability: ${availability.isLocationAvailable}")
        }
    }

    // Start location updates
    fusedLocationClient.requestLocationUpdates(
        locationRequest,
        locationCallback,
        Looper.getMainLooper()
    ).addOnFailureListener { exception ->
        Log.e("BreakdownSOS", "Failed to request location updates: ${exception.message}")
        if (!hasReceivedLocation) {
            timeoutHandler?.removeCallbacks(timeoutRunnable)
            // Try last known location as final fallback
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null && !hasReceivedLocation) {
                    hasReceivedLocation = true
                    Log.w("BreakdownSOS", "Using last known location as fallback")
                    onLocation(lastLoc.latitude, lastLoc.longitude)
                } else {
                    onError("Erreur de localisation: ${exception.message}. Sur émulateur, définissez une position via Extended Controls.")
                }
            }
        }
    }

    // Strategy 3: Try getCurrentLocation for modern devices
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
        .addOnSuccessListener { currentLoc ->
            if (currentLoc != null && !hasReceivedLocation) {
                Log.d("BreakdownSOS", "Current location obtained: lat=${currentLoc.latitude}, lon=${currentLoc.longitude}")
                hasReceivedLocation = true
                timeoutHandler?.removeCallbacks(timeoutRunnable)
                fusedLocationClient.removeLocationUpdates(locationCallback)
                onLocation(currentLoc.latitude, currentLoc.longitude)
            }
        }
        .addOnFailureListener { exception ->
            Log.w("BreakdownSOS", "getCurrentLocation failed: ${exception.message}, waiting for updates...")
        }
}

fun Double.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}
