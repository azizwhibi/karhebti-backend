package com.example.karhebti_android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.karhebti_android.data.BreakdownResponse
import com.example.karhebti_android.ui.theme.RedSOS
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * Écran de suivi en temps réel du dépanneur
 * Affiche :
 * - Position du client (vous) en rouge 🔴
 * - Position du dépanneur en vert 🟢 (simulé pour l'instant)
 * - Trajet entre les deux positions
 * - Temps estimé d'arrivée
 * - Bouton d'appel direct
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageTrackingScreen(
    breakdown: BreakdownResponse,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Position du client (de la panne)
    val clientLocation = remember {
        GeoPoint(breakdown.latitude ?: 36.8065, breakdown.longitude ?: 10.1815)
    }

    // Position du dépanneur (simulée - avance progressivement vers le client)
    var garageLocation by remember {
        mutableStateOf(
            GeoPoint(
                (breakdown.latitude ?: 36.8065) - 0.05, // 5km au sud
                (breakdown.longitude ?: 10.1815) - 0.05
            )
        )
    }

    var estimatedTime by remember { mutableStateOf(15) } // minutes
    var distanceKm by remember { mutableStateOf(5.2) }

    // Simulation du mouvement du dépanneur (toutes les 3 secondes)
    LaunchedEffect(Unit) {
        while (estimatedTime > 0) {
            delay(3000)

            // Rapprocher le dépanneur du client
            garageLocation = GeoPoint(
                garageLocation.latitude + (clientLocation.latitude - garageLocation.latitude) * 0.05,
                garageLocation.longitude + (clientLocation.longitude - garageLocation.longitude) * 0.05
            )

            // Mettre à jour la distance et le temps
            distanceKm = (distanceKm * 0.95).coerceAtLeast(0.1)
            estimatedTime = (distanceKm * 3).toInt().coerceAtLeast(1) // ~3 min par km

            // Mettre à jour la carte
            mapView?.let { map ->
                map.overlays.clear()

                // Marqueur client (rouge)
                val clientMarker = Marker(map).apply {
                    position = clientLocation
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Vous êtes ici"
                    snippet = "Position de votre panne"
                }

                // Marqueur dépanneur (vert)
                val garageMarker = Marker(map).apply {
                    position = garageLocation
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Dépanneur"
                    snippet = "Arrive dans $estimatedTime min"
                }

                // Ligne de trajet
                val line = Polyline().apply {
                    addPoint(garageLocation)
                    addPoint(clientLocation)
                    outlinePaint.color = android.graphics.Color.BLUE
                    outlinePaint.strokeWidth = 5f
                }

                map.overlays.add(line)
                map.overlays.add(clientMarker)
                map.overlays.add(garageMarker)
                map.invalidate()
            }
        }
    }

    // Animation du pulse pour le statut
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dépanneur en route") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Carte OpenStreetMap
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().userAgentValue = ctx.packageName
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(13.0)
                        controller.setCenter(clientLocation)

                        // Marqueur client (rouge)
                        val clientMarker = Marker(this).apply {
                            position = clientLocation
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Vous êtes ici"
                            snippet = "Position de votre panne"
                        }

                        // Marqueur dépanneur (vert)
                        val garageMarker = Marker(this).apply {
                            position = garageLocation
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "Dépanneur"
                            snippet = "Arrive dans $estimatedTime min"
                        }

                        // Ligne de trajet
                        val line = Polyline().apply {
                            addPoint(garageLocation)
                            addPoint(clientLocation)
                            outlinePaint.color = android.graphics.Color.BLUE
                            outlinePaint.strokeWidth = 5f
                        }

                        overlays.add(line)
                        overlays.add(clientMarker)
                        overlays.add(garageMarker)

                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay avec les informations
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                // Carte d'information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Statut principal
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(scale)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "En route",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                                Text(
                                    text = "Le dépanneur arrive",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Divider()

                        Spacer(modifier = Modifier.height(16.dp))

                        // Temps estimé
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "$estimatedTime min",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Arrivée estimée",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "%.1f km".format(distanceKm),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Distance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Barre de progression
                        LinearProgressIndicator(
                            progress = 1f - (distanceKm / 5.2f).toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF4CAF50),
                            trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Boutons d'action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Bouton appeler
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:+21612345678") // Numéro du garage
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Appeler")
                            }

                            // Bouton message
                            OutlinedButton(
                                onClick = {
                                    // TODO: Ouvrir chat avec le garage
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Message")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Carte d'information supplémentaire
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Type de panne : ${breakdown.type}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDetach()
        }
    }
}

