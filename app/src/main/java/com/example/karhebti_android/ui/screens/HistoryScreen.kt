package com.example.karhebti_android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Simple UI list for SOS history. This file intentionally keeps a small internal
 * data class `BreakdownItemUi` so it can be dropped into the project without
 * adding new model files. In the real app bind this composable to your ViewModel
 * state (list of breakdowns) instead.
 */

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    // list of past SOS entries to render
    items: List<BreakdownItemUi> = sampleItems()
) {
    val ctx = LocalContext.current

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Historique des SOS",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucune demande SOS pour le moment")
            }
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.type, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Statut: ${item.status}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = item.date, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Dialer button (no CALL_PHONE permission required)
                            OutlinedButton(onClick = {
                                val phone = item.agentPhone
                                if (phone.isNullOrBlank()) {
                                    // Ideally show snackbar — use Toast for simplicity
                                    android.widget.Toast.makeText(ctx, "Numéro d'agent indisponible", android.widget.Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                val phoneUri = Uri.parse("tel:$phone")
                                val intent = Intent(Intent.ACTION_DIAL, phoneUri)
                                ctx.startActivity(intent)
                            }, modifier = Modifier.weight(1f)) {
                                Text("Appeler")
                            }

                            // Video/Voice call via Jitsi
                            Button(onClick = {
                                // Only start activity if room provided
                                val room = item.roomName
                                if (room.isNullOrBlank()) {
                                    android.widget.Toast.makeText(ctx, "Room indisponible pour cet appel", android.widget.Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                // Use explicit class name to avoid compile-time dependency issues
                                val intent = Intent().apply {
                                    setClassName(ctx.packageName, "com.example.karhebti_android.jitsi.JitsiCallActivity")
                                    putExtra("ROOM", room)
                                }
                                // Safe start: check that intent resolves
                                try {
                                    ctx.startActivity(intent)
                                } catch (t: Throwable) {
                                    android.widget.Toast.makeText(ctx, "Impossible de lancer l'appel: ${t.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }, modifier = Modifier.weight(1f)) {
                                Text("Rejoindre l'appel")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Small UI model used only by this screen to avoid creating new top-level models.
 */
data class BreakdownItemUi(
    val id: String,
    val type: String,
    val status: String,
    val date: String,
    val agentName: String? = null,
    val agentPhone: String? = null,
    val roomName: String? = null // Jitsi room identifier
)

private fun sampleItems(): List<BreakdownItemUi> = listOf(
    BreakdownItemUi(
        id = "1",
        type = "PNEU",
        status = "RESOLVED",
        date = "2025-11-20",
        agentName = "Garage Express",
        agentPhone = "+21671123456",
        roomName = "sos-room-1"
    ),
    BreakdownItemUi(
        id = "2",
        type = "BATTERIE",
        status = "OPEN",
        date = "2025-11-27",
        agentName = "AutoService",
        agentPhone = "+21671999999",
        roomName = ""
    )
)
