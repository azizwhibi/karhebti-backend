 package com.example.karhebti_android.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Composant Jetpack Compose pour afficher une carte OpenStreetMap avec osmdroid
 * 
 * @param latitude Latitude du point à afficher
 * @param longitude Longitude du point à afficher
 * @param modifier Modificateur Compose
 * @param zoom Niveau de zoom initial (défaut: 15.0)
 * @param markerTitle Titre du marqueur (défaut: "Votre position")
 * @param onLocationSelected Callback appelé lorsque l'utilisateur tape sur la carte (optionnel)
 */
@Composable
fun OpenStreetMapView(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    zoom: Double = 15.0,
    markerTitle: String = "Votre position",
    onLocationSelected: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Initialiser la configuration osmdroid une seule fois
    DisposableEffect(Unit) {
        initializeOsmDroid(context)
        onDispose { }
    }
    
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                // Configuration de la carte
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                
                // Centrer la carte sur la position
                val startPoint = GeoPoint(latitude, longitude)
                controller.setZoom(zoom)
                controller.setCenter(startPoint)
                
                // Ajouter un marqueur rouge à la position
                val marker = Marker(this).apply {
                    position = startPoint
                    title = markerTitle
                    snippet = "Demande SOS"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    
                    // Utiliser l'icône par défaut d'osmdroid (marqueur rouge)
                    // Si vous voulez personnaliser, décommentez ci-dessous :
                    // icon = ContextCompat.getDrawable(ctx, R.drawable.ic_marker)
                }
                
                overlays.add(marker)
                
                // Ajouter un écouteur de clic sur la carte si onLocationSelected est fourni
                onLocationSelected?.let { callback ->
                    overlays.add(object : org.osmdroid.views.overlay.Overlay() {
                        override fun onSingleTapConfirmed(
                            e: android.view.MotionEvent,
                            mapView: MapView
                        ): Boolean {
                            val projection = mapView.projection
                            val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

                            // Mettre à jour la position du marqueur
                            // Le marqueur est toujours à l'index 0, le tap overlay est à l'index 1
                            val markerOverlay = mapView.overlays.firstOrNull { it is Marker } as? Marker
                            markerOverlay?.let {
                                it.position = geoPoint
                                it.title = markerTitle
                                mapView.invalidate()
                            }

                            // Notifier le callback avec les nouvelles coordonnées
                            callback(geoPoint.latitude, geoPoint.longitude)

                            return true
                        }
                    })
                }

                // Activer le zoom avec les boutons
                zoomController.setVisibility(
                    org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
                )
            }
        },
        update = { mapView ->
            // Mettre à jour la position si elle change
            val newPoint = GeoPoint(latitude, longitude)
            mapView.controller.setCenter(newPoint)
            
            // Mettre à jour le marqueur
            val markerOverlay = mapView.overlays.firstOrNull { it is Marker } as? Marker
            markerOverlay?.let {
                it.position = newPoint
                it.title = markerTitle
            }
            
            mapView.invalidate()
        }
    )
}

/**
 * Initialise la configuration osmdroid
 * Doit être appelé une seule fois au démarrage de l'application
 */
private fun initializeOsmDroid(context: Context) {
    // Configuration du cache et du user agent
    Configuration.getInstance().apply {
        userAgentValue = context.packageName
        
        // Définir le répertoire de cache pour les tuiles
        osmdroidBasePath = context.filesDir
        osmdroidTileCache = context.cacheDir
    }
}
