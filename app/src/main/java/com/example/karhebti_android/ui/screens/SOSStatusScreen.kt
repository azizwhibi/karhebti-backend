package com.example.karhebti_android.ui.screens

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.karhebti_android.ui.theme.RedSOS

/**
 * Écran affichant le statut d'une demande SOS
 * Affiche l'état de la demande : En attente (PENDING), Acceptée (ACCEPTED), 
 * Refusée (REFUSED), En cours (IN_PROGRESS), Terminée (COMPLETED)
 * 
 * Flux selon le flowchart :
 * 1. PENDING → En attente de réponse du garage
 * 2. ACCEPTED → Garage a accepté, bouton d'appel activé
 * 3. REFUSED → Garage a refusé
 * 4. IN_PROGRESS → Assistance en cours (appel possible)
 * 5. COMPLETED → Assistance terminée
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSStatusScreen(
    breakdownId: String?,
    type: String,
    latitude: Double,
    longitude: Double,
    status: String = "PENDING",
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Animation pour le pulse du bouton SOS
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statut SOS") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icône animée selon le statut
            when (status) {
                "PENDING" -> {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(scale)
                            .background(RedSOS.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "En attente",
                            tint = RedSOS,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
                "ACCEPTED" -> {
                    // Garage a accepté la demande - afficher une icône verte avec animation
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(scale)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Accepté",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
                "REFUSED" -> {
                    // Garage a refusé la demande
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Refusé",
                        tint = RedSOS,
                        modifier = Modifier.size(120.dp)
                    )
                }
                "IN_PROGRESS" -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(120.dp),
                        strokeWidth = 8.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                "COMPLETED" -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Terminé",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(120.dp)
                    )
                }
                else -> {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Statut inconnu",
                        tint = Color.Gray,
                        modifier = Modifier.size(120.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Titre du statut
            Text(
                text = when (status) {
                    "PENDING" -> "Demande SOS reçue"
                    "ACCEPTED" -> "SOS Accepté ✓"
                    "REFUSED" -> "SOS Refusé"
                    "IN_PROGRESS" -> "Assistance en route"
                    "COMPLETED" -> "Assistance terminée"
                    else -> "Statut inconnu"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Message de statut
            Text(
                text = when (status) {
                    "PENDING" -> "Votre demande d'assistance a été enregistrée. Un technicien sera bientôt assigné."
                    "ACCEPTED" -> "Le garage a accepté votre demande ! Vous pouvez maintenant lancer un appel vocal/vidéo."
                    "REFUSED" -> "Le garage a refusé votre demande. Veuillez réessayer ou contacter un autre garage."
                    "IN_PROGRESS" -> "Un technicien est en route vers votre position. Veuillez rester sur place."
                    "COMPLETED" -> "L'assistance a été effectuée avec succès. Merci d'avoir utilisé notre service."
                    else -> "Veuillez patienter..."
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // Carte d'informations
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ID de la demande
                    if (breakdownId != null) {
                        InfoRow(label = "ID Demande", value = "#$breakdownId")
                    }

                    // Type de panne
                    InfoRow(label = "Type de panne", value = type)

                    // Position
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                "Position GPS",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Lat: ${String.format("%.4f", latitude)}, Lon: ${String.format("%.4f", longitude)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Statut
                    InfoRow(
                        label = "Statut",
                        value = when (status) {
                            "PENDING" -> "En attente"
                            "ACCEPTED" -> "Accepté"
                            "REFUSED" -> "Refusé"
                            "IN_PROGRESS" -> "En cours"
                            "COMPLETED" -> "Terminé"
                            else -> status
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Bouton d'appel vocal/vidéo (Jitsi)
            // Activé uniquement si le garage a accepté (ACCEPTED) ou si l'assistance est en cours (IN_PROGRESS)
            if (status == "ACCEPTED" || status == "IN_PROGRESS") {
                Button(
                    onClick = {
                        // Lancer Jitsi via navigateur web (temporaire jusqu'à configuration du SDK)
                        val roomName = "sos-${breakdownId ?: "unknown"}"
                        val jitsiUrl = "https://meet.jit.si/$roomName"
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(jitsiUrl))
                        context.startActivity(intent)
                        
                        // TODO: Une fois le SDK Jitsi configuré, utiliser:
                        // val intent = Intent(context, JitsiCallActivity::class.java).apply {
                        //     putExtra("ROOM_NAME", roomName)
                        // }
                        // context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Appeler le technicien (Vocal/Vidéo)")
                }
                
                Spacer(Modifier.height(12.dp))
            }
            
            // Bouton "Contacter" désactivé si en attente
            if (status == "PENDING") {
                OutlinedButton(
                    onClick = { /* Disabled */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = false
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("En attente de réponse du garage...")
                }
                
                Spacer(Modifier.height(12.dp))
            }
            
            // Message si refusé
            if (status == "REFUSED") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = RedSOS.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = RedSOS
                        )
                        Text(
                            "Le garage n'est pas disponible pour cette demande. Veuillez créer une nouvelle demande SOS.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retour à l'accueil")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
