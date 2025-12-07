package com.example.karhebti_android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
 * 🚗 Écran de Navigation pour Garage Owner
 *
 * S'affiche automatiquement après avoir accepté une demande SOS
 *
 * Features:
 * - Carte montrant la position du client
 * - Distance et ETA
 * - Bouton "Démarrer la navigation" (OSM/Google Maps)
 * - Info client (téléphone)
 * - Countdown timer
 * - Bouton "Marquer comme arrivé"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageNavigationScreen(
    breakdownId: String,
    clientLat: Double,
    clientLon: Double,
    clientName: String = "Client",
    clientPhone: String = "+216 XX XXX XXX",
    breakdownType: String = "PNEU",
    garageLat: Double = 36.8165, // Position actuelle du garage (TODO: GPS)
    garageLon: Double = 10.1915,
    onBackClick: () -> Unit = {},
    onArrived: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State
    var distance by remember { mutableStateOf(calculateDistance(garageLat, garageLon, clientLat, clientLon)) }
    var eta by remember { mutableStateOf((distance * 3).toInt().coerceAtLeast(5)) }
    var isNavigating by remember { mutableStateOf(false) }

    // Auto-refresh toutes les 30 secondes
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000) // 30 secondes
            // TODO: Fetch current garage position from GPS
            // distance = calculateDistance(newGarageLat, newGarageLon, clientLat, clientLon)
            // eta = (distance * 3).toInt().coerceAtLeast(1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚗 Navigation vers client") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
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
            NavigationBanner(
                breakdownType = breakdownType,
                distance = distance,
                eta = eta
            )

            // Map showing route
            MapWithRoute(
                garageLat = garageLat,
                garageLon = garageLon,
                clientLat = clientLat,
                clientLon = clientLon,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Client Info Card
            ClientInfoCard(
                clientName = clientName,
                clientPhone = clientPhone,
                distance = distance,
                eta = eta,
                onCallClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clientPhone"))
                    context.startActivity(intent)
                },
                onNavigateClick = {
                    // Open navigation app
                    openNavigationApp(context, clientLat, clientLon)
                    isNavigating = true
                },
                onArrivedClick = onArrived
            )
        }
    }
}

@Composable
private fun NavigationBanner(
    breakdownType: String,
    distance: Double,
    eta: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Navigation,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF2196F3)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "✅ Demande acceptée!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
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
                        Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF1976D2)
                    )
                    Text(
                        breakdownType,
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
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF1976D2)
                    )
                    Text(
                        "%.1f km".format(distance),
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
                        tint = Color(0xFF1976D2)
                    )
                    Text(
                        "$eta min",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MapWithRoute(
    garageLat: Double,
    garageLon: Double,
    clientLat: Double,
    clientLon: Double,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                // Center map between both points
                val centerLat = (garageLat + clientLat) / 2
                val centerLon = (garageLon + clientLon) / 2
                controller.setZoom(13.0)
                controller.setCenter(GeoPoint(centerLat, centerLon))

                // Garage marker (You - current position)
                val garageMarker = Marker(this).apply {
                    position = GeoPoint(garageLat, garageLon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "🚗 Vous"
                    snippet = "Votre position actuelle"
                }
                overlays.add(garageMarker)

                // Client marker (destination)
                val clientMarker = Marker(this).apply {
                    position = GeoPoint(clientLat, clientLon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "📍 Client"
                    snippet = "Destination"
                }
                overlays.add(clientMarker)

                // Route line
                val line = Polyline().apply {
                    addPoint(GeoPoint(garageLat, garageLon))
                    addPoint(GeoPoint(clientLat, clientLon))
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
private fun ClientInfoCard(
    clientName: String,
    clientPhone: String,
    distance: Double,
    eta: Int,
    onCallClick: () -> Unit,
    onNavigateClick: () -> Unit,
    onArrivedClick: () -> Unit
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
                "📍 Direction: Client",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            // Client info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Person,
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
                        clientName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        clientPhone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    label = "Temps",
                    value = "$eta min"
                )
            }

            Spacer(Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call button
                OutlinedButton(
                    onClick = onCallClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Appeler")
                }

                // Navigate button
                Button(
                    onClick = onNavigateClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Naviguer")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Arrived button
            Button(
                onClick = onArrivedClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("✅ Marquer comme arrivé")
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

// Open navigation app (OSM/Google Maps)
private fun openNavigationApp(context: android.content.Context, lat: Double, lon: Double) {
    try {
        // Try Google Maps first
        val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lon")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        // Fallback to generic geo intent
        val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Client)")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
        context.startActivity(mapIntent)
    }
}

