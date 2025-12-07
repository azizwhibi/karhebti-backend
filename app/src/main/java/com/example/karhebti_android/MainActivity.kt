package com.example.karhebti_android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.karhebti_android.data.notifications.FCMTokenService
import com.example.karhebti_android.navigation.NavGraph
import com.example.karhebti_android.ui.theme.KarhebtiandroidTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "✅ Permission de notification accordée")
        } else {
            Log.w("MainActivity", "❌ Permission de notification refusée")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate started")

        // Demander la permission de notification pour Android 13+
        askNotificationPermission()

        try {
            // Initialize RetrofitClient with application context
            com.example.karhebti_android.data.api.RetrofitClient.initialize(this.applicationContext)

            // Initialiser FCM et envoyer le token au backend
            initializeFCM()

            enableEdgeToEdge()
            setContent {
                KarhebtiandroidTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()

                        // Handle notification intent navigation
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            handleNotificationIntent(navController)
                        }

                        NavGraph(navController = navController)
                    }
                }
            }
            Log.d("MainActivity", "setContent completed successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun askNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Initialiser Firebase Cloud Messaging
     */
    private fun initializeFCM() {
        try {
            val fcmTokenService = FCMTokenService(this)

            // Enregistrer le token device au backend
            fcmTokenService.registerDeviceToken()

            // S'abonner aux topics
            fcmTokenService.subscribeToTopics()

            Log.d("MainActivity", "✅ FCM initialisé avec succès")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Erreur lors de l'initialisation FCM: ${e.message}", e)
        }
    }

    /**
     * Handle notification intent and navigate to appropriate screen
     */
    private fun handleNotificationIntent(navController: androidx.navigation.NavHostController) {
        try {
            val fromNotification = intent.getBooleanExtra("from_notification", false)

            if (fromNotification) {
                val notificationType = intent.getStringExtra("notification_type")

                Log.d("MainActivity", "📱 Opened from notification: type=$notificationType")

                when (notificationType) {
                    "BREAKDOWN_REQUEST" -> {
                        // Garage owner received SOS notification
                        val breakdownId = intent.getStringExtra("breakdownId")

                        if (!breakdownId.isNullOrBlank()) {
                            Log.d("MainActivity", "🚨 Navigating to breakdown details: $breakdownId")

                            // Navigate to garage breakdown details screen
                            navController.navigate(
                                com.example.karhebti_android.navigation.Screen.GarageBreakdownDetails.createRoute(breakdownId)
                            )
                        } else {
                            Log.w("MainActivity", "⚠️ No breakdownId in notification intent")
                        }
                    }
                    else -> {
                        Log.d("MainActivity", "ℹ️ Unknown notification type: $notificationType")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error handling notification intent: ${e.message}", e)
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Handle notification when app is already running
        Log.d("MainActivity", "onNewIntent called")
    }
}
