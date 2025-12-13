package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.viewmodel.ReservationViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import com.example.karhebti_android.data.api.ReservationResponse
import com.example.karhebti_android.ui.theme.AccentYellow
import com.example.karhebti_android.ui.theme.AlertRed
import com.example.karhebti_android.ui.theme.DeepPurple
import com.example.karhebti_android.ui.theme.InputBorder
import com.example.karhebti_android.ui.theme.InputBorderFocused
import com.example.karhebti_android.ui.theme.InputPlaceholder
import com.example.karhebti_android.ui.theme.InputText
import com.example.karhebti_android.ui.theme.LightGrey
import com.example.karhebti_android.ui.theme.LightPurple
import com.example.karhebti_android.ui.theme.MediumGrey
import com.example.karhebti_android.ui.theme.SoftWhite
import com.example.karhebti_android.ui.theme.StatusGood
import com.example.karhebti_android.ui.theme.TextPrimary
import com.example.karhebti_android.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsListScreen(
    navController: NavHostController
) {
    val context = LocalContext.current
    val viewModel: ReservationViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val reservationsState by viewModel.reservationsState.observeAsState()
    val deleteState by viewModel.deleteReservationState.observeAsState()

    var selectedFilter by remember { mutableStateOf("Tous") }
    var searchQuery by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(true) }
    val filters = listOf("Tous", "en_attente", "confirmé", "annulé")
    var showCalendarView by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.getMyReservations()
    }

    // Reload when a reservation is deleted
    LaunchedEffect(deleteState) {
        if (deleteState is Resource.Success) {
            viewModel.getMyReservations()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes réservations") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { sortAscending = !sortAscending }) {
                        Icon(
                            imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = if (sortAscending) "Tri chronologique" else "Tri anti-chronologique",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = {
                        showCalendarView = !showCalendarView
                    }) {
                        Icon(
                            imageVector = if (showCalendarView) Icons.Default.List else Icons.Default.CalendarToday,
                            contentDescription = if (showCalendarView) "Vue liste" else "Vue calendrier",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (!showCalendarView) {
                // Search and Filter Section
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Rechercher un garage...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Default.Search, "Rechercher", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Effacer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )

                // Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    when (filter) {
                                        "en_attente" -> "En attente"
                                        "confirmé" -> "Confirmé"
                                        "annulé" -> "Annulé"
                                        else -> filter
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Delete State Indicator
            when (deleteState) {
                is Resource.Loading -> {
                    LinearProgressIndicator(
                        color = DeepPurple,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is Resource.Error -> {
                    Text(
                        text = (deleteState as Resource.Error).message ?: "Erreur lors de l'annulation",
                        color = AlertRed,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                else -> {}
            }

            // Main Content
            Box(modifier = Modifier.fillMaxSize()) {
                if (showCalendarView) {
                    // Calendar View Placeholder
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Vue calendrier",
                                modifier = Modifier.size(64.dp),
                                tint = DeepPurple
                            )
                            Text(
                                "Vue calendrier",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                "Fonctionnalité à venir",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    when (val state = reservationsState) {
                        is Resource.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(color = DeepPurple)
                                    Text("Chargement des réservations...", color = TextSecondary)
                                }
                            }
                        }
                        is Resource.Success -> {
                            val allReservations = state.data ?: emptyList()
                            val filteredReservations = allReservations.filter { reservation ->
                                val garageName = reservation.getGarageName().orEmpty()
                                val garageAddress = reservation.getGarageAddress().orEmpty()

                                val matchesSearch = searchQuery.isBlank() ||
                                        garageName.contains(searchQuery, ignoreCase = true) ||
                                        garageAddress.contains(searchQuery, ignoreCase = true)
                                val matchesFilter = selectedFilter == "Tous" ||
                                        reservation.status == selectedFilter
                                matchesSearch && matchesFilter
                            }
                            val sortedReservations = if (sortAscending) {
                                filteredReservations.sortedBy { it.date }
                            } else {
                                filteredReservations.sortedByDescending { it.date }
                            }

                            if (sortedReservations.isEmpty()) {
                                EmptyReservationsState()
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(sortedReservations, key = { it.id }) { reservation ->
                                        ModernReservationCard(
                                            reservation = reservation,
                                            onCancel = {
                                                viewModel.deleteReservation(reservation.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        is Resource.Error -> {
                            ErrorReservationsState(
                                message = state.message ?: "Erreur de chargement",
                                onRetry = { viewModel.getMyReservations() }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun ModernReservationCard(
    reservation: ReservationResponse,
    onCancel: () -> Unit
) {
    val canCancel = remember(reservation.date) {
        canCancelReservation(reservation.date)
    }
    val daysRemaining = getDaysRemaining(reservation.date)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Garage name and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reservation.getGarageName() ?: "Garage non spécifié",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = reservation.getGarageAddress() ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (reservation.status) {
                        "confirmé" -> StatusGood.copy(alpha = 0.2f)
                        "annulé" -> AlertRed.copy(alpha = 0.2f)
                        else -> AccentYellow.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = reservation.status.replace("_", " ").replaceFirstChar { it.uppercase() },
                        color = when (reservation.status) {
                            "confirmé" -> StatusGood
                            "annulé" -> AlertRed
                            else -> AccentYellow
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = LightGrey)

            // Date and Time Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                        Text(
                            text = formatReservationDate(reservation.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Heure",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${reservation.heureDebut} - ${reservation.heureFin}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Services Section
            if (!reservation.services.isNullOrEmpty()) {
                Column {
                    Text(
                        text = "Services",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(reservation.services) { service ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DeepPurple.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = service,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepPurple,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Commentaires
            reservation.commentaires?.let { comment ->
                if (comment.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LightPurple.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "💬 $comment",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Days Remaining and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Days Remaining Info
                if (daysRemaining >= 0 && reservation.status == "en_attente") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            daysRemaining > 2 -> StatusGood.copy(alpha = 0.2f)
                            daysRemaining > 0 -> AccentYellow.copy(alpha = 0.2f)
                            else -> AlertRed.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = when {
                                daysRemaining > 2 -> "📅 $daysRemaining jours restants"
                                daysRemaining > 0 -> "⏰ $daysRemaining jour(s) restant(s)"
                                else -> "⚠️ Aujourd'hui"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                daysRemaining > 2 -> StatusGood
                                daysRemaining > 0 -> AccentYellow
                                else -> AlertRed
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Cancel Button for pending reservations
                if (reservation.status == "en_attente") {
                    Button(
                        onClick = onCancel,
                        enabled = canCancel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canCancel) AlertRed else MediumGrey,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Annuler")
                    }
                }
            }

            // Cancellation restriction info
            if (reservation.status == "en_attente" && !canCancel) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AlertRed.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "⚠️ Annulation impossible - Moins de 2 jours restants",
                        style = MaterialTheme.typography.labelSmall,
                        color = AlertRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyReservationsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Aucune réservation",
                modifier = Modifier.size(64.dp),
                tint = TextSecondary.copy(alpha = 0.5f)
            )
            Text(
                "Aucune réservation trouvée",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                "Vos réservations apparaîtront ici",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorReservationsState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Erreur",
                modifier = Modifier.size(64.dp),
                tint = AlertRed
            )
            Text(
                "Erreur de chargement",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Réessayer")
            }
        }
    }
}

// Helper functions
fun canCancelReservation(reservationDate: Date): Boolean {
    val now = Calendar.getInstance()
    val reservation = Calendar.getInstance().apply {
        time = reservationDate
    }

    val diff = reservation.timeInMillis - now.timeInMillis
    val daysDiff = TimeUnit.MILLISECONDS.toDays(diff)

    return daysDiff > 2
}

fun getDaysRemaining(reservationDate: Date): Long {
    val now = Calendar.getInstance()
    val reservation = Calendar.getInstance().apply {
        time = reservationDate
    }

    val diff = reservation.timeInMillis - now.timeInMillis
    return TimeUnit.MILLISECONDS.toDays(diff)
}

private fun formatReservationDate(date: Date): String {
    val formatter = SimpleDateFormat("EEE d MMM yyyy", Locale.FRENCH)
    return formatter.format(date)
}

// Extension functions for ReservationResponse with proper null safety
fun ReservationResponse.getGarageName(): String? {
    return when (val garage = this.garageId) {
        is String -> null // This would be just the ID, you might need to fetch the actual name
        is Map<*, *> -> (garage["nom"] as? String)
        else -> null
    }
}

fun ReservationResponse.getGarageAddress(): String? {
    return when (val garage = this.garageId) {
        is String -> null // This would be just the ID
        is Map<*, *> -> (garage["adresse"] as? String)
        else -> null
    }
}