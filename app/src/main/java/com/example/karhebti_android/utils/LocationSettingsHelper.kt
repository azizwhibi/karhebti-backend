package com.example.karhebti_android.utils

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Helper pour vérifier et gérer l'état du GPS
 */
object LocationSettingsHelper {
    
    /**
     * Vérifie si le GPS est activé sur l'appareil
     */
    fun isGPSEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    
    /**
     * Ouvre les paramètres de localisation
     */
    fun openLocationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

/**
 * Composable qui fournit un launcher pour ouvrir les paramètres de localisation
 * et vérifier si le GPS a été activé au retour
 */
@Composable
fun rememberLocationSettingsLauncher(
    onGPSEnabled: () -> Unit,
    onGPSDisabled: () -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val context = LocalContext.current
    
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Vérifier si le GPS est maintenant activé
        if (LocationSettingsHelper.isGPSEnabled(context)) {
            onGPSEnabled()
        } else {
            onGPSDisabled()
        }
    }
}
