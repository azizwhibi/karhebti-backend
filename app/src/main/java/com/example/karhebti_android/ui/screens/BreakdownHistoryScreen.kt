package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Simple history item used by the UI. Project already has a Breakdown model —
 * this lightweight class keeps the screen standalone and easy to wire.
 */
data class HistoryItem(
    val id: String,
    val type: String,
    val status: String,
    val date: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * BreakdownHistoryScreen - écran Compose listant les pannes récentes.
 * - Affiche une liste d'items simples (type, statut, date)
 * - Bouton "Appeler" sur chaque ligne — déclenche onCall(roomId)
 * - Bouton retour en haut qui appelle onBackClick
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun BreakdownHistoryScreen(
    modifier: Modifier = Modifier,
    items: List<HistoryItem> = emptyList(),  // Default empty list for now
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    onCall: (roomId: String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Top bar with a back button and title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Historique des pannes",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (items.isEmpty()) {
            // Empty state
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Aucune panne trouvée", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRefresh) { Text("Rafraîchir") }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // weight() modifier comes from androidx.compose.foundation.layout
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.type.uppercase(), style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Statut: ${item.status}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Date: ${item.date}", style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.padding(6.dp))

                            Button(
                                onClick = {
                                    // Construire room id (exemple : "sos_<id>") ou envoyer l'id brut au backend
                                    val roomId = "sos_${item.id}"
                                    onCall(roomId)
                                }
                            ) {
                                Icon(imageVector = Icons.Filled.Call, contentDescription = "Appeler")
                                Spacer(modifier = Modifier.padding(6.dp))
                                Text(text = "Appeler")
                            }
                        }
                    }
                }
            }
        }
    }
}
