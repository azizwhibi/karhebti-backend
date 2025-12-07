package com.example.karhebti_android.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.api.MaintenanceResponse
import com.example.karhebti_android.data.api.MaintenanceExtendedResponse
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.ui.theme.*
import com.example.karhebti_android.viewmodel.MaintenanceViewModel
import com.example.karhebti_android.viewmodel.CarViewModel
import com.example.karhebti_android.viewmodel.GarageViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import com.example.karhebti_android.viewmodel.EntretiensFilterViewModel
import com.example.karhebti_android.util.effectiveDateSafe
import com.example.karhebti_android.data.repository.TranslationManager
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import java.util.Locale



// Backend-Integrated EntretiensScreen with Translation Support
// All maintenance data from API, Create/Delete operations call backend
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntretiensScreen(
    onBackClick: () -> Unit = {},
    onMaintenanceClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val maintenanceViewModel: MaintenanceViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val carViewModel: CarViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val garageViewModel: GarageViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    // New filter/search/sort ViewModel - create with SavedState support
    val filterViewModel: EntretiensFilterViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return EntretiensFilterViewModel(
                    context.applicationContext as android.app.Application,
                    SavedStateHandle()
                ) as T
            }
        }
    )

    // Translation manager setup
    val db = com.example.karhebti_android.data.database.AppDatabase.getInstance(context.applicationContext)
    val translationRepository = com.example.karhebti_android.data.repository.TranslationRepository(
        apiService = com.example.karhebti_android.data.api.RetrofitClient.apiService,
        translationDao = db.translationDao(),
        languageCacheDao = db.languageCacheDao(),
        languageListCacheDao = db.languageListCacheDao()
    )
    val translationManager = remember { TranslationManager.getInstance(translationRepository, context) }
    val coroutineScope = rememberCoroutineScope()
    val currentLanguage by translationManager.currentLanguage.collectAsState()

    // Translated UI strings
    var maintenanceTitle by remember { mutableStateOf("Entretiens") }
    var backText by remember { mutableStateOf("Retour") }
    var addMaintenanceText by remember { mutableStateOf("Ajouter entretien") }
    var upcomingTabText by remember { mutableStateOf("À venir") }
    var historyTabText by remember { mutableStateOf("Historique") }
    var searchPlaceholder by remember { mutableStateOf("Rechercher") }
    var clearText by remember { mutableStateOf("Effacer") }
    var allText by remember { mutableStateOf("Tous") }
    var urgentText by remember { mutableStateOf("Urgent") }
    var soonText by remember { mutableStateOf("Bientôt") }
    var plannedText by remember { mutableStateOf("Prévu") }
    var sortByText by remember { mutableStateOf("Trier") }
    var dateText by remember { mutableStateOf("Date") }
    var costText by remember { mutableStateOf("Coût") }
    var mileageText by remember { mutableStateOf("Kilométrage") }
    var createdText by remember { mutableStateOf("Créé") }
    var noMaintenanceText by remember { mutableStateOf("Aucun entretien") }

    // Update translations when language changes
    LaunchedEffect(currentLanguage) {
        coroutineScope.launch {
            maintenanceTitle = translationManager.translate("maintenance_title", "Entretiens", currentLanguage)
            backText = translationManager.translate("back", "Retour", currentLanguage)
            addMaintenanceText = translationManager.translate("add_maintenance", "Ajouter entretien", currentLanguage)
            upcomingTabText = translationManager.translate("upcoming", "À venir", currentLanguage)
            historyTabText = translationManager.translate("history", "Historique", currentLanguage)
            searchPlaceholder = translationManager.translate("search", "Rechercher", currentLanguage)
            clearText = translationManager.translate("clear", "Effacer", currentLanguage)
            allText = translationManager.translate("all", "Tous", currentLanguage)
            urgentText = translationManager.translate("urgent", "Urgent", currentLanguage)
            soonText = translationManager.translate("soon", "Bientôt", currentLanguage)
            plannedText = translationManager.translate("planned", "Prévu", currentLanguage)
            sortByText = translationManager.translate("sort_by", "Trier", currentLanguage)
            dateText = translationManager.translate("date", "Date", currentLanguage)
            costText = translationManager.translate("cost", "Coût", currentLanguage)
            mileageText = translationManager.translate("mileage", "Kilométrage", currentLanguage)
            createdText = translationManager.translate("created", "Créé", currentLanguage)
            noMaintenanceText = translationManager.translate("no_maintenance", "Aucun entretien", currentLanguage)
        }
    }

    // Observe states
    val maintenancesState by maintenanceViewModel.maintenancesState.observeAsState()
    val createMaintenanceState by maintenanceViewModel.createMaintenanceState.observeAsState()
    val carsState by carViewModel.carsState.observeAsState()
    val garagesState by garageViewModel.garagesState.observeAsState()

    val filterState by filterViewModel.maintenancesState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(upcomingTabText, historyTabText)
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<MaintenanceResponse?>(null) }
    val scope = rememberCoroutineScope()

    // Local UI state for search & sort controls (bound to viewModel SavedStateHandle)
    var searchText by remember { mutableStateOf(filterViewModel.searchQuery) }
    var selectedUrgency by remember { mutableStateOf(allText) }
    var sortField by remember { mutableStateOf(filterViewModel.sortField) }
    var sortOrder by remember { mutableStateOf(filterViewModel.sortOrder) }

    // Load data on screen start
    LaunchedEffect(Unit) {
        maintenanceViewModel.getMaintenances()
        carViewModel.getMyCars()
        garageViewModel.getGarages()
        // initialize filter search
        filterViewModel.searchMaintenances()
    }

    // Sync local UI state to ViewModel when changed with automatic searches (debounced for search)
    LaunchedEffect(searchText) {
        // debounce typing
        delay(400L)
        filterViewModel.searchQuery = searchText
        // reset to first page when performing a new search
        filterViewModel.searchMaintenances(1)
    }
    LaunchedEffect(sortField, sortOrder) {
        filterViewModel.sortField = sortField
        filterViewModel.sortOrder = sortOrder
        filterViewModel.searchMaintenances(1)
    }

    // Handle create result
    LaunchedEffect(createMaintenanceState) {
        when (createMaintenanceState) {
            is Resource.Success -> {
                showAddDialog = false
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(maintenanceTitle) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, backText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, addMaintenanceText)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search (first row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(searchPlaceholder) },
                    singleLine = true,
                    trailingIcon = {
                        if (searchText.isNotBlank()) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = clearText)
                            }
                        }
                    }
                )
            }

            // Filters & Sort (second row under search)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status filter dropdown
                var statusExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    Button(onClick = { statusExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedUrgency)
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.padding(start = 6.dp))
                    }
                    DropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        DropdownMenuItem(text = { Text(allText) }, onClick = { selectedUrgency = allText; statusExpanded = false })
                        DropdownMenuItem(text = { Text(urgentText) }, onClick = { selectedUrgency = urgentText; statusExpanded = false })
                        DropdownMenuItem(text = { Text(soonText) }, onClick = { selectedUrgency = soonText; statusExpanded = false })
                        DropdownMenuItem(text = { Text(plannedText) }, onClick = { selectedUrgency = plannedText; statusExpanded = false })
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Sort field dropdown
                var sortExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    Button(onClick = { sortExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        // compute label to avoid unnecessary Elvis on non-nullable type
                        val sortLabel = when (sortField) {
                            "" -> "-"
                            "dueAt" -> dateText
                            "cout" -> costText
                            "mileage" -> mileageText
                            "createdAt" -> createdText
                            else -> sortField
                        }
                        Text("$sortByText: $sortLabel")
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.padding(start = 6.dp))
                    }
                    DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        DropdownMenuItem(text = { Text("$dateText (dueAt)") }, onClick = { sortField = "dueAt"; sortExpanded = false })
                        DropdownMenuItem(text = { Text("$costText (cout)") }, onClick = { sortField = "cout"; sortExpanded = false })
                        DropdownMenuItem(text = { Text("$mileageText (mileage)") }, onClick = { sortField = "mileage"; sortExpanded = false })
                        DropdownMenuItem(text = { Text("$createdText (createdAt)") }, onClick = { sortField = "createdAt"; sortExpanded = false })
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Order toggle
                IconButton(onClick = { sortOrder = if (sortOrder == "asc") "desc" else "asc" }) {
                    Icon(if (sortOrder == "asc") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = "Order")
                }
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                // If filter state exists and is successful, show paginated filtered results
                if (filterState is Resource.Success) {
                    val paged = (filterState as Resource.Success).data?.data ?: emptyList()
                    val now = Date()
                    val displayed = paged.filter { m ->
                        // Use helper to safely get the effective date (dueAt preferred, fall back to date)
                        val effective = m.effectiveDateSafe()

                        val dateMatches = effective?.let { d ->
                            if (selectedTab == 0) d.after(now) || d == now
                            else d.before(now)
                        } ?: false

                        if (!dateMatches) false
                        else if (selectedUrgency == allText) true
                        else {
                            val daysUntil = effective?.let { ((it.time - now.time) / (1000 * 60 * 60 * 24)).toInt() } ?: Int.MAX_VALUE
                            when (selectedUrgency) {
                                urgentText -> daysUntil in 0..7
                                soonText -> daysUntil in 8..30
                                plannedText -> daysUntil > 30
                                else -> true
                            }
                        }
                    }

                    // Final client-side filter that can match related entities (cars, garages)
                    val carsList = when (carsState) {
                        is Resource.Success -> (carsState as Resource.Success).data ?: emptyList()
                        else -> emptyList()
                    }
                    val garagesList = when (garagesState) {
                        is Resource.Success -> (garagesState as Resource.Success).data ?: emptyList()
                        else -> emptyList()
                    }

                    val searchQ = filterViewModel.searchQuery.takeIf { it.isNotBlank() }?.lowercase(Locale.getDefault())
                    val finalDisplayed = if (searchQ != null) {
                        displayed.filter { m ->
                            val car = m.voiture?.let { id -> carsList.find { it.id == id } }
                            val garage = m.garage?.let { id -> garagesList.find { it.id == id } }

                            val combined = listOfNotNull(
                                m.title,
                                m.type,
                                m.notes,
                                m.tags.joinToString(" "),
                                m.status,
                                m.cout.toString(),
                                m.mileage?.toString(),
                                car?.marque,
                                car?.modele,
                                car?.immatriculation,
                                garage?.nom
                            ).joinToString(" ").lowercase(Locale.getDefault())

                            combined.contains(searchQ)
                        }
                    } else displayed

                    if (finalDisplayed.isEmpty()) {
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
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = TextSecondary.copy(alpha = 0.5f)
                                )
                                Text(
                                    if (selectedTab == 0) noMaintenanceText else "Aucun historique",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    if (selectedTab == 0) "Planifiez votre prochain entretien" else "Vos entretiens passés apparaîtront ici",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(finalDisplayed, key = { it.id }) { maintenance ->
                                // Safely extract cars list from Resource
                                val cars = when (carsState) {
                                    is Resource.Success -> (carsState as Resource.Success).data ?: emptyList()
                                    else -> emptyList()
                                }
                                // Safely extract garages list as well
                                val garages = when (garagesState) {
                                    is Resource.Success -> (garagesState as Resource.Success).data ?: emptyList()
                                    else -> emptyList()
                                }

                                MaintenanceCardExtended(
                                    maintenance = maintenance,
                                    onDelete = {
                                        // Create a MaintenanceResponse object for the delete dialog
                                        val maintenanceResponse = MaintenanceResponse(
                                            id = maintenance.id,
                                            type = maintenance.type,
                                            date = maintenance.date,
                                            cout = maintenance.cout,
                                            voiture = maintenance.voiture,
                                            garage = maintenance.garage,
                                            status = maintenance.status
                                        )
                                        showDeleteDialog = maintenanceResponse
                                    },
                                    onClick = { onMaintenanceClick(maintenance.id) },
                                    cars = cars,
                                    garages = garages
                                )
                            }
                        }
                    }
                } else {
                    // Fallback to existing non-paginated API state
                    when (val state = maintenancesState) {
                        is Resource.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        "Chargement des entretiens...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        is Resource.Success -> {
                            val allMaintenances = state.data ?: emptyList()
                            val now = Date()

                            // Filter based on tab - date is non-nullable so compare directly
                            val displayedMaintenances = allMaintenances.filter { m ->
                                val date = m.date
                                val isUpcoming = selectedTab == 0
                                val dateMatches = if (isUpcoming) date.after(now) || date == now else date.before(now)
                                if (!dateMatches) false
                                else if (selectedUrgency == allText) true
                                else {
                                    val daysUntil = ((date.time - now.time) / (1000 * 60 * 60 * 24)).toInt()
                                    when (selectedUrgency) {
                                        urgentText -> daysUntil in 0..7
                                        soonText -> daysUntil in 8..30
                                        plannedText -> daysUntil > 30
                                        else -> true
                                    }
                                }
                            }

                            if (displayedMaintenances.isEmpty()) {
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
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = TextSecondary.copy(alpha = 0.5f)
                                        )
                                        Text(
                                            if (selectedTab == 0) noMaintenanceText else "Aucun historique",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = TextPrimary
                                        )
                                        Text(
                                            if (selectedTab == 0) "Planifiez votre prochain entretien" else "Vos entretiens passés apparaîtront ici",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(displayedMaintenances, key = { it.id }) { maintenance ->
                                        val cars = when (carsState) {
                                            is Resource.Success -> (carsState as Resource.Success).data ?: emptyList()
                                            else -> emptyList()
                                        }
                                        val garages = when (garagesState) {
                                            is Resource.Success -> (garagesState as Resource.Success).data ?: emptyList()
                                            else -> emptyList()
                                        }

                                        // Safely extract garages only if needed later (avoid unused variable)

                                        MaintenanceCardBackendIntegrated(
                                            maintenance = maintenance,
                                            onDelete = { showDeleteDialog = maintenance },
                                            onClick = { onMaintenanceClick(maintenance.id) },
                                            cars = cars
                                            , garages = garages
                                        )
                                    }
                                }
                            }
                        }
                        is Resource.Error -> {
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
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = AlertRed
                                    )
                                    Text(
                                        "Erreur de chargement",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        state.message ?: "Une erreur est survenue",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = { maintenanceViewModel.getMaintenances() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.Refresh, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Réessayer")
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    // Add maintenance dialog
    if (showAddDialog) {
        AddMaintenanceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { type, date, cout, garageId, voitureId ->
                maintenanceViewModel.createMaintenance(type, date, cout, garageId, voitureId)
            },
            createState = createMaintenanceState,
            carsState = carsState,
            garagesState = garagesState
        )
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { maintenance ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Supprimer l'entretien ?") },
            text = { Text("Voulez-vous vraiment supprimer cet entretien ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            maintenanceViewModel.deleteMaintenance(maintenance.id)
                            showDeleteDialog = null
                            delay(300L) // Reduced delay for faster feedback
                            // Refresh both data sources
                            maintenanceViewModel.getMaintenances()
                            filterViewModel.searchMaintenances()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AlertRed)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun MaintenanceCardBackendIntegrated(
    maintenance: MaintenanceResponse,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    cars: List<com.example.karhebti_android.data.api.CarResponse> = emptyList(),
    garages: List<com.example.karhebti_android.data.api.GarageResponse> = emptyList()
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRANCE)

    val daysUntil = try {
        ((maintenance.date.time - Date().time) / (1000 * 60 * 60 * 24)).toInt()
    } catch (_: Exception) {
        Int.MAX_VALUE
    }

    val (urgencyLabel, urgencyColor, urgencyIcon) = when {
        daysUntil == Int.MAX_VALUE -> Triple("Date inconnue", MaterialTheme.colorScheme.surfaceVariant, Icons.Default.CalendarToday)
        daysUntil < 0 -> Triple("Terminé", MaterialTheme.colorScheme.surfaceVariant, Icons.Default.CheckCircle)
        daysUntil == 0 -> Triple("Aujourd'hui", AlertRed, Icons.Default.Warning)
        daysUntil <= 7 -> Triple("Urgent", AlertRed, Icons.Default.Warning)
        daysUntil <= 30 -> Triple("Bientôt", AccentYellow, Icons.Default.Schedule)
        else -> Triple("Prévu", MaterialTheme.colorScheme.tertiary, Icons.Default.Event)
    }

    // Look up the car from the cars list
    val car = maintenance.voiture?.let { carId ->
        cars.find { it.id == carId }
    }

    // Look up the garage from the garages list
    val garage = maintenance.garage?.let { garageId ->
        garages.find { it.id == garageId }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header section with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Icon container
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = maintenance.type.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            car?.let {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${it.marque} ${it.modele}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Delete button - more prominent
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = AlertRed.copy(alpha = 0.1f),
                            contentColor = AlertRed
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Details section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Urgency badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = urgencyColor.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            urgencyIcon,
                            contentDescription = null,
                            tint = urgencyColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            urgencyLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = urgencyColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            dateFormat.format(maintenance.date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Cost and garage info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Coût estimé",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                "${maintenance.cout}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "DT",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    garage?.let { g ->
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Garage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Garage,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    g.nom,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// New card for MaintenanceExtendedResponse returned by the paginated search API
@Composable
fun MaintenanceCardExtended(
    maintenance: MaintenanceExtendedResponse,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    cars: List<com.example.karhebti_android.data.api.CarResponse> = emptyList(),
    garages: List<com.example.karhebti_android.data.api.GarageResponse> = emptyList()
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRANCE)

    val daysUntil = try {
        // Try to read dueAt; if that fails at runtime (missing field), fall back to date.
        val target = maintenance.effectiveDateSafe()
        target?.let { ((it.time - Date().time) / (1000 * 60 * 60 * 24)).toInt() } ?: Int.MAX_VALUE
    } catch (_: Exception) {
        Int.MAX_VALUE
    }

    val (urgencyLabel, urgencyColor, urgencyIcon) = when {
        daysUntil == Int.MAX_VALUE -> Triple("Date inconnue", MaterialTheme.colorScheme.surfaceVariant, Icons.Default.CalendarToday)
        daysUntil < 0 -> Triple("Terminé", MaterialTheme.colorScheme.surfaceVariant, Icons.Default.CheckCircle)
        daysUntil == 0 -> Triple("Aujourd'hui", AlertRed, Icons.Default.Warning)
        daysUntil <= 7 -> Triple("Urgent", AlertRed, Icons.Default.Warning)
        daysUntil <= 30 -> Triple("Bientôt", AccentYellow, Icons.Default.Schedule)
        else -> Triple("Prévu", MaterialTheme.colorScheme.tertiary, Icons.Default.Event)
    }

    val car = maintenance.voiture?.let { carId -> cars.find { it.id == carId } }
    val garage = maintenance.garage?.let { garageId -> garages.find { it.id == garageId } }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header section with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Icon container
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = maintenance.type.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            car?.let {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${it.marque} ${it.modele}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Delete button - more prominent
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = AlertRed.copy(alpha = 0.1f),
                            contentColor = AlertRed
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Details section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Urgency badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = urgencyColor.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            urgencyIcon,
                            contentDescription = null,
                            tint = urgencyColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            urgencyLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = urgencyColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            dateFormat.format(maintenance.date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Cost and garage info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Coût estimé",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                "${maintenance.cout}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "DT",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    garage?.let { g ->
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Garage",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Garage,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    g.nom,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Double, String, String) -> Unit,
    createState: Resource<MaintenanceResponse>?,
    carsState: Resource<*>?,
    garagesState: Resource<*>?
) {
    var type by remember { mutableStateOf("vidange") }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var cout by remember { mutableStateOf("") }
    var selectedCarId by remember { mutableStateOf("") }
    var selectedGarageId by remember { mutableStateOf("") }
    var expandedType by remember { mutableStateOf(false) }
    var expandedCar by remember { mutableStateOf(false) }
    var expandedGarage by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val types = listOf("vidange", "révision", "réparation", "pneus", "freins")
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
    val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

    // Safely extract cars and garages from state without unchecked casts
    val cars = remember(carsState) {
        when (carsState) {
            is Resource.Success -> {
                val d = carsState.data
                if (d is List<*>) d.filterIsInstance<com.example.karhebti_android.data.api.CarResponse>() else emptyList()
            }
            else -> emptyList()
        }
    }

    val allGarages = remember(garagesState) {
        when (garagesState) {
            is Resource.Success -> {
                val d = garagesState.data
                if (d is List<*>) d.filterIsInstance<com.example.karhebti_android.data.api.GarageResponse>() else emptyList()
            }
            else -> emptyList()
        }
    }

    // Filter garages by selected service type
    val filteredGarages = remember(type, allGarages) {
        // If no garages have serviceTypes configured, show all garages
        val hasServiceTypes = allGarages.any { it.serviceTypes?.isNotEmpty() == true }
        if (!hasServiceTypes) {
            // No serviceTypes configured, show all garages
            allGarages
        } else {
            // Filter by serviceTypes, but also include garages without serviceTypes to avoid hiding them
            allGarages.filter { garage ->
                garage.serviceTypes == null ||
                garage.serviceTypes.isEmpty() ||
                garage.serviceTypes.contains(type)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel entretien") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = type.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type d'entretien") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.menuAnchor()
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
                                    selectedGarageId = "" // Reset garage when type changes
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                // Car selection dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCar,
                    onExpandedChange = { expandedCar = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (selectedCarId.isNotBlank()) {
                            cars.find { it.id == selectedCarId }?.let { "${it.marque} ${it.modele}" } ?: ""
                        } else "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Véhicule") },
                        placeholder = { Text("Sélectionner un véhicule") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCar) },
                        isError = cars.isEmpty(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCar,
                        onDismissRequest = { expandedCar = false }
                    ) {
                        if (cars.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Aucun véhicule disponible") },
                                onClick = { expandedCar = false }
                            )
                        } else {
                            cars.forEach { car ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${car.marque} ${car.modele}")
                                            Text(
                                                car.immatriculation,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCarId = car.id
                                        expandedCar = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Garage selection dropdown (filtered by service type)
                ExposedDropdownMenuBox(
                    expanded = expandedGarage,
                    onExpandedChange = { expandedGarage = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (selectedGarageId.isNotBlank()) {
                            filteredGarages.find { it.id == selectedGarageId }?.nom ?: ""
                        } else "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Garage") },
                        placeholder = { Text("Sélectionner un garage") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGarage) },
                        isError = filteredGarages.isEmpty(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedGarage,
                        onDismissRequest = { expandedGarage = false }
                    ) {
                        if (filteredGarages.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Aucun garage pour ce service",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                onClick = { expandedGarage = false }
                            )
                        } else {
                            filteredGarages.forEach { garage ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(garage.nom)
                                            Text(
                                                garage.adresse,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = AccentYellow
                                                )
                                                Text(
                                                    garage.noteUtilisateur.toString(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedGarageId = garage.id
                                        expandedGarage = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Date picker field
                OutlinedTextField(
                    value = selectedDate?.let { dateFormat.format(it) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    placeholder = { Text("Sélectionner une date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, "Choisir date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cout,
                    onValueChange = { cout = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Coût (DT)") },
                    placeholder = { Text("Montant en dinars") },
                    singleLine = true,
                    leadingIcon = {
                        Text("DT", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                when (createState) {
                    is Resource.Loading -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is Resource.Error -> {
                        Text(
                            text = createState.message ?: "Erreur",
                            color = AlertRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val coutValue = cout.toDoubleOrNull()
                    val dateString = selectedDate?.let { isoDateFormat.format(it) }
                    if (type.isNotBlank() && dateString != null && coutValue != null &&
                        selectedCarId.isNotBlank() && selectedGarageId.isNotBlank()) {
                        onAdd(type, dateString, coutValue, selectedGarageId, selectedCarId)
                    }
                },
                enabled = createState !is Resource.Loading &&
                         type.isNotBlank() && selectedDate != null &&
                         cout.toDoubleOrNull() != null &&
                         selectedCarId.isNotBlank() && selectedGarageId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
            ) {
                if (createState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Ajouter")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = createState !is Resource.Loading
            ) {
                Text("Annuler")
            }
        }
    )

    // Date Picker Dialog
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        selectedDate?.let { calendar.time = it }

        DatePickerDialog(
            LocalContext.current,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.time
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { showDatePicker = false }
            show()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EntretiensScreenPreview() {
    KarhebtiandroidTheme {
        EntretiensScreen()
    }
}
