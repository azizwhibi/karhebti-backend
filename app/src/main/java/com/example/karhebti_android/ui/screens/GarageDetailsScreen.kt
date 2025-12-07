package com.example.karhebti_android.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.karhebti_android.data.api.ServiceResponse
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.navigation.Screen
import com.example.karhebti_android.ui.theme.*
import com.example.karhebti_android.viewmodel.GarageViewModel
import com.example.karhebti_android.viewmodel.ServiceViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory

import androidx.compose.material.icons.filled.Navigation
import com.example.karhebti_android.ui.screens.GarageMapDialog
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageDetailsScreen(
    garageId: String,
    onBackClick: () -> Unit,
    userRole: String,
    navController: NavHostController
) {
    var showMapDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val garageViewModel: GarageViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val serviceViewModel: ServiceViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    val garagesState by garageViewModel.garagesState.observeAsState()
    val servicesState by serviceViewModel.servicesState.observeAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(garageId) {
        garageViewModel.getGarages()
        serviceViewModel.getServicesByGarage(garageId)
    }

    val garage = garagesState?.data?.find { it.id == garageId }

    val servicesAll = servicesState?.data ?: emptyList()
    val services = servicesAll.filter { service ->
        when (val g = service.garage) {
            is String -> g == garageId
            is Map<*, *> -> (g["_id"] as? String) == garageId
            is com.example.karhebti_android.data.api.GarageResponse -> g.id == garageId
            else -> false
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = garage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Text(
                                garage?.nom ?: "",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = AccentYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "%.1f".format(garage?.noteUtilisateur ?: 0.0),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    " • ${services.size} service(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    if (userRole == "propGarage" && garage != null) {
                        Row(modifier = Modifier.padding(end = 4.dp)) {
                            IconButton(
                                onClick = {
                                    navController.navigate(Screen.EditGarage.createRoute(garageId))
                                }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Modifier",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            IconButton(
                                onClick = { showDeleteDialog = true }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = AlertRed.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = garage != null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                if (userRole == "propGarage") {
                    FloatingActionButton(
                        onClick = { navController.navigate(Screen.AddService.createRoute(garageId)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            "Ajouter Service",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(Screen.Reservation.createRoute(garageId))
                        },
                        containerColor = AccentGreen,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            "Réserver",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(paddingValues)
        ) {
            if (garage == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Text(
                            "Chargement...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Hero Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box {
                            // Gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                            )
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth()
                            ) {
                                // Title and Rating
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            garage.nom,
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.ExtraBold,
                                            lineHeight = 36.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Surface(
                                            color = AccentYellow.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(16.dp),
                                            shadowElevation = 4.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    null,
                                                    tint = AccentYellow,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "%.1f".format(garage.noteUtilisateur),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    " / 5",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (userRole == "propGarage") {
                                        Surface(
                                            color = SoftBlue.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(16.dp),
                                            onClick = {
                                                navController.navigate(Screen.GarageReservationsList.createRoute(garageId))
                                            },
                                            shadowElevation = 4.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.CalendarToday,
                                                    null,
                                                    tint = SoftBlue,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "Réservations",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = SoftBlue,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Info Cards
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ModernInfoRow(
                                        icon = Icons.Default.LocationOn,
                                        iconColor = AccentGreen,
                                        label = "Adresse",
                                        value = garage.adresse
                                    )

                                    ModernInfoRow(
                                        icon = Icons.Default.Phone,
                                        iconColor = DeepPurple,
                                        label = "Téléphone",
                                        value = garage.telephone
                                    )

                                    if (garage.heureOuverture?.isNotBlank() == true &&
                                        garage.heureFermeture?.isNotBlank() == true) {
                                        ModernInfoRow(
                                            icon = Icons.Default.Schedule,
                                            iconColor = SoftBlue,
                                            label = "Horaires",
                                            value = "${garage.heureOuverture} - ${garage.heureFermeture}"
                                        )
                                    }
                                    // ✅ NOUVEAU: Afficher le nombre de créneaux
                                    if (garage.numberOfBays != null && garage.numberOfBays!! > 0) {
                                        ModernInfoRow(
                                            icon = Icons.Default.Garage,
                                            iconColor = AccentGreen,
                                            label = "Créneaux de réparation",
                                            value = "${garage.numberOfBays} emplacement${if (garage.numberOfBays!! > 1) "s" else ""}"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Services Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Build,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .padding(10.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Services proposés",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${services.size}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            when (servicesState) {
                                is Resource.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp),
                                            strokeWidth = 4.dp
                                        )
                                    }
                                }
                                is Resource.Success -> {
                                    if (services.isEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Build,
                                                contentDescription = null,
                                                modifier = Modifier.size(56.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                "Aucun service disponible",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    } else {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(services) { service ->
                                                UltraModernServiceChip(service = service)
                                            }
                                        }
                                    }
                                }
                                is Resource.Error -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(AlertRed.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.Error,
                                                contentDescription = null,
                                                tint = AlertRed,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Erreur de chargement",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = AlertRed,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                    // ✅ NOUVEAU: Repair Bays Info Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = AccentGreen.copy(alpha = 0.15f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Garage,
                                            contentDescription = null,
                                            tint = AccentGreen,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .padding(10.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Créneaux de réparation",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = AccentGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = AccentGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${garage.numberOfBays ?: 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AccentGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = AccentGreen.copy(alpha = 0.08f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Nombre d'emplacements simultanés",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Ce garage dispose de ${garage.numberOfBays ?: 1} créneau${if ((garage.numberOfBays ?: 1) > 1) "x" else ""} de réparation pour servir plusieurs clients simultanément.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }


                    // Quick Actions Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                "Actions rapides",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ModernQuickActionButton(
                                    icon = Icons.Default.Phone,
                                    label = "Appeler",
                                    color = AccentGreen,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:${garage.telephone}")
                                        }
                                        context.startActivity(intent)
                                    }
                                )

                                ModernQuickActionButton(
                                    icon = Icons.Default.LocationOn,
                                    label = "Itinéraire",
                                    color = SoftBlue,
                                    modifier = Modifier.weight(1f),
                                    onClick = { showMapDialog = true }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (userRole != "propGarage") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = {
                                            navController.navigate(Screen.Reservation.createRoute(garageId))
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(58.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(16.dp),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "Réserver ce garage",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            navController.navigate(Screen.ReservationsList.route)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(58.dp),
                                        border = BorderStroke(2.dp, SoftBlue),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = SoftBlue
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ListAlt,
                                            null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            "Voir mes réservations",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        navController.navigate(Screen.GarageReservationsList.createRoute(garageId))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(58.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftBlue),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ListAlt,
                                        null,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Voir les réservations",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = AlertRed.copy(alpha = 0.15f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = AlertRed,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(16.dp)
                        )
                    }
                },
                title = {
                    Text(
                        "Supprimer le garage ?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Cette action est irréversible. Tous les services et réservations associés seront également supprimés.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            garageViewModel.deleteGarage(garageId)
                            navController.popBackStack()
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Supprimer", fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                shape = RoundedCornerShape(28.dp)
            )
        }
        // Map Dialog - Show garage location
        if (showMapDialog) {
            GarageMapDialog(
                context = context,
                garageName = garage?.nom ?: "",
                garageAddress = garage?.adresse ?: "",
                latitude = garage?.latitude,
                longitude = garage?.longitude,
                onDismiss = { showMapDialog = false }
            )
        }

    }
}

@Composable
fun ModernInfoRow(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = iconColor.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier
                        .size(40.dp)
                        .padding(10.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun UltraModernServiceChip(service: ServiceResponse) {
    Card(
        modifier = Modifier.width(210.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(44.dp)
                            .padding(12.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = service.type.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Price
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AccentGreen.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Prix moyen",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${service.coutMoyen} DH",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Duration
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AccentYellow.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = AccentYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Durée estimée",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${service.dureeEstimee} min",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentYellow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status
            Surface(
                color = StatusGood.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StatusGood)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Disponible",
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusGood,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ModernQuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
