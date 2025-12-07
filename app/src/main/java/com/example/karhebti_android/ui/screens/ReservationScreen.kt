package com.example.karhebti_android.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.karhebti_android.data.api.ServiceResponse
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.viewmodel.GarageViewModel
import com.example.karhebti_android.viewmodel.ReservationViewModel
import com.example.karhebti_android.viewmodel.ServiceViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import com.example.karhebti_android.ui.theme.*
import com.example.karhebti_android.viewmodel.RepairBayViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationScreen(
    garageId: String,
    navController: NavHostController,
    onReservationSuccess: () -> Unit = {}
) {
    val context = LocalContext.current

    val reservationViewModel: ReservationViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val serviceViewModel: ServiceViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val garageViewModel: GarageViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val repairBayViewModel: RepairBayViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    val servicesState by serviceViewModel.servicesState.observeAsState()
    val createState by reservationViewModel.createReservationState.observeAsState()
    val garagesState by garageViewModel.garagesState.observeAsState()
    val availableBaysState by repairBayViewModel.availableBaysState.observeAsState()

    // État pour la sélection de date et heure
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var selectedStartTime by remember { mutableStateOf<String?>(null) }
    var selectedEndTime by remember { mutableStateOf<String?>(null) }
    var selectedServiceTypes by remember { mutableStateOf<List<String>>(emptyList()) }
    var commentaires by remember { mutableStateOf("") }

    var dateError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }
    var servicesError by remember { mutableStateOf<String?>(null) }
    var bayError by remember { mutableStateOf<String?>(null) }

    // Format de date pour l'affichage
    val displayDateFormat = SimpleDateFormat("EEEE d MMMM", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Récupérer les informations du garage
    val garage = garagesState?.data?.find { it.id == garageId }
    val openingTime = garage?.heureOuverture ?: "08:00"
    val closingTime = garage?.heureFermeture ?: "18:00"

    // Générer les créneaux horaires disponibles
    val availableTimeSlots = remember(openingTime, closingTime) {
        generateTimeSlots(openingTime, closingTime)
    }

    // Filtrer les heures de fin disponibles
    val availableEndTimeSlots = remember(selectedStartTime, availableTimeSlots) {
        if (selectedStartTime == null) {
            emptyList()
        } else {
            val startIndex = availableTimeSlots.indexOf(selectedStartTime)
            if (startIndex != -1) {
                availableTimeSlots.subList(startIndex + 1, availableTimeSlots.size)
            } else {
                emptyList()
            }
        }
    }

    // Calcul du coût total
    val totalCost = remember(selectedServiceTypes, servicesState) {
        when (servicesState) {
            is Resource.Success<*> -> {
                val services = (servicesState as Resource.Success<List<ServiceResponse>>).data.orEmpty()
                selectedServiceTypes.sumOf { serviceType ->
                    services.find { it.type == serviceType }?.coutMoyen ?: 0.0
                }
            }
            else -> 0.0
        }
    }

    val totalDuration = remember(selectedServiceTypes, servicesState) {
        when (servicesState) {
            is Resource.Success<*> -> {
                val services = (servicesState as Resource.Success<List<ServiceResponse>>).data.orEmpty()
                selectedServiceTypes.sumOf { serviceType ->
                    services.find { it.type == serviceType }?.dureeEstimee ?: 0
                }
            }
            else -> 0
        }
    }

    // Calcul de la durée réservée
    val reservedDuration = remember(selectedStartTime, selectedEndTime, availableTimeSlots) {
        calculateDuration(selectedStartTime, selectedEndTime, availableTimeSlots)
    }

    // ✅ Vérifier les créneaux disponibles quand date/heure changent
    LaunchedEffect(selectedDate, selectedStartTime, selectedEndTime) {
        if (selectedDate != null && selectedStartTime != null && selectedEndTime != null) {
            val dateStr = dateFormat.format(selectedDate!!.time)
            Log.d("ReservationScreen", "Checking bays for: $dateStr $selectedStartTime-$selectedEndTime")
            repairBayViewModel.getAvailableRepairBays(
                garageId = garageId,
                date = dateStr,
                heureDebut = selectedStartTime!!,
                heureFin = selectedEndTime!!
            )
        } else {
            repairBayViewModel.clearAvailableBays()
        }
    }

    // ✅ Surveiller les créneaux disponibles
    // Surveiller les créneaux disponibles
    LaunchedEffect(availableBaysState) {
        bayError = when (availableBaysState) {
            is Resource.Success -> {
                val bays = (availableBaysState as Resource.Success).data ?: emptyList() // ✅ Ajoutez ?: emptyList()
                if (bays.isEmpty()) {
                    "❌ Aucun créneau de réparation disponible pour ces horaires. Veuillez choisir une autre période."
                } else {
                    null
                }
            }
            is Resource.Error -> {
                (availableBaysState as Resource.Error).message
            }
            else -> null
        }
    }


    LaunchedEffect(garageId) {
        serviceViewModel.getServicesByGarage(garageId)
        garageViewModel.getGarages()
    }

    LaunchedEffect(createState) {
        when (createState) {
            is Resource.Success -> {
                onReservationSuccess()
                navController.popBackStack()
            }
            else -> {}
        }
    }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Nouvelle réservation",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        garage?.let {
                            Text(
                                it.nom,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Progress Indicator
                AnimatedVisibility(
                    visible = createState !is Resource.Loading,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LinearProgressIndicator(
                        progress = when {
                            selectedDate != null && selectedStartTime != null &&
                                    selectedEndTime != null && selectedServiceTypes.isNotEmpty() -> 1f
                            selectedDate != null && selectedStartTime != null && selectedEndTime != null -> 0.75f
                            selectedDate != null -> 0.5f
                            else -> 0.25f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = AccentGreen,
                        trackColor = LightGrey.copy(alpha = 0.3f)
                    )
                }

                // Hero Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            DeepPurple.copy(alpha = 0.15f),
                                            AccentGreen.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = DeepPurple.copy(alpha = 0.15f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(16.dp),
                                    tint = DeepPurple
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Réservez votre créneau",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Sélectionnez la date, l'heure et les services",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Form Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Step 1: Date
                        ModernFormSection(
                            stepNumber = "1",
                            title = "Date de réservation",
                            isComplete = selectedDate != null && dateError == null
                        ) {
                            Column {
                                AnimatedVisibility(visible = selectedDate != null) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = DeepPurple.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = DeepPurple,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    "Date sélectionnée",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    selectedDate?.let { displayDateFormat.format(it.time) } ?: "",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = DeepPurple,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                ScrollableDateCards(
                                    selectedDate = selectedDate,
                                    onDateSelected = { date ->
                                        selectedDate = date
                                        selectedStartTime = null
                                        selectedEndTime = null
                                        dateError = validateSelectedDate(date)
                                    }
                                )

                                AnimatedVisibility(visible = dateError != null) {
                                    Row(
                                        modifier = Modifier.padding(top = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = AlertRed
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = dateError ?: "",
                                            color = AlertRed,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }

                        // Step 2: Time
                        ModernFormSection(
                            stepNumber = "2",
                            title = "Horaires de réservation",
                            isComplete = selectedStartTime != null && selectedEndTime != null &&
                                    timeError == null && bayError == null
                        ) {
                            Column {
                                AnimatedVisibility(visible = selectedStartTime != null && selectedEndTime != null) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = AccentGreen.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.AccessTime,
                                                    contentDescription = null,
                                                    tint = AccentGreen,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        "Créneau sélectionné",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = TextSecondary
                                                    )
                                                    Text(
                                                        "$selectedStartTime - $selectedEndTime",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = AccentGreen,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = "Durée réservée: $reservedDuration min",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }

                                TimeComboBoxSelector(
                                    selectedStartTime = selectedStartTime,
                                    selectedEndTime = selectedEndTime,
                                    onStartTimeChanged = { startTime ->
                                        selectedStartTime = startTime
                                        if (selectedEndTime != null && !isValidTimeRange(startTime, selectedEndTime, availableTimeSlots)) {
                                            selectedEndTime = null
                                        }
                                        timeError = validateSelectedTimes(startTime, selectedEndTime, totalDuration)
                                    },
                                    onEndTimeChanged = { endTime ->
                                        selectedEndTime = endTime
                                        timeError = validateSelectedTimes(selectedStartTime, endTime, totalDuration)
                                    },
                                    availableTimeSlots = availableTimeSlots,
                                    availableEndTimeSlots = availableEndTimeSlots,
                                    totalDuration = totalDuration,
                                    openingTime = openingTime,
                                    closingTime = closingTime
                                )

                                // ✅ Afficher les créneaux disponibles ou erreur
                                AnimatedVisibility(visible = availableBaysState is Resource.Success) {
                                    val bays = (availableBaysState as? Resource.Success)?.data ?: emptyList()

                                    if (bays.isNotEmpty()) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = StatusGood.copy(alpha = 0.1f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = StatusGood,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    "✓ ${bays.size} créneau(x) de réparation disponible(s)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = StatusGood,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    } else {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 12.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = AlertRed.copy(alpha = 0.1f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Block,
                                                    contentDescription = null,
                                                    tint = AlertRed,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        "Tous les créneaux sont occupés",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = AlertRed,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        "Veuillez choisir un autre horaire",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = AlertRed
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Afficher les erreurs
                                AnimatedVisibility(visible = timeError != null || bayError != null) {
                                    Row(
                                        modifier = Modifier.padding(top = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = AlertRed
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = bayError ?: timeError ?: "",
                                            color = AlertRed,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                // Loading indicator
                                AnimatedVisibility(visible = availableBaysState is Resource.Loading) {
                                    Row(
                                        modifier = Modifier.padding(top = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = DeepPurple,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Vérification des créneaux disponibles...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Step 3: Services
                        ModernFormSection(
                            stepNumber = "3",
                            title = "Services souhaités",
                            isComplete = selectedServiceTypes.isNotEmpty()
                        ) {
                            when (servicesState) {
                                is Resource.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                color = DeepPurple,
                                                strokeWidth = 4.dp
                                            )
                                            Text(
                                                "Chargement des services...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                                is Resource.Success -> {
                                    val availableServices = (servicesState as Resource.Success<List<ServiceResponse>>).data.orEmpty()
                                    if (availableServices.isEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = LightGrey.copy(alpha = 0.3f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    Icons.Default.Build,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp),
                                                    tint = TextSecondary.copy(alpha = 0.5f)
                                                )
                                                Spacer(Modifier.height(12.dp))
                                                Text(
                                                    "Aucun service disponible",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = TextSecondary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    } else {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(availableServices) { service ->
                                                UltraModernServiceChip(
                                                    service = service,
                                                    selected = selectedServiceTypes.contains(service.type),
                                                    onSelectionChanged = { selected ->
                                                        selectedServiceTypes = if (selected) {
                                                            if (selectedServiceTypes.size < 5) {
                                                                selectedServiceTypes + service.type
                                                            } else {
                                                                selectedServiceTypes
                                                            }
                                                        } else {
                                                            selectedServiceTypes - service.type
                                                        }
                                                        servicesError = validateServices(selectedServiceTypes)
                                                    }
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "${selectedServiceTypes.size}/5 services",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = TextSecondary
                                            )
                                            if (selectedServiceTypes.size >= 5) {
                                                Text(
                                                    "Maximum atteint",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AccentYellow
                                                )
                                            }
                                        }
                                    }
                                }
                                is Resource.Error -> {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = AlertRed.copy(alpha = 0.1f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.Error,
                                                "Erreur",
                                                tint = AlertRed,
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                "Erreur de chargement",
                                                color = AlertRed,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                else -> {}
                            }

                            AnimatedVisibility(visible = servicesError != null) {
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = AlertRed
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = servicesError ?: "",
                                        color = AlertRed,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        // Step 4: Comments
                        ModernFormSection(
                            stepNumber = "4",
                            title = "Commentaires (optionnel)",
                            isComplete = false,
                            isOptional = true
                        ) {
                            OutlinedTextField(
                                value = commentaires,
                                onValueChange = { commentaires = it },
                                placeholder = { Text("Des détails supplémentaires...", color = InputPlaceholder) },
                                leadingIcon = {
                                    Icon(Icons.Default.Comment, "Commentaire", tint = DeepPurple)
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = InputBackground,
                                    focusedContainerColor = Color.White,
                                    unfocusedBorderColor = InputBorder,
                                    focusedBorderColor = DeepPurple,
                                    unfocusedTextColor = InputText,
                                    focusedTextColor = InputText,
                                    cursorColor = DeepPurple
                                ),
                                singleLine = false,
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Summary Card
                AnimatedVisibility(
                    visible = selectedServiceTypes.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AccentGreen.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .padding(10.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Récapitulatif",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DeepPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = StatusGood.copy(alpha = 0.08f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Services sélectionnés",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        selectedServiceTypes.joinToString(" • "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = AccentYellow.copy(alpha = 0.1f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = AccentYellow,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "$totalDuration min",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = AccentYellow,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Durée totale",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = AccentGreen.copy(alpha = 0.1f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.AttachMoney,
                                            contentDescription = null,
                                            tint = AccentGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "${String.format("%.2f", totalCost)} DH",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = AccentGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Coût estimé",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ✅ Submit Button avec validation stricte
                val formValid = selectedDate != null &&
                        selectedStartTime != null &&
                        selectedEndTime != null &&
                        selectedServiceTypes.isNotEmpty() &&
                        dateError == null &&
                        timeError == null &&
                        bayError == null &&
                        availableBaysState is Resource.Success &&
                        ((availableBaysState as? Resource.Success)?.data?.isNotEmpty() == true)

                Button(
                    onClick = {
                        dateError = validateSelectedDate(selectedDate)
                        timeError = validateSelectedTimes(selectedStartTime, selectedEndTime, totalDuration)
                        servicesError = validateServices(selectedServiceTypes)

                        if (dateError == null && timeError == null && servicesError == null && bayError == null) {
                            reservationViewModel.createReservation(
                                garageId = garageId,
                                date = selectedDate?.let { dateFormat.format(it.time) } ?: "",
                                heureDebut = selectedStartTime ?: "",
                                heureFin = selectedEndTime ?: "",
                                services = selectedServiceTypes,
                                commentaires = commentaires
                            )
                        }
                    },
                    enabled = formValid && createState !is Resource.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (formValid) DeepPurple else MediumGrey,
                        contentColor = Color.White,
                        disabledContainerColor = MediumGrey.copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (formValid) 8.dp else 2.dp
                    )
                ) {
                    if (createState is Resource.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Création en cours...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, "Réserver", modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (!formValid && bayError != null) {
                                "Créneaux indisponibles"
                            } else {
                                "Confirmer la réservation"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ✅ Message d'aide
                if (!formValid && bayError != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AccentYellow.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = AccentYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Astuce",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AccentYellow,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Essayez de choisir une autre date ou un autre horaire. Ce garage dispose de ${garage?.numberOfBays ?: 1} créneau(x) de réparation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Status Messages
                AnimatedVisibility(
                    visible = createState is Resource.Error,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                "Erreur",
                                tint = AlertRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = (createState as? Resource.Error)?.message ?: "Erreur lors de la réservation",
                                color = AlertRed,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = createState is Resource.Success,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusGood.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                "Succès",
                                tint = StatusGood,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Réservation créée avec succès!",
                                color = StatusGood,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}


// Composant pour les comboboxes de sélection d'heure
@Composable
fun TimeComboBoxSelector(
    selectedStartTime: String?,
    selectedEndTime: String?,
    onStartTimeChanged: (String?) -> Unit,
    onEndTimeChanged: (String?) -> Unit,
    availableTimeSlots: List<String>,
    availableEndTimeSlots: List<String>,
    totalDuration: Int,
    openingTime: String,
    closingTime: String
) {
    var startExpanded by remember { mutableStateOf(false) }
    var endExpanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sélectionnez votre créneau",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Horaires: $openingTime - $closingTime",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Combobox Heure de début
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Heure de début",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                StyledExposedDropdownMenuBox(
                    expanded = startExpanded,
                    onExpandedChange = { startExpanded = !startExpanded },
                    selectedValue = selectedStartTime ?: "Sélectionnez",
                    label = "Début",
                    items = availableTimeSlots,
                    onItemSelected = onStartTimeChanged,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Combobox Heure de fin
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Heure de fin",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                StyledExposedDropdownMenuBox(
                    expanded = endExpanded,
                    onExpandedChange = { endExpanded = !endExpanded },
                    selectedValue = selectedEndTime ?: "Sélectionnez",
                    label = "Fin",
                    items = availableEndTimeSlots,
                    onItemSelected = onEndTimeChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedStartTime != null
                )
            }
        }

        if (totalDuration > 0) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AccentYellow.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = AccentYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Durée des services: $totalDuration min",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        if (selectedStartTime != null && selectedEndTime != null) {
                            val duration = calculateDuration(selectedStartTime, selectedEndTime, availableTimeSlots)
                            Text(
                                text = "Durée réservée: $duration min",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (duration >= totalDuration) StatusGood else AlertRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Composant Combobox stylé
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedValue: String,
    label: String,
    items: List<String>,
    onItemSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            placeholder = {
                Text("Choisir $label", color = InputPlaceholder)
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = if (enabled) InputBackground else InputBackground.copy(alpha = 0.5f),
                focusedContainerColor = Color.White,
                unfocusedBorderColor = if (enabled) InputBorder else InputBorder.copy(alpha = 0.5f),
                focusedBorderColor = DeepPurple,
                unfocusedTextColor = if (enabled) InputText else InputText.copy(alpha = 0.5f),
                focusedTextColor = InputText,
                cursorColor = DeepPurple,
                unfocusedTrailingIconColor = if (enabled) DeepPurple else DeepPurple.copy(alpha = 0.5f),
                focusedTrailingIconColor = DeepPurple
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            enabled = enabled
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.exposedDropdownSize()
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    },
                    onClick = {
                        onItemSelected(item)
                        onExpandedChange(false)
                    },
                    modifier = Modifier.background(
                        color = if (selectedValue == item) DeepPurple.copy(alpha = 0.1f) else Color.Transparent
                    )
                )
            }
        }
    }
}

// Fonction pour générer les créneaux horaires
private fun generateTimeSlots(openingTime: String, closingTime: String): List<String> {
    val slots = mutableListOf<String>()

    try {
        val openParts = openingTime.split(":").map { it.toInt() }
        val closeParts = closingTime.split(":").map { it.toInt() }

        var currentHour = openParts[0]
        val endHour = closeParts[0]

        while (currentHour <= endHour) {
            slots.add(String.format("%02d:00", currentHour))
            currentHour++
        }
    } catch (e: Exception) {
        return listOf("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00")
    }

    return slots
}

// Fonction pour calculer la durée en minutes
private fun calculateDuration(startTime: String?, endTime: String?, timeSlots: List<String>): Int {
    if (startTime == null || endTime == null) return 0

    val startIndex = timeSlots.indexOf(startTime)
    val endIndex = timeSlots.indexOf(endTime)

    if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) return 0

    return (endIndex - startIndex) * 60
}

// Fonction pour valider la plage horaire
private fun isValidTimeRange(startTime: String?, endTime: String?, timeSlots: List<String>): Boolean {
    if (startTime == null || endTime == null) return false

    val startIndex = timeSlots.indexOf(startTime)
    val endIndex = timeSlots.indexOf(endTime)

    return startIndex != -1 && endIndex != -1 && endIndex > startIndex
}

// Fonctions de validation
private fun validateSelectedDate(date: Calendar?): String? {
    return when {
        date == null -> "Veuillez sélectionner une date"
        else -> null
    }
}

private fun validateSelectedTimes(startTime: String?, endTime: String?, totalDuration: Int): String? {
    return when {
        startTime == null -> "Veuillez sélectionner une heure de début"
        endTime == null -> "Veuillez sélectionner une heure de fin"
        startTime >= endTime -> "L'heure de fin doit être après l'heure de début"
        else -> null
    }
}

private fun validateServices(services: List<String>): String? {
    return if (services.isEmpty()) "Au moins un service est requis" else null
}

// Les autres composants (ScrollableDateCards, DateCard, ModernFormSection, UltraModernServiceChip)
// restent identiques à votre version précédente

@Composable
fun ScrollableDateCards(
    selectedDate: Calendar?,
    onDateSelected: (Calendar) -> Unit
) {
    val daysToShow = 30
    val today = Calendar.getInstance()

    Column {
        Text(
            text = "Prochains jours disponibles",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(daysToShow) { index ->
                val date = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, index)
                }
                DateCard(
                    date = date,
                    isSelected = selectedDate?.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR) &&
                            selectedDate.get(Calendar.YEAR) == date.get(Calendar.YEAR),
                    isToday = index == 0,
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

@Composable
fun DateCard(
    date: Calendar,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val shortDateFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayFormat = SimpleDateFormat("d", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Card(
        modifier = Modifier
            .width(80.dp)
            .height(100.dp)
            .scale(scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> DeepPurple
                isToday -> AccentGreen.copy(alpha = 0.15f)
                else -> Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = shortDateFormat.format(date.time).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isSelected -> Color.White.copy(alpha = 0.8f)
                    isToday -> AccentGreen
                    else -> TextSecondary
                },
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = dayFormat.format(date.time),
                style = MaterialTheme.typography.headlineSmall,
                color = when {
                    isSelected -> Color.White
                    isToday -> AccentGreen
                    else -> TextPrimary
                },
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = monthFormat.format(date.time),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isSelected -> Color.White.copy(alpha = 0.8f)
                    isToday -> AccentGreen
                    else -> TextSecondary
                },
                fontWeight = FontWeight.Medium
            )

            if (isToday && !isSelected) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Auj",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ModernFormSection(
    stepNumber: String,
    title: String,
    isComplete: Boolean,
    isOptional: Boolean = false,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isComplete) StatusGood.copy(alpha = 0.15f) else DeepPurple.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isComplete) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = StatusGood,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            stepNumber,
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (isOptional) {
                    Text(
                        "Facultatif",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            if (isComplete) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StatusGood.copy(alpha = 0.15f)
                ) {
                    Text(
                        "✓ Complété",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusGood,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
fun UltraModernServiceChip(
    service: ServiceResponse,
    selected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Card(
        modifier = Modifier
            .width(180.dp)
            .scale(scale)
            .clickable { onSelectionChanged(!selected) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) DeepPurple else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 12.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) Color.White.copy(alpha = 0.2f) else DeepPurple.copy(alpha = 0.1f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = if (selected) Color.White else DeepPurple,
                        modifier = Modifier
                            .size(36.dp)
                            .padding(8.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (selected) Color.White else DeepPurple.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (selected) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                        contentDescription = if (selected) "Sélectionné" else "Sélectionner",
                        tint = if (selected) DeepPurple else DeepPurple,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(4.dp)
                    )
                }
            }

            Text(
                text = service.type.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) Color.White else TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Divider(
                color = if (selected) Color.White.copy(alpha = 0.3f) else LightGrey
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Durée",
                    modifier = Modifier.size(18.dp),
                    tint = if (selected) Color.White.copy(alpha = 0.9f) else TextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "${service.dureeEstimee} min",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White else TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Durée",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White.copy(alpha = 0.7f) else TextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.AttachMoney,
                    contentDescription = "Coût",
                    modifier = Modifier.size(18.dp),
                    tint = if (selected) Color.White.copy(alpha = 0.9f) else TextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "${String.format("%.0f", service.coutMoyen)} DH",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White else AccentGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Prix moyen",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White.copy(alpha = 0.7f) else TextSecondary
                    )
                }
            }
        }
    }
}
