package com.example.karhebti_android.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.karhebti_android.ui.theme.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/****
 * Dialog to display garage location on an OSM map
 * Allows users to view the exact location and open it in external map apps
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageMapDialog(
    context: Context,
    garageName: String,
    garageAddress: String,
    latitude: Double?,  // Change to nullable
    longitude: Double?, // Change to nullable
    onDismiss: () -> Unit
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = DeepPurple,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                "Itinéraire vers $garageName",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Display address
                Text(
                    garageAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(Modifier.height(8.dp))

                // Display coordinates if available
                if (latitude != null && longitude != null) {
                    Text(
                        "Coordonnées: ${String.format("%.5f", latitude)}, ${String.format("%.5f", longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    Spacer(Modifier.height(16.dp))

                    // OSM Map - Only show if coordinates are available
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                Configuration.getInstance().userAgentValue = ctx.packageName

                                MapView(ctx).apply {
                                    setTileSource(TileSourceFactory.MAPNIK)
                                    setMultiTouchControls(true)
                                    controller.setZoom(16.0)
                                    controller.setCenter(GeoPoint(latitude, longitude))

                                    // Add garage marker
                                    val marker = Marker(this).apply {
                                        position = GeoPoint(latitude, longitude)
                                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        title = garageName
                                        snippet = garageAddress
                                    }
                                    overlays.add(marker)

                                    mapView = this
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )
                    }
                } else {
                    // Show message when no coordinates available
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = LightGrey.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    "Coordonnées non disponibles",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    "Ouverture avec l'adresse uniquement",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (latitude != null && longitude != null) {
                        // Open with exact coordinates
                        val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($garageName)")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            // Fallback without package restriction
                            val fallbackIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            context.startActivity(fallbackIntent)
                        }
                    } else {
                        // Fallback to address search
                        val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(garageAddress)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            // Browser fallback
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(garageAddress)}")
                            )
                            context.startActivity(browserIntent)
                        }
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Directions,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Ouvrir Maps", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Annuler", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(28.dp)
    )

    // Clean up map view when dialog is dismissed
    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDetach()
        }
    }
}