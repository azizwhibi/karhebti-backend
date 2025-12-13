package com.example.karhebti_android.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.karhebti_android.data.api.CarResponse
import com.example.karhebti_android.data.api.MaintenanceResponse
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.viewmodel.CarViewModel
import com.example.karhebti_android.viewmodel.MaintenanceViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import java.text.SimpleDateFormat
import java.text.Normalizer
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    vehicleId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val carViewModel: CarViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val maintenanceViewModel: MaintenanceViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    // Partage du GarageViewModel depuis le parent pour éviter sa recréation répétée dans le dialog
    val garageViewModel: com.example.karhebti_android.viewmodel.GarageViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    val carsState by carViewModel.carsState.observeAsState()
    val deleteCarState by carViewModel.deleteCarState.observeAsState()
    val maintenancesState by maintenanceViewModel.maintenancesState.observeAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddMaintenanceDialog by remember { mutableStateOf(false) }
    var hasNavigatedBack by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (carsState == null) carViewModel.getMyCars()
        maintenanceViewModel.getMaintenances()
        // Charger les garages une seule fois depuis le parent
        garageViewModel.getGarages()
    }

    DisposableEffect(Unit) {
        onDispose { carViewModel.resetDeleteState() }
    }

    LaunchedEffect(deleteCarState) {
        if (deleteCarState is Resource.Success && !hasNavigatedBack) {
            hasNavigatedBack = true
            carViewModel.getMyCars()
            carViewModel.resetDeleteState()
            onBackClick()
        }
    }

    if (deleteCarState is Resource.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    "Suppression en cours...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        return
    }

    val car = remember(carsState, vehicleId) {
        (carsState as? Resource.Success)?.data?.find { it.id == vehicleId }
    }

    val carMaintenances = remember(maintenancesState, vehicleId) {
        (maintenancesState as? Resource.Success)?.data?.filter { it.voiture == vehicleId } ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails du véhicule") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            when {
                carsState is Resource.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                car == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Véhicule non trouvé",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Button(onClick = onBackClick) {
                                Text("Retour")
                            }
                        }
                    }
                }
                else -> {
                    VehicleDetailContent(
                        car = car,
                        maintenances = carMaintenances,
                        maintenancesLoading = maintenancesState is Resource.Loading,
                        onDeleteClick = { showDeleteDialog = true },
                        onAddMaintenanceClick = { showAddMaintenanceDialog = true }
                    )
                }
            }
        }
    }

    if (showDeleteDialog && car != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer le véhicule ?") },
            text = {
                Text(
                    "Voulez-vous vraiment supprimer ${car.marque} ${car.modele} ? Cette action est irréversible."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        carViewModel.deleteCar(car.id)
                    },
                    enabled = deleteCarState !is Resource.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    if (deleteCarState is Resource.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Text("Supprimer")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = deleteCarState !is Resource.Loading
                ) { Text("Annuler") }
            }
        )
    }

    if (showAddMaintenanceDialog && car != null) {
        // Passer les viewModels depuis le parent pour éviter recréations répétées dans le dialog
        AddMaintenanceDialogWithPrefilledCar(
            onDismiss = { showAddMaintenanceDialog = false },
            prefilledCarId = vehicleId,
            onSuccess = {
                showAddMaintenanceDialog = false
                maintenanceViewModel.getMaintenances()
            },
            maintenanceViewModel = maintenanceViewModel,
            garageViewModel = garageViewModel,
            carViewModel = carViewModel
        )
    }
}

