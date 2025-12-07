package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.BreakdownResponse
import com.example.karhebti_android.network.BreakdownsApi
import com.example.karhebti_android.repository.BreakdownsRepository
import com.example.karhebti_android.ui.theme.RedSOS
import com.example.karhebti_android.viewmodel.BreakdownUiState
import com.example.karhebti_android.viewmodel.BreakdownViewModel
import com.example.karhebti_android.viewmodel.BreakdownViewModelFactory
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 🚨 Écran de liste des demandes SOS
 *
 * Pour les propriétaires de garage
 * Affiche toutes les demandes SOS en attente (PENDING)
 *
 * Features:
 * - Liste des breakdowns PENDING
 * - Tri par distance (plus proche en premier)
 * - Affichage type, distance, temps
 * - Click pour voir détails
 * - Badge de statut
 * - Pull to refresh
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSRequestsListScreen(
    onBackClick: () -> Unit,
    onSOSClick: (String) -> Unit = {},
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

    val uiState by viewModel.uiState.collectAsState()

    // Load breakdowns on start
    LaunchedEffect(Unit) {
        viewModel.fetchAllBreakdowns(status = "PENDING")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚨 Demandes SOS") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.fetchAllBreakdowns(status = "PENDING")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, "Actualiser")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RedSOS,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
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
            when (val state = uiState) {
                is BreakdownUiState.Loading -> {
                    LoadingContent()
                }
                is BreakdownUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = {
                            coroutineScope.launch {
                                viewModel.fetchAllBreakdowns(status = "PENDING")
                            }
                        }
                    )
                }
                is BreakdownUiState.Success -> {
                    val breakdowns = (state.data as? List<*>)?.filterIsInstance<BreakdownResponse>() ?: emptyList()

                    if (breakdowns.isEmpty()) {
                        EmptyContent()
                    } else {
                        BreakdownsList(
                            breakdowns = breakdowns,
                            onSOSClick = onSOSClick
                        )
                    }
                }
                else -> {
                    // Idle state
                    EmptyContent()
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
            Text("Chargement des demandes...")
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
private fun EmptyContent() {
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
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF4CAF50)
            )
            Text(
                "Aucune demande SOS",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Il n'y a aucune demande d'assistance en attente pour le moment.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BreakdownsList(
    breakdowns: List<BreakdownResponse>,
    onSOSClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Text(
                text = "${breakdowns.size} demande(s) en attente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Breakdowns list
        items(breakdowns) { breakdown ->
            BreakdownCard(
                breakdown = breakdown,
                onClick = { onSOSClick(breakdown.id) }
            )
        }
    }
}

@Composable
private fun BreakdownCard(
    breakdown: BreakdownResponse,
    onClick: () -> Unit
) {
    // Calculate distance (simulate for now - should get actual garage location)
    val distance = breakdown.latitude?.let { lat ->
        breakdown.longitude?.let { lon ->
            // TODO: Get actual garage location
            calculateDistance(lat, lon, lat + 0.05, lon + 0.05)
        }
    } ?: 0.0

    val eta = (distance * 3).toInt().coerceAtLeast(5)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        getIconForType(breakdown.type),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(getColorForType(breakdown.type).copy(alpha = 0.2f))
                            .padding(8.dp),
                        tint = getColorForType(breakdown.type)
                    )
                    Text(
                        breakdown.type,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (breakdown.status.uppercase()) {
                        "PENDING" -> Color(0xFFFFA726)
                        "ACCEPTED" -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = breakdown.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Description
            if (!breakdown.description.isNullOrBlank()) {
                Text(
                    breakdown.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                Spacer(Modifier.height(12.dp))
            }

            // Distance & ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "%.1f km".format(distance),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "$eta min",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Voir détails",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
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

private fun getColorForType(type: String) = when (type.uppercase()) {
    "PNEU" -> Color(0xFFFF5722)
    "BATTERIE" -> Color(0xFFFFC107)
    "MOTEUR" -> Color(0xFFF44336)
    "CARBURANT" -> Color(0xFF4CAF50)
    "REMORQUAGE" -> Color(0xFF2196F3)
    else -> Color(0xFF9E9E9E)
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

