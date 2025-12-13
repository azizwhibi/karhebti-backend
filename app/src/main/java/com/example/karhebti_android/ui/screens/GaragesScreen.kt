package com.example.karhebti_android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.karhebti_android.data.api.GarageResponse
import com.example.karhebti_android.data.api.ServiceResponse
import com.example.karhebti_android.data.preferences.TokenManager
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.navigation.Screen
import com.example.karhebti_android.ui.theme.*
import com.example.karhebti_android.viewmodel.GarageViewModel
import com.example.karhebti_android.viewmodel.ServiceViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import com.example.karhebti_android.ui.screens.GarageMapDialog
import androidx.compose.material.icons.filled.Navigation
import com.example.karhebti_android.ui.components.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaragesScreen(
    navController: NavHostController,
    onBackClick: () -> Unit = {},
    onAddGarageClick: () -> Unit = {},
    onGarageClick: (garageId: String) -> Unit = {},
    onModifyGarage: (garageId: String) -> Unit = {},
) {
    val context = LocalContext.current
    val garageViewModel: GarageViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    // ✅ Créer UNE SEULE instance de ServiceViewModel
    val serviceViewModel: ServiceViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val currentUser = TokenManager.getInstance(context).getUser()
    val isPropGarage = currentUser?.role == "propGarage"

    val garagesState by garageViewModel.garagesState.observeAsState()
    val deleteGarageState by garageViewModel.deleteGarageState.observeAsState()
    val servicesState by serviceViewModel.servicesState.observeAsState()

    // ✅ NOUVEAU: Map pour stocker les services par garage
    val servicesByGarage = remember { mutableStateMapOf<String, List<ServiceResponse>>() }

    var selectedFilter by remember { mutableStateOf("Tous") }
    var searchQuery by remember { mutableStateOf("") }
    var sortAscending by remember { mutableStateOf(true) }
    var showRecommendations by remember { mutableStateOf(false) }

    val filters = listOf(
        "Tous", "vidange", "contrôle technique", "réparation pneu", "changement pneu",
        "freinage", "batterie", "climatisation", "échappement", "révision complète",
        "diagnostic électronique", "carrosserie", "peinture", "pare-brise", "suspension",
        "embrayage", "transmission", "injection", "refroidissement", "démarrage",
        "lavage auto", "équilibrage roues", "parallélisme", "système électrique",
        "filtre à air", "filtre à huile", "plaquettes de frein"
    )

    // ✅ Charger les garages au démarrage
    LaunchedEffect(Unit) {
        garageViewModel.getGarages()
    }

    // ✅ NOUVEAU: Charger les services de chaque garage quand la liste des garages change
    LaunchedEffect(garagesState) {
        if (garagesState is Resource.Success) {
            val garages = (garagesState as Resource.Success<List<GarageResponse>>).data ?: emptyList()
            garages.forEach { garage ->
                serviceViewModel.getServicesByGarage(garage.id)
            }
        }
    }

    // ✅ NOUVEAU: Mettre à jour la map des services quand ils changent
    LaunchedEffect(servicesState) {
        if (servicesState is Resource.Success) {
            val allServices = (servicesState as Resource.Success<List<ServiceResponse>>).data ?: emptyList()
            // Regrouper les services par garage
            val garages = (garagesState as? Resource.Success<List<GarageResponse>>)?.data ?: emptyList()
            garages.forEach { garage ->
                val garageServices = allServices.filter { service ->
                    when (val g = service.garage) {
                        is String -> g == garage.id
                        is Map<*, *> -> (g["_id"] as? String) == garage.id
                        is GarageResponse -> g.id == garage.id
                        else -> false
                    }
                }
                servicesByGarage[garage.id] = garageServices
            }
        }
    }

    // Gradient background
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Garages",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        AnimatedVisibility(visible = !showRecommendations) {
                            Text(
                                "Trouvez le garage parfait",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    // Sort button with animation
                    IconButton(onClick = { sortAscending = !sortAscending }) {
                        AnimatedContent(
                            targetState = sortAscending,
                            transitionSpec = { fadeIn() togetherWith fadeOut() }
                        ) { ascending ->
                            Icon(
                                imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = if (ascending) "Tri A-Z" else "Tri Z-A",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Recommendations toggle
                    IconButton(onClick = { showRecommendations = !showRecommendations }) {
                        AnimatedContent(
                            targetState = showRecommendations,
                            transitionSpec = {
                                scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
                            }
                        ) { showRec ->
                            Icon(
                                imageVector = if (showRec) Icons.Default.List else Icons.Default.Stars,
                                contentDescription = if (showRec) "Tous" else "Recommandations",
                                tint = if (showRec) AccentYellow else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        },
        floatingActionButton = {
            if (isPropGarage) {
                AnimatedVisibility(
                    visible = !showRecommendations,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = onAddGarageClick,
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 12.dp
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Ajouter un garage",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search and filters section with animation
                AnimatedVisibility(
                    visible = !showRecommendations,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        // Modern search bar
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        "Rechercher un garage...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        "Rechercher",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                trailingIcon = {
                                    AnimatedVisibility(
                                        visible = searchQuery.isNotBlank(),
                                        enter = scaleIn() + fadeIn(),
                                        exit = scaleOut() + fadeOut()
                                    ) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                Icons.Default.Clear,
                                                "Effacer",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                ),
                                singleLine = true
                            )
                        }

                        // Filter chips with horizontal scroll
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filters) { filter ->
                                ModernFilterChip(
                                    label = filter,
                                    isSelected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter }
                                )
                            }
                        }
                    }
                }

                // Delete state indicator
                AnimatedVisibility(
                    visible = deleteGarageState is Resource.Loading,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    LinearProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Main content
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = showRecommendations,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                        }
                    ) { showRec ->
                        if (showRec) {
                            RecommendationsEmptyState()
                        } else {
                            when (val state = garagesState) {
                                is Resource.Loading -> ModernLoadingState()
                                is Resource.Success -> {
                                    val allGarages = state.data ?: emptyList()
                                    val filteredGarages = allGarages.filter { garage ->
                                        val matchesSearch = searchQuery.isBlank() ||
                                                garage.nom.contains(searchQuery, ignoreCase = true) ||
                                                garage.adresse.contains(searchQuery, ignoreCase = true)

                                        // ✅ CORRIGÉ: Utiliser les services chargés depuis servicesByGarage
                                        val matchesFilter = selectedFilter == "Tous" || run {
                                            val garageServices = servicesByGarage[garage.id] ?: emptyList()
                                            garageServices.any { service ->
                                                service.type.contains(selectedFilter, ignoreCase = true)
                                            }
                                        }

                                        matchesSearch && matchesFilter
                                    }
                                    val sortedGarages = if (sortAscending) {
                                        filteredGarages.sortedBy { it.nom.lowercase() }
                                    } else {
                                        filteredGarages.sortedByDescending { it.nom.lowercase() }
                                    }

                                    if (sortedGarages.isEmpty()) {
                                        ModernEmptyState()
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp),
                                            contentPadding = PaddingValues(vertical = 16.dp)
                                        ) {
                                            items(sortedGarages, key = { it.id }) { garage ->
                                                // ✅ Passer le serviceViewModel partagé
                                                ModernGarageCard(
                                                    garage = garage,
                                                    serviceViewModel = serviceViewModel,
                                                    navController = navController,
                                                    onGarageClick = { onGarageClick(garage.id) },
                                                    onDeleteGarage = { garageViewModel.deleteGarage(it) },
                                                    isPropGarage = isPropGarage
                                                )
                                            }
                                        }
                                    }
                                }
                                is Resource.Error -> {
                                    ModernErrorState(
                                        message = state.message ?: "Erreur",
                                        onRetry = { garageViewModel.getGarages() }
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        modifier = Modifier.scale(scale),
        shadowElevation = if (isSelected) 6.dp else 2.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun ModernGarageCard(
    garage: GarageResponse,
    serviceViewModel: ServiceViewModel,
    navController: NavHostController,
    onGarageClick: () -> Unit,
    onDeleteGarage: (String) -> Unit,
    isPropGarage: Boolean
) {
    val context = LocalContext.current
    val servicesState by serviceViewModel.servicesState.observeAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) } // État pour le dialog de carte
    var isExpanded by remember { mutableStateOf(false) }

    // ✅ État local pour les services de CE garage spécifique
    var garageServices by remember { mutableStateOf<List<ServiceResponse>>(emptyList()) }

    // ✅ Charger les services UNE SEULE FOIS au montage
    LaunchedEffect(garage.id) {
        serviceViewModel.getServicesByGarage(garage.id)
    }

    // ✅ Filtrer les services pour CE garage quand ils changent
    LaunchedEffect(servicesState) {
        if (servicesState is Resource.Success) {
            val allServices = (servicesState as Resource.Success<List<ServiceResponse>>).data ?: emptyList()
            garageServices = allServices.filter { service ->
                when (val g = service.garage) {
                    is String -> g == garage.id
                    is Map<*, *> -> (g["_id"] as? String) == garage.id
                    is GarageResponse -> g.id == garage.id
                    else -> false
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onGarageClick() }
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header avec gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = garage.nom,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = garage.adresse,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Star rating badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentYellow.copy(alpha = 0.2f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = AccentYellow
                            )
                            Text(
                                text = "%.1f".format(garage.noteUtilisateur),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Opening hours with icon
            if (garage.heureOuverture?.isNotBlank() == true &&
                garage.heureFermeture?.isNotBlank() == true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoftBlue.copy(alpha = 0.1f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = SoftBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Horaires",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "${garage.heureOuverture} - ${garage.heureFermeture}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Services section
            when (servicesState) {
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
                is Resource.Success -> {
                    // ✅ Utiliser garageServices au lieu de filtrer à nouveau
                    if (garageServices.isNotEmpty()) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Services disponibles",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${garageServices.size}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(garageServices.take(if (isExpanded) garageServices.size else 3)) { service ->
                                    ServiceTag(service.type)
                                }
                                if (!isExpanded && garageServices.size > 3) {
                                    item {
                                        Surface(
                                            onClick = { isExpanded = true },
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        ) {
                                            Text(
                                                text = "+${garageServices.size - 3}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ✅ Message si aucun service
                        Text(
                            "Aucun service disponible",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
                is Resource.Error -> {
                    Text(
                        "Erreur chargement services",
                        color = AlertRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> {
                    // ✅ État initial ou null
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernActionButton(
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

                ModernActionButton(
                    icon = Icons.Default.LocationOn,
                    label = "Itinéraire",
                    color = SoftBlue,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showMapDialog = true // Ouvrir le dialog de carte
                    }
                )
            }

            // Owner actions
            if (isPropGarage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernActionButton(
                        icon = Icons.Default.Edit,
                        label = "Modifier",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.EditGarage.createRoute(garage.id)) }
                    )
                    ModernActionButton(
                        icon = Icons.Default.Delete,
                        label = "Supprimer",
                        color = AlertRed,
                        modifier = Modifier.weight(1f),
                        onClick = { showDeleteDialog = true }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = AlertRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Supprimer ce garage ?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Cette action est irréversible. Tous les services associés seront également supprimés.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGarage(garage.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler", color = TextSecondary)
                }
            }
        )
    }

    // Map Dialog - Show garage location
    if (showMapDialog) {
        GarageMapDialog(
            context = context,
            garageName = garage.nom,
            garageAddress = garage.adresse,
            latitude = garage.latitude,
            longitude = garage.longitude,
            onDismiss = { showMapDialog = false }
        )
    }
}

@Composable
fun ServiceTag(label: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ModernActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ModernLoadingState() {
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
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            Text(
                "Chargement des garages...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ModernEmptyState() {
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
                imageVector = Icons.Default.Store,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
            Text(
                "Aucun garage trouvé",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Essayez de modifier vos filtres de recherche",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecommendationsEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Stars,
                contentDescription = "Recommandations",
                modifier = Modifier.size(80.dp),
                tint = AccentYellow
            )
            Text(
                "Recommandations de garages",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Fonctionnalité à venir prochainement",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ModernErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Error icon with animated pulse
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale),
                tint = AlertRed
            )

            Text(
                "Erreur de chargement",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Réessayer",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
