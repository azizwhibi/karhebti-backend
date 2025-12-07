package com.example.karhebti_android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.BreakdownResponse
import com.example.karhebti_android.network.BreakdownsApi
import com.example.karhebti_android.repository.BreakdownsRepository
import com.example.karhebti_android.ui.theme.RedSOS
import com.example.karhebti_android.viewmodel.BreakdownViewModel
import com.example.karhebti_android.viewmodel.BreakdownViewModelFactory
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 🚨 Garage Breakdown Details Screen
 *
 * This screen is shown to garage owners when they receive a breakdown notification.
 *
 * Features:
 * - Display breakdown type and description
 * - Show client location on interactive map
 * - Calculate distance from garage to breakdown
 * - Display client contact information
 * - Accept/Refuse buttons
 * - Confirmation dialogs
 * - Success/Error handling
 *
 * Flow:
 * 1. Garage owner taps notification
 * 2. This screen opens with breakdown details
 * 3. Garage owner reviews information
 * 4. Garage owner clicks Accept or Refuse
 * 5. Confirmation dialog appears
 * 6. On confirm, status updates in backend
 * 7. Success: Navigate to navigation screen
 *    Failure: Show error message
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageBreakdownDetailsScreen(
    breakdownId: String,
    onBackClick: () -> Unit,
    onAcceptSuccess: (BreakdownResponse) -> Unit = {},
    onRefuseSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Setup ViewModel
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

    // State
    var breakdown by remember { mutableStateOf<BreakdownResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAcceptDialog by remember { mutableStateOf(false) }
    var showRefuseDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    // Load breakdown details
    LaunchedEffect(breakdownId) {
        try {
            val result = viewModel.getBreakdownStatus(breakdownId)
            result.onSuccess {
                breakdown = it
                isLoading = false
            }.onFailure {
                error = it.message
                isLoading = false
            }
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    // Accept handler
    fun handleAccept() {
        isProcessing = true
        coroutineScope.launch {
            try {
                android.util.Log.d("GarageBreakdownDetails", "Accepting breakdown: $breakdownId")

                // Call accept API with String ID - collect the Flow
                repo.acceptBreakdown(breakdownId).collect { result ->
                    result.onSuccess { acceptedBreakdown ->
                        android.util.Log.d("GarageBreakdownDetails", "✅ Breakdown accepted: ${acceptedBreakdown.id}")
                        onAcceptSuccess(acceptedBreakdown)
                        snackbarHostState.showSnackbar("Demande acceptée avec succès!")
                    }.onFailure { error ->
                        android.util.Log.e("GarageBreakdownDetails", "❌ Error accepting: ${error.message}", error)
                        snackbarHostState.showSnackbar("Erreur: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GarageBreakdownDetails", "❌ Exception: ${e.message}", e)
                snackbarHostState.showSnackbar("Erreur: ${e.message}")
            } finally {
                isProcessing = false
                showAcceptDialog = false
            }
        }
    }

    // Refuse handler
    fun handleRefuse() {
        isProcessing = true
        coroutineScope.launch {
            try {
                android.util.Log.d("GarageBreakdownDetails", "Refusing breakdown: $breakdownId")

                // Call refuse API with String ID - collect the Flow
                repo.refuseBreakdown(breakdownId).collect { result ->
                    result.onSuccess {
                        android.util.Log.d("GarageBreakdownDetails", "ℹ️ Breakdown refused: $breakdownId")
                        onRefuseSuccess()
                        snackbarHostState.showSnackbar("Demande refusée")
                    }.onFailure { error ->
                        android.util.Log.e("GarageBreakdownDetails", "❌ Error refusing: ${error.message}", error)
                        snackbarHostState.showSnackbar("Erreur: ${error.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GarageBreakdownDetails", "❌ Exception: ${e.message}", e)
                snackbarHostState.showSnackbar("Erreur: ${e.message}")
            } finally {
                isProcessing = false
                showRefuseDialog = false
            }
        }
    }

    // Accept Confirmation Dialog
    if (showAcceptDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showAcceptDialog = false },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Accepter cette demande SOS?")
            },
            text = {
                Column {
                    Text("En acceptant, vous vous engagez à:")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Vous rendre sur place dans les plus brefs délais")
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Apporter le matériel nécessaire (${breakdown?.type ?: ""})")
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Contacter le client si besoin")
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "⏱️ Temps estimé: ${breakdown?.let { calculateETA(it) }} minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { handleAccept() },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Confirmer")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAcceptDialog = false },
                    enabled = !isProcessing
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    // Refuse Confirmation Dialog
    if (showRefuseDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showRefuseDialog = false },
            icon = {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = null,
                    tint = RedSOS,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Refuser cette demande?")
            },
            text = {
                Column {
                    Text("Êtes-vous sûr de vouloir refuser cette demande d'assistance?")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Le système cherchera automatiquement un autre garage disponible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { handleRefuse() },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedSOS
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Refuser")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRefuseDialog = false },
                    enabled = !isProcessing
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚨 Demande SOS") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RedSOS,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    LoadingContent()
                }
                error != null -> {
                    ErrorContent(
                        message = error!!,
                        onRetry = {
                            isLoading = true
                            error = null
                            coroutineScope.launch {
                                val result = viewModel.getBreakdownStatus(breakdownId)
                                result.onSuccess {
                                    breakdown = it
                                    isLoading = false
                                }.onFailure {
                                    error = it.message
                                    isLoading = false
                                }
                            }
                        }
                    )
                }
                breakdown != null -> {
                    BreakdownDetailsContent(
                        breakdown = breakdown!!,
                        onAcceptClick = { showAcceptDialog = true },
                        onRefuseClick = { showRefuseDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = RedSOS
            )
            Text("Chargement des détails...")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = RedSOS
            )
            Text(
                "Erreur",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Réessayer")
            }
        }
    }
}

@Composable
private fun BreakdownDetailsContent(
    breakdown: BreakdownResponse,
    onAcceptClick: () -> Unit,
    onRefuseClick: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Calculate distance
    val distance = breakdown.latitude?.let { lat ->
        breakdown.longitude?.let { lon ->
            // TODO: Replace with actual garage location
            calculateDistance(lat, lon, lat + 0.05, lon + 0.05)
        }
    } ?: 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Emergency Badge
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = RedSOS.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = RedSOS,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "DEMANDE URGENTE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RedSOS
                    )
                    Text(
                        "Un client a besoin d'assistance immédiate",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Breakdown Type
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    getIconForType(breakdown.type),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "Type de panne",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        breakdown.type,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Description
        if (!breakdown.description.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Description",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        breakdown.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Distance & ETA
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "%.1f km".format(distance),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Distance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(60.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${calculateETA(breakdown)} min",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Temps estimé",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Map
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Position du client",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                breakdown.latitude?.let { lat ->
                    breakdown.longitude?.let { lon ->
                        MapViewComposable(
                            latitude = lat,
                            longitude = lon,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
        }

        // Client Info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Informations client",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))

                // Phone number (if available)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("+216 XX XXX XXX") // TODO: Get from user data
                    }
                    IconButton(
                        onClick = {
                            // TODO: Get actual phone number
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:+21600000000")
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Default.Call, "Appeler")
                    }
                }

                // Coordinates
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${breakdown.latitude?.let { "%.4f".format(it) }}, ${breakdown.longitude?.let { "%.4f".format(it) }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRefuseClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = RedSOS
                )
            ) {
                Icon(Icons.Default.Cancel, null)
                Spacer(Modifier.width(8.dp))
                Text("Refuser")
            }

            Button(
                onClick = onAcceptClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.width(8.dp))
                Text("Accepter")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MapViewComposable(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                controller.setZoom(15.0)
                controller.setCenter(GeoPoint(latitude, longitude))

                // Add marker
                val marker = Marker(this).apply {
                    position = GeoPoint(latitude, longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Position du client"
                }
                overlays.add(marker)
            }
        },
        modifier = modifier
    )
}

// Helper functions
private fun getIconForType(type: String) = when (type.uppercase()) {
    "PNEU" -> Icons.Default.Circle
    "BATTERIE" -> Icons.Default.BatteryAlert
    "MOTEUR" -> Icons.Default.Build
    "CARBURANT" -> Icons.Default.LocalGasStation
    "REMORQUAGE" -> Icons.Default.DirectionsCar
    else -> Icons.Default.Warning
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

private fun calculateETA(breakdown: BreakdownResponse): Int {
    // Simple calculation: ~3 minutes per km
    val distance = breakdown.latitude?.let { lat ->
        breakdown.longitude?.let { lon ->
            calculateDistance(lat, lon, lat + 0.05, lon + 0.05)
        }
    } ?: 5.0
    return (distance * 3).toInt().coerceAtLeast(5)
}

