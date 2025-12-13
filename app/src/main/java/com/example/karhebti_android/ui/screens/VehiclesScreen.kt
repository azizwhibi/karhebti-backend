package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.api.CarResponse
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.ui.theme.*
import com.example.karhebti_android.viewmodel.CarViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import android.net.Uri
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.karhebti_android.viewmodel.CarImageViewModel
import com.example.karhebti_android.ui.components.ImageUploadField
import com.example.karhebti_android.ui.components.UploadProgressIndicator
import com.example.karhebti_android.ui.components.ImageValidationError
import com.example.karhebti_android.data.repository.TranslationManager
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.karhebti_android.ui.components.BottomNavigationBar

// Backend-Integrated VehiclesScreen with comprehensive image upload
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesScreen(
    onBackClick: () -> Unit = {},
    onVehicleClick: (String) -> Unit = {},
    navController: NavController? = null
) {
    val context = LocalContext.current
    val carViewModel: CarViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    // Car image upload viewmodel
    val carImageViewModel: CarImageViewModel = viewModel()

    // Observe cars state from backend
    val carsState by carViewModel.carsState.observeAsState()
    val createCarState by carViewModel.createCarState.observeAsState()
    val uploadState by carImageViewModel.uploadState.collectAsState()
    val isUploading by carImageViewModel.isUploading.collectAsState()

    // UI State
    var showAddDialog by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }

    // Pending image Uri selected in the add dialog (uploaded after creation)
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

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
    var vehiclesTitle by remember { mutableStateOf("Mes véhicules") }
    var vehiclesCount by remember { mutableStateOf("") }
    var loadingText by remember { mutableStateOf("Chargement des véhicules...") }
    var addVehicleText by remember { mutableStateOf("Ajouter véhicule") }
    var backText by remember { mutableStateOf("Retour") }

    // Load cars on screen start - ALWAYS refresh when screen becomes visible
    LaunchedEffect(Unit) {
        carViewModel.getMyCars()
    }

    // Auto-refresh whenever the screen is recomposed (navigating back)
    DisposableEffect(Unit) {
        onDispose {
            // No cleanup needed
        }
    }

    // Handle create car result
    LaunchedEffect(createCarState) {
        when (createCarState) {
            is Resource.Success -> {
                // If an image was selected before creating the car, upload it now
                val createdCar = (createCarState as Resource.Success).data
                if (createdCar != null && pendingImageUri != null) {
                    // Upload image for the newly created car
                    carImageViewModel.uploadCarImage(createdCar.id, pendingImageUri!!)
                    pendingImageUri = null
                }

                showAddDialog = false
                // Refresh the list after creating a car
                carViewModel.getMyCars()
            }
            is Resource.Error -> {
                // Error shown in dialog
            }
            else -> {}
        }
    }

    // Refresh car list after image upload
    LaunchedEffect(uploadState) {
        if (uploadState is Resource.Success) {
            // Image uploaded successfully, refresh the car list
            carViewModel.getMyCars()
        }
    }

    // Update translations on language change or carsState change
    LaunchedEffect(currentLanguage, carsState) {
        coroutineScope.launch {
            vehiclesTitle = translationManager.translate("vehicles_title", "Mes véhicules", currentLanguage)
            vehiclesCount = if (carsState is Resource.Success) {
                val n = (carsState as Resource.Success).data?.size ?: 0
                translationManager.translate("vehicles_count", "$n véhicule(s)", currentLanguage)
            } else ""
            loadingText = translationManager.translate("vehicles_loading", "Chargement des véhicules...", currentLanguage)
            addVehicleText = translationManager.translate("add_vehicle", "Ajouter véhicule", currentLanguage)
            backText = translationManager.translate("back", "Retour", currentLanguage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(vehiclesTitle)
                        if (carsState is Resource.Success) {
                            Text(
                                text = vehiclesCount,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
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
        bottomBar = {
            if (navController != null) {
                BottomNavigationBar(navController = navController)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, addVehicleText)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            when (val state = carsState) {
                is Resource.Loading -> {
                    // Loading indicator
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
                                loadingText,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is Resource.Success -> {
                    val cars = state.data ?: emptyList()

                    if (refreshing) {
                        refreshing = false
                    }

                    if (cars.isEmpty()) {
                        // Empty state
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
                                    imageVector = Icons.Default.DriveEta,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    "Aucun véhicule",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    "Ajoutez votre premier véhicule pour commencer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Ajouter un véhicule")
                                }
                            }
                        }
                    } else {
                        // Cars list
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(cars, key = { it.id }) { car ->
                                VehicleCardBackendIntegrated(
                                    car = car,
                                    onClick = { onVehicleClick(car.id) },
                                    onDelete = { carViewModel.deleteCar(it) },
                                    carImageViewModel = carImageViewModel
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    // Error state
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
                                onClick = { carViewModel.getMyCars() },
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
                else -> {
                    // Initial state
                }
            }
        }
    }

    // Add vehicle dialog
    if (showAddDialog) {
        AddVehicleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { marque, modele, annee, immatriculation, typeCarburant, kilometrage ->
                carViewModel.createCar(marque, modele, annee, immatriculation, typeCarburant, kilometrage)
            },
            createState = createCarState,
            uploadState = uploadState,
            isUploading = isUploading,
            onImagePicked = { uri -> pendingImageUri = uri }
        )
    }
}

@Composable
fun VehicleCardBackendIntegrated(
    car: CarResponse,
    onClick: () -> Unit,
    onDelete: (String) -> Unit,
    carImageViewModel: CarImageViewModel? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showImageUploadDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image section
            if (!car.imageUrl.isNullOrEmpty()) {
                val fullImageUrl = com.example.karhebti_android.util.ImageUrlHelper.getFullImageUrl(car.imageUrl)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable { showImageUploadDialog = true }
                ) {
                    if (!fullImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(fullImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "${car.marque} ${car.modele}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            onError = { _ ->
                                android.util.Log.e("VehicleCard", "Failed to load image from URL: $fullImageUrl")
                            }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Header Row with leading icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading car icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${car.marque} ${car.modele}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Année ${car.annee}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Menu for actions (delete, upload image)
                Row {
                    IconButton(onClick = { showImageUploadDialog = true }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Upload image")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Supprimer") },
                            onClick = { showMenu = false; onDelete(car.id) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Info Row with chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            car.immatriculation,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            car.typeCarburant,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.LocalGasStation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                )
            }
        }
    }

    // Image upload dialog
    if (showImageUploadDialog && carImageViewModel != null) {
        ImageUploadDialog(
            carId = car.id,
            carName = "${car.marque} ${car.modele}",
            carImageViewModel = carImageViewModel,
            onDismiss = { showImageUploadDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageUploadDialog(
    carId: String,
    carName: String,
    carImageViewModel: CarImageViewModel,
    onDismiss: () -> Unit
) {
    val uploadState by carImageViewModel.uploadState.collectAsState()
    val validationError by carImageViewModel.validationState.collectAsState()
    val isUploading by carImageViewModel.isUploading.collectAsState()
    val selectedImageUri = remember { mutableStateOf<Uri?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une photo - $carName") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Validation error display
                if (validationError != null) {
                    ImageValidationError(
                        error = validationError!!,
                        onDismiss = { selectedImageUri.value = null }
                    )
                }

                // Image upload field
                ImageUploadField(
                    selectedImageUri = selectedImageUri,
                    onImageSelected = { uri ->
                        selectedImageUri.value = uri
                    },
                    isLoading = isUploading,
                    error = validationError,
                    label = "Sélectionner une photo du véhicule"
                )

                // Upload status
                when (uploadState) {
                    is Resource.Loading -> {
                        UploadProgressIndicator(
                            progress = 50,
                            isUploading = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    is Resource.Success -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Image téléchargée avec succès !",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    (uploadState as Resource.Error<CarResponse>).message ?: "Erreur lors du téléchargement",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    else -> {}
                }

                /*Text(
                    "Images acceptées: JPG, PNG, WebP • Taille max: 5MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )*/
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedImageUri.value != null) {
                        carImageViewModel.uploadCarImage(carId, selectedImageUri.value!!)
                    }
                },
                enabled = selectedImageUri.value != null && !isUploading && validationError == null,
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Télécharger")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int, String, String, Int?) -> Unit,
    createState: Resource<CarResponse>?,
    uploadState: Resource<CarResponse>? = null,
    isUploading: Boolean = false,
    onImagePicked: ((Uri?) -> Unit)? = null
) {
    var marque by remember { mutableStateOf("") }
    var modele by remember { mutableStateOf("") }
    var annee by remember { mutableStateOf("") }
    var immatriculation by remember { mutableStateOf("") }
    var kilometrage by remember { mutableStateOf("") }
    var typeCarburant by remember { mutableStateOf("Essence") }
    var expanded by remember { mutableStateOf(false) }
    val selectedImageUri = remember { mutableStateOf<Uri?>(null) }

    val carburants = listOf("Essence", "Diesel", "Électrique", "Hybride", "GPL")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un véhicule") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = marque,
                    onValueChange = { marque = it },
                    label = { Text("Marque") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = modele,
                    onValueChange = { modele = it },
                    label = { Text("Modèle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = annee,
                    onValueChange = { if (it.length <= 4) annee = it.filter { c -> c.isDigit() } },
                    label = { Text("Année") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = immatriculation,
                    onValueChange = { immatriculation = it },
                    label = { Text("Immatriculation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = kilometrage,
                    onValueChange = { kilometrage = it.filter { c -> c.isDigit() } },
                    label = { Text("Kilométrage") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ex: 50000") }
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = typeCarburant,
                        onValueChange = {},
                        label = { Text("Type de carburant") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        carburants.forEach { c ->
                            DropdownMenuItem(text = { Text(c) }, onClick = {
                                typeCarburant = c
                                expanded = false
                            })
                        }
                    }
                }

                // Image picker UI with validation feedback
                if (onImagePicked != null) {
                    ImageUploadField(
                        selectedImageUri = selectedImageUri,
                        onImageSelected = { uri ->
                            selectedImageUri.value = uri
                            onImagePicked(uri)
                        },
                        isLoading = isUploading,
                        label = "Photo du véhicule (optionnelle)"
                    )
                }

                // Show create state (loading/error)
                when (createState) {
                    is Resource.Loading -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            "Création du véhicule...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is Resource.Error -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = createState.message ?: "Erreur lors de la création",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    else -> {}
                }

                // Show upload state if creating
                if (createState is Resource.Loading || isUploading) {
                    UploadProgressIndicator(
                        progress = if (isUploading) 50 else 30,
                        isUploading = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val yearInt = annee.toIntOrNull() ?: 0
                    val kmInt = kilometrage.toIntOrNull()
                    onAdd(marque, modele, yearInt, immatriculation, typeCarburant, kmInt)
                },
                enabled = marque.isNotEmpty() && modele.isNotEmpty() && annee.isNotEmpty() && immatriculation.isNotEmpty() && createState !is Resource.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun VehiclesScreenPreview() {
    KarhebtiandroidTheme {
        VehiclesScreen()
    }
}