@Composable
fun VehicleDetailContent(
    car: CarResponse,
    maintenances: List<MaintenanceResponse>,
    maintenancesLoading: Boolean,
    onDeleteClick: () -> Unit,
    onAddMaintenanceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero section with theme gradient and car image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val fullImageUrl = com.example.karhebti_android.util.ImageUrlHelper.getFullImageUrl(car.imageUrl)
            // Car image background
            AsyncImage(
                model = fullImageUrl ?: "https://cdn-icons-png.flaticon.com/512/743/743007.png",
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            // Gradient overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
            // Foreground content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Text(
                    text = "${car.marque} ${car.modele}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 32.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = car.immatriculation,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Caractéristiques",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernInfoCard(
                    icon = Icons.Default.CalendarToday,
                    label = "Année",
                    value = car.annee.toString(),
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                ModernInfoCard(
                    icon = Icons.Default.LocalGasStation,
                    label = "Carburant",
                    value = car.typeCarburant,
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            MaintenanceSectionCard(
                maintenances = maintenances,
                isLoading = maintenancesLoading,
                onAddClick = onAddMaintenanceClick
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedButton(
                onClick = onDeleteClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.error
                        )
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Supprimer ce véhicule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ModernInfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun MaintenanceSectionCard(
    maintenances: List<MaintenanceResponse>,
    isLoading: Boolean,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Entretiens",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${maintenances.size} entretien(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Ajouter",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else if (maintenances.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Aucun entretien",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Commencez par ajouter un entretien",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    maintenances.take(5).forEach { maintenance ->
                        CompactMaintenanceCard(maintenance = maintenance)
                    }
                    if (maintenances.size > 5) {
                        Text(
                            text = "Et ${maintenances.size - 5} autre(s)...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompactMaintenanceCard(maintenance: MaintenanceResponse) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
    val formattedDate = runCatching { dateFormat.format(maintenance.date) }.getOrElse { "Date inconnue" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = maintenance.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "${maintenance.cout} DT",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceDialogWithPrefilledCar(
    onDismiss: () -> Unit,
    prefilledCarId: String,
    onSuccess: () -> Unit,
    maintenanceViewModel: MaintenanceViewModel,
    garageViewModel: com.example.karhebti_android.viewmodel.GarageViewModel,
    carViewModel: CarViewModel
) {
    val context = LocalContext.current

    val createMaintenanceState by maintenanceViewModel.createMaintenanceState.observeAsState()
    val garagesState by garageViewModel.garagesState.observeAsState()
    val carsState by carViewModel.carsState.observeAsState()

    // Utiliser rememberSaveable pour éviter la perte d'état lors de recompositions
    var type by rememberSaveable { mutableStateOf("vidange") }
    // On sauvegarde la date en millisecondes (Long) car Date n'est pas directement saveable
    var selectedDateMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var cout by rememberSaveable { mutableStateOf("") }
    var selectedGarageId by rememberSaveable { mutableStateOf("") }
    var expandedType by rememberSaveable { mutableStateOf(false) }
    var expandedGarage by rememberSaveable { mutableStateOf(false) }
    var hasTriggeredCreation by rememberSaveable { mutableStateOf(false) }

    // Liste correcte des types (une entrée par string)
    val types = listOf(
        "vidange",
        "contrôle technique",
        "réparation pneu",
        "changement pneu",
        "freinage",
        "batterie",
        "climatisation",
        "échappement",
        "révision complète",
        "diagnostic électronique",
        "carrosserie",
        "peinture",
        "pare-brise",
        "suspension",
        "embrayage",
        "transmission",
        "injection",
        "refroidissement",
        "démarrage",
        "lavage auto",
        "équilibrage roues",
        "parallélisme",
        "système électrique",
        "filtre à air",
        "filtre à huile",
        "plaquettes de frein"
    )
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
    val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

    LaunchedEffect(createMaintenanceState) {
        if (createMaintenanceState is Resource.Success && hasTriggeredCreation) {
            onSuccess()
        }
    }

    // Log pour déboguer
    LaunchedEffect(garagesState) {
        when (val state = garagesState) {
            is Resource.Loading -> Log.d("AddMaintenance", "Chargement des garages...")
            is Resource.Success -> {
                val garages = state.data
                if (garages != null) {
                    Log.d("AddMaintenance", "Garages chargés: ${garages.size} garages")
                    garages.forEach { garage ->
                        Log.d("AddMaintenance", "Garage: ${garage.nom}, Services: ${garage.serviceTypes}")
                    }
                } else {
                    Log.d("AddMaintenance", "Garages chargés mais data est null")
                }
            }
            is Resource.Error -> {
                val error = state.message
                Log.e("AddMaintenance", "Erreur chargement garages: $error")
            }
            null -> Log.d("AddMaintenance", "garagesState est null")
        }
    }

    val allGarages = remember(garagesState) {
        (garagesState as? Resource.Success<List<com.example.karhebti_android.data.api.GarageResponse>>)?.data ?: emptyList()
    }

    // Si la liste des garages est vide au moment de l'ouverture du dialog, demander explicitement le chargement
    LaunchedEffect(allGarages.isEmpty()) {
        if (allGarages.isEmpty()) {
            Log.w("AddMaintenance", "allGarages vide au moment d'ouverture du dialog, requête de chargement")
            garageViewModel.getGarages()
        }
    }

    // Normalisation utile pour comparer en ignorant casse, espaces et accents
    val normalize: (String) -> String = { s ->
        Normalizer.normalize(s ?: "", Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase(Locale.getDefault())
            .trim()
    }

    val filteredGarages = remember(type, allGarages) {
        val target = normalize(type)
        val filtered = allGarages.filter { garage ->
            val services = garage.serviceTypes ?: emptyList()
            // Debug des services du garage (limité)
            if (services.isNotEmpty()) {
                Log.d("AddMaintenance", "Garage=${garage.nom}, services=${services.take(5)}")
            }
            // découper les valeurs si le backend a envoyé plusieurs types dans une seule string
            val parts = services.flatMap { serviceTypeRaw ->
                serviceTypeRaw.split(Regex("[,;/]"))
                    .map { normalize(it) }
                    .filter { it.isNotBlank() }
            }
            // si pas de séparateur, parts contiendra la version normalisée complète
            parts.any { s ->
                // accepter égalité ou inclusion (ex: "vidange moteur" contient "vidange")
                s == target || s.contains(target) || target.contains(s)
            }
        }
        Log.d("AddMaintenance", "Type sélectionné: $type (normalized='$target'), Garages filtrés: ${filtered.size}/${allGarages.size}")
        filtered
    }

    // Si aucun garage compatible trouvé, on affichera la liste complète (UX plus conviviale)
    // Construire une liste ordonnée : d'abord les compatibles, puis les autres
    val orderedGarages = remember(filteredGarages, allGarages) {
        if (allGarages.isEmpty()) return@remember emptyList<com.example.karhebti_android.data.api.GarageResponse>()
        if (filteredGarages.isEmpty()) {
            Log.w("AddMaintenance", "Aucun garage compatible trouvé pour '$type' — affichage de tous les garages")
            allGarages
        } else {
            val others = allGarages.filter { ag -> filteredGarages.none { it.id == ag.id } }
            filteredGarages + others
        }
    }

    val carName = remember(carsState, prefilledCarId) {
        val car = (carsState as? Resource.Success)?.data?.find { it.id == prefilledCarId }
        car?.let { "${it.marque} ${it.modele}" } ?: "Véhicule sélectionné"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false),
        title = {
            Text(
                "Nouvel entretien",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Véhicule",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = carName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = it }
                ) {
                    OutlinedTextField(
                        value = type.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type d'entretien") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        types.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    type = item
                                    expandedType = false
                                    selectedGarageId = ""
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = selectedDateMillis?.let { dateFormat.format(java.util.Date(it)) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    placeholder = { Text("Sélectionner une date") },
                    trailingIcon = {
                        IconButton(onClick = {
                            val calendar = java.util.Calendar.getInstance()
                            val datePicker = android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(year, month, day)
                                    selectedDateMillis = calendar.timeInMillis
                                },
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH),
                                calendar.get(java.util.Calendar.DAY_OF_MONTH)
                            )
                            datePicker.show()
                        }) {
                            Icon(Icons.Default.CalendarToday, "Sélectionner date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cout,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) cout = it },
                    label = { Text("Coût (DT)") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expandedGarage,
                    onExpandedChange = { expandedGarage = it }
                ) {
                    OutlinedTextField(
                        value = orderedGarages.find { it.id == selectedGarageId }?.nom ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Garage (optionnel)") },
                        placeholder = { Text(if (filteredGarages.isEmpty()) "Aucun garage compatible — affichage de tous les garages" else "Sélectionner un garage") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGarage) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedGarage,
                        onDismissRequest = { expandedGarage = false }
                    ) {
                        if (orderedGarages.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Aucun garage disponible") },
                                onClick = { }
                            )
                        } else {
                            // Afficher d'abord les compatibles (si présents)
                            val hasFiltered = filteredGarages.isNotEmpty()
                            if (hasFiltered) {
                                filteredGarages.forEach { garage ->
                                    DropdownMenuItem(
                                        text = { Text(garage.nom) },
                                        onClick = {
                                            selectedGarageId = garage.id
                                            expandedGarage = false
                                        }
                                    )
                                }
                                // Ajouter une séparation et un header pour les autres garages s'il y en a
                                val others = orderedGarages.drop(filteredGarages.size)
                                if (others.isNotEmpty()) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    DropdownMenuItem(
                                        text = { Text("Autres garages", style = MaterialTheme.typography.labelSmall) },
                                        onClick = { /* header non cliquable */ },
                                        enabled = false
                                    )
                                    others.forEach { garage ->
                                        DropdownMenuItem(
                                            text = { Text(garage.nom) },
                                            onClick = {
                                                selectedGarageId = garage.id
                                                expandedGarage = false
                                            }
                                        )
                                    }
                                }
                            } else {
                                // Aucun garage compatible => afficher tous
                                orderedGarages.forEach { garage ->
                                    DropdownMenuItem(
                                        text = { Text(garage.nom) },
                                        onClick = {
                                            selectedGarageId = garage.id
                                            expandedGarage = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (createMaintenanceState is Resource.Error) {
                    Text(
                        text = (createMaintenanceState as Resource.Error).message ?: "Erreur",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedDateMillis?.let { dateMillis ->
                        val coutValue = cout.toDoubleOrNull() ?: 0.0
                        val dateStr = isoDateFormat.format(java.util.Date(dateMillis))
                        maintenanceViewModel.createMaintenance(
                            type = type,
                            date = dateStr,
                            cout = coutValue,
                            garage = selectedGarageId.ifEmpty { "" },
                            voiture = prefilledCarId
                        )
                        hasTriggeredCreation = true
                    }
                },
                enabled = selectedDateMillis != null && cout.isNotEmpty() && createMaintenanceState !is Resource.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (createMaintenanceState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Ajouter")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = createMaintenanceState !is Resource.Loading
            ) { Text("Annuler") }
        }
    )
}
