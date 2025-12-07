package com.example.karhebti_android.ui.screens

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.karhebti_android.data.BreakdownResponse
import com.example.karhebti_android.ui.theme.RedSOS
import kotlinx.coroutines.delay

/**
 * Écran d'attente après envoi d'une demande SOS
 * Affiche :
 * - Animation de chargement
 * - Statut en temps réel : PENDING → ACCEPTED/REFUSED
 * - Quand ACCEPTED → Navigation automatique vers l'écran de trajet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSWaitingScreen(
    breakdownId: String,
    onGarageAccepted: (BreakdownResponse) -> Unit,
    onGarageRefused: () -> Unit,
    onBackClick: () -> Unit,
    onGetBreakdownStatus: suspend (String) -> Result<BreakdownResponse>,
    modifier: Modifier = Modifier
) {
    var breakdownStatus by remember { mutableStateOf<BreakdownResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Animation du pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Polling du statut toutes les 3 secondes
    LaunchedEffect(breakdownId) {
        android.util.Log.d("SOSWaiting", "🚀 Starting polling for breakdown: $breakdownId")

        while (true) {
            try {
                android.util.Log.d("SOSWaiting", "🔄 Polling status... (interval: 3s)")
                val result = onGetBreakdownStatus(breakdownId)

                result.onSuccess { breakdown ->
                    android.util.Log.d("SOSWaiting", "✅ Got status: ${breakdown.status}")
                    breakdownStatus = breakdown
                    isLoading = false

                    // Vérifier le statut
                    when (breakdown.status.uppercase()) {
                        "ACCEPTED" -> {
                            // Garage a accepté → Naviguer vers trajet
                            android.util.Log.d("SOSWaiting", "🎉 Status ACCEPTED detected! Navigating to ClientTracking...")
                            delay(1000) // Petite pause pour montrer l'info
                            onGarageAccepted(breakdown)
                            return@LaunchedEffect
                        }
                        "REFUSED" -> {
                            android.util.Log.d("SOSWaiting", "❌ Status REFUSED detected")
                            delay(2000)
                            onGarageRefused()
                            return@LaunchedEffect
                        }
                        "CANCELLED" -> {
                            android.util.Log.d("SOSWaiting", "🚫 Status CANCELLED detected")
                            onGarageRefused()
                            return@LaunchedEffect
                        }
                    }
                }
                result.onFailure {
                    android.util.Log.e("SOSWaiting", "❌ Error fetching status: ${it.message}")
                    errorMessage = it.message
                }
            } catch (e: Exception) {
                android.util.Log.e("SOSWaiting", "💥 Exception during polling: ${e.message}", e)
                errorMessage = e.message
            }

            delay(3000) // Vérifier toutes les 3 secondes (réduit de 5s à 3s)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("En attente de confirmation") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RedSOS,
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icône SOS animée
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(RedSOS.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = RedSOS
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Titre
            Text(
                text = "Demande SOS envoyée !",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Statut actuel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = RedSOS
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Connexion au garage...",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }

                        breakdownStatus != null -> {
                            val status = breakdownStatus!!.status.uppercase()

                            // Icône selon le statut
                            when (status) {
                                "PENDING", "OPEN" -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        color = RedSOS
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "⏳ En attente de réponse",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Le garage examine votre demande...\nVous serez notifié dès qu'il répond.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                "ACCEPTED" -> {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color(0xFF4CAF50)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "✅ Demande acceptée !",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Le dépanneur arrive !\nRedirection vers le suivi du trajet...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                "REFUSED" -> {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color(0xFFF44336)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "❌ Demande refusée",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = Color(0xFFF44336)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Le garage ne peut pas répondre pour le moment.\nEssayez un autre garage.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        errorMessage != null -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Erreur de connexion",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "Erreur inconnue",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Informations supplémentaires
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ID de la demande : ${breakdownId.take(8)}...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (breakdownStatus != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Position : ${breakdownStatus!!.latitude?.let { "%.4f".format(it) }}, ${breakdownStatus!!.longitude?.let { "%.4f".format(it) }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bouton annuler
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = RedSOS
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Annuler et retourner")
            }
        }
    }
}

