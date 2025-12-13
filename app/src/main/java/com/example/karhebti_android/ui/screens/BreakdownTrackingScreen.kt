package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.karhebti_android.ui.theme.RedSOS
import com.example.karhebti_android.ui.theme.GreenResolved
import com.example.karhebti_android.ui.theme.OrangeInProgress
import com.example.karhebti_android.ui.theme.BlueInfo
import com.example.karhebti_android.data.BreakdownResponse
import com.example.karhebti_android.data.AgentResponse

@Composable
fun BreakdownTrackingScreen(
    breakdown: BreakdownResponse,
    agent: AgentResponse?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Carte placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFFE0E0E0), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Carte (à intégrer)", color = Color.Gray)
        }
        Spacer(Modifier.height(16.dp))
        // Infos agent
        if (agent != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).background(BlueInfo, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(agent.name, fontWeight = FontWeight.Bold)
                    Text(agent.phone, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(16.dp))
        // Statut de la panne
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(type = breakdown.type, status = breakdown.status)
            Spacer(Modifier.width(8.dp))
            Text(breakdown.status, color = statusColor(breakdown.status), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        // Timeline simple
        Timeline(status = breakdown.status)
    }
}

@Composable
fun Timeline(status: String) {
    val steps = listOf("OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        steps.forEach {
            val reached = steps.indexOf(it) <= steps.indexOf(status)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(if (reached) statusColor(it) else Color.LightGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text((steps.indexOf(it) + 1).toString(), color = Color.White)
            }
        }
    }
}

@Composable
fun StatusBadge(type: String, status: String) {
    val color = statusColor(status)
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(color, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(type.take(1), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

fun statusColor(status: String): Color = when (status) {
    "OPEN" -> RedSOS
    "ASSIGNED" -> BlueInfo
    "IN_PROGRESS" -> OrangeInProgress
    "RESOLVED" -> GreenResolved
    else -> Color.Gray
}
