package com.example.karhebti_android.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.*

/**
 * 🎉 Écran de Tracking Client
 *
 * Affiché automatiquement quand un garage accepte la demande SOS
 *
 * Features:
 * - Carte montrant les 2 positions (client + garage)
 * - Distance en temps réel
 * - ETA calculé
 * - Info garage (nom, téléphone)
 * - Bouton d'appel direct
 * - Animation de pulsation
 * - Auto-refresh toutes les 10 secondes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientTrackingScreen(
    breakdownId: String,
    clientLat: Double,
    clientLon: Double,
    garageLat: Double,
    garageLon: Double,
    garageName: String = "Garage Auto Service",
    garagePhone: String = "+216 XX XXX XXX",
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State
    var distance by remember { mutableStateOf(calculateDistance(clientLat, clientLon, garageLat, garageLon)) }
    var eta by remember { mutableStateOf((distance * 3).toInt().coerceAtLeast(5)) }
    var isGarageMoving by remember { mutableStateOf(true) }

    // Animation de pulsation pour le marqueur
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Auto-refresh toutes les 10 secondes (simulation)
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000) // 10 secondes
            // TODO: Fetch real garage position from backend
            // distance = calculateDistance(clientLat, clientLon, newGarageLat, newGarageLon)
            // eta = (distance * 3).toInt().coerceAtLeast(1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎉 Garage trouvé!") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Success Banner
            SuccessBanner(
                garageName = garageName,
                eta = eta
            )

            // Map showing both positions
            MapWithTracking(
                clientLat = clientLat,
                clientLon = clientLon,
                garageLat = garageLat,
                garageLon = garageLon,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Garage Info Card
            GarageInfoCard(
                garageName = garageName,
                garagePhone = garagePhone,
                distance = distance,
                eta = eta,
                onCallClick = {
                    // TODO: Implement call functionality
                }
            )
        }
    }
}

@Composable
private fun SuccessBanner(
    garageName: String,
    eta: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF4CAF50)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "✅ Demande acceptée!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "🗺️ Navigation démarrée",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF2E7D32)
                    )
                    Text(
                        garageName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF2E7D32)
                    )
                    Text(
                        "ETA: $eta min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MapWithTracking(
    clientLat: Double,
    clientLon: Double,
    garageLat: Double,
    garageLon: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                // Center map between both points
                val centerLat = (clientLat + garageLat) / 2
                val centerLon = (clientLon + garageLon) / 2
                controller.setZoom(13.0)
                controller.setCenter(GeoPoint(centerLat, centerLon))

                // Client marker (You)
                val clientMarker = Marker(this).apply {
                    position = GeoPoint(clientLat, clientLon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "📍 Vous"
                    snippet = "Votre position"
                }
                overlays.add(clientMarker)

                // Garage marker
                val garageMarker = Marker(this).apply {
                    position = GeoPoint(garageLat, garageLon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "🏢 Garage"
                    snippet = "En route vers vous"
                }
                overlays.add(garageMarker)

                // Line between points
                val line = Polyline().apply {
                    addPoint(GeoPoint(clientLat, clientLon))
                    addPoint(GeoPoint(garageLat, garageLon))
                    outlinePaint.color = android.graphics.Color.parseColor("#2196F3")
                    outlinePaint.strokeWidth = 8f
                }
                overlays.add(line)
            }
        },
        modifier = modifier
    )
}

@Composable
private fun GarageInfoCard(
    garageName: String,
    garagePhone: String,
    distance: Double,
    eta: Int,
    onCallClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "📍 Informations du garage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            // Garage name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F2FD))
                        .padding(8.dp),
                    tint = Color(0xFF2196F3)
                )
                Column {
                    Text(
                        garageName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "En route vers vous",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider()

            Spacer(Modifier.height(16.dp))

            // Distance and ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoItem(
                    icon = Icons.Default.Place,
                    label = "Distance",
                    value = "%.1f km".format(distance)
                )

                InfoItem(
                    icon = Icons.Default.Schedule,
                    label = "Arrivée",
                    value = "$eta min"
                )
            }

            Spacer(Modifier.height(16.dp))

            // Call button
            Button(
                onClick = onCallClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(Icons.Default.Phone, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("📞 Appeler $garagePhone")
            }
        }
    }
}

@Composable
private fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// Helper function
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

