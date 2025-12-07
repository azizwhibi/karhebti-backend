package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.api.GarageResponse
import com.example.karhebti_android.data.api.OsmLocationSuggestion
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.ui.theme.DeepPurple
import com.example.karhebti_android.ui.theme.AccentGreen
import com.example.karhebti_android.viewmodel.GarageViewModel
import com.example.karhebti_android.viewmodel.ServiceViewModel
import com.example.karhebti_android.viewmodel.OsmViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

data class GarageServiceForm(
    val type: String,
    var coutMoyen: String = "",
    var dureeEstimee: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGarageScreen(
    onBackClick: () -> Unit = {},
    onGarageAdded: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val garageViewModel: GarageViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val serviceViewModel: ServiceViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val osmViewModel: OsmViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    val createGarageState by garageViewModel.createGarageState.observeAsState()
    val searchResults by osmViewModel.searchResults.observeAsState()
    val selectedLocation by osmViewModel.selectedLocation.observeAsState()
    // Observer le résultat du reverse geocoding (ajouté)
    val reverseGeocodeResult by osmViewModel.reverseGeocodeResult.observeAsState()

    var nom by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var noteUtilisateur by remember { mutableStateOf("0.0") }
    var heureOuverture by remember { mutableStateOf("08:00") }
    var heureFermeture by remember { mutableStateOf("18:00") }

    // ✅ NOUVEAU: Nombre de créneaux de réparation
    var numberOfBays by remember { mutableStateOf(1) }

    // Coordonnées GPS
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    // Map dialog state
    var showMapDialog by remember { mutableStateOf(false) }
    var showAddressSuggestions by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val allServiceTypes = listOf(
        "vidange", "contrôle technique", "réparation pneu", "changement pneu",
        "freinage", "batterie", "climatisation", "échappement", "révision complète",
        "diagnostic électronique", "carrosserie", "peinture", "pare-brise",
        "suspension", "embrayage", "transmission", "injection", "refroidissement",
        "démarrage", "lavage auto", "équilibrage roues", "parallélisme",
        "système électrique", "filtre à air", "filtre à huile", "plaquettes de frein"
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedServices by remember { mutableStateOf(listOf<GarageServiceForm>()) }

    // ✅ Variable pour suivre si on a déjà traité ce garage
    var processedGarageId by remember { mutableStateOf<String?>(null) }

    val canSubmit = nom.isNotBlank() &&
            adresse.isNotBlank() &&
            telephone.isNotBlank() &&
            (noteUtilisateur.toDoubleOrNull() ?: -1.0) in 0.0..5.0 &&
            heureOuverture.matches(Regex("^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]\$")) &&
            heureFermeture.matches(Regex("^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]\$")) &&
            heureOuverture < heureFermeture &&
            selectedServices.isNotEmpty() &&
            selectedServices.all {
                it.coutMoyen.toDoubleOrNull() != null && it.dureeEstimee.toIntOrNull() != null
            } &&
            latitude != null && longitude != null &&
            numberOfBays in 1..10 // ✅ Validation du nombre de créneaux

    // Handle selected location from OSM search
    LaunchedEffect(selectedLocation) {
        selectedLocation?.let { location ->
            adresse = location.displayName
            latitude = location.latitude
            longitude = location.longitude
            showAddressSuggestions = false
        }
    }

    // Mettre à jour 'adresse' quand le reverse geocoding renvoie un résultat
    LaunchedEffect(reverseGeocodeResult) {
        if (reverseGeocodeResult is Resource.Success) {
            val data = (reverseGeocodeResult as Resource.Success).data
            if (data != null) {
                adresse = data.displayName
                showAddressSuggestions = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajouter un garage") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 28.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Nom du garage
                    OutlinedTextField(
                        value = nom,
                        onValueChange = { nom = it },
                        label = { Text("Nom du garage") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    // Adresse avec recherche et carte
                    Column {
                        OutlinedTextField(
                            value = adresse,
                            onValueChange = { query ->
                                adresse = query
                                showAddressSuggestions = query.length >= 3

                                searchJob?.cancel()

                                if (query.length >= 3) {
                                    searchJob = coroutineScope.launch {
                                        delay(500)
                                        osmViewModel.searchAddress(query)
                                    }
                                } else {
                                    osmViewModel.clearSearch()
                                }
                            },
                            label = { Text("Adresse") },
                            placeholder = { Text("Rechercher ou choisir sur la carte...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "Localisation",
                                    tint = DeepPurple
                                )
                            },
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = { showMapDialog = true }) {
                                        Icon(
                                            Icons.Default.Map,
                                            contentDescription = "Choisir sur la carte",
                                            tint = DeepPurple
                                        )
                                    }
                                    if (adresse.isNotEmpty()) {
                                        IconButton(onClick = {
                                            adresse = ""
                                            latitude = null
                                            longitude = null
                                            showAddressSuggestions = false
                                            osmViewModel.clearSearch()
                                            osmViewModel.clearSelection()
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Address suggestions dropdown
                        if (showAddressSuggestions && searchResults is Resource.Success) {
                            val suggestions = (searchResults as Resource.Success<List<OsmLocationSuggestion>>).data

                            if (!suggestions.isNullOrEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 250.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        suggestions.forEach { suggestion ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        osmViewModel.selectLocation(suggestion)
                                                    },
                                                color = Color.Transparent
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.LocationOn,
                                                        contentDescription = null,
                                                        tint = DeepPurple.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(Modifier.width(12.dp))
                                                    Text(
                                                        text = suggestion.displayName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.Black
                                                    )
                                                }
                                            }
                                            if (suggestion != suggestions.last()) {
                                                HorizontalDivider()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (searchResults is Resource.Loading) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                color = DeepPurple
                            )
                        }

                        if (latitude != null && longitude != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = DeepPurple.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = DeepPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Position sélectionnée: ${String.format(Locale.getDefault(), "%.4f", latitude ?: 0.0)}, ${String.format(Locale.getDefault(), "%.4f", longitude ?: 0.0)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DeepPurple,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Téléphone
                    OutlinedTextField(
                        value = telephone,
                        onValueChange = { telephone = it },
                        label = { Text("Téléphone") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(16.dp))

                    // Heures d'ouverture/fermeture
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = heureOuverture,
                            onValueChange = { heureOuverture = it },
                            label = { Text("Ouverture") },
                            placeholder = { Text("08:00") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                        OutlinedTextField(
                            value = heureFermeture,
                            onValueChange = { heureFermeture = it },
                            label = { Text("Fermeture") },
                            placeholder = { Text("18:00") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    // ✅ NOUVEAU: Nombre de créneaux de réparation
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = AccentGreen.copy(alpha = 0.08f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Garage,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Créneaux de réparation",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGreen
                                    )
                                    Text(
                                        "Nombre d'emplacements simultanés",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Decrease button
                                IconButton(
                                    onClick = { if (numberOfBays > 1) numberOfBays-- },
                                    enabled = numberOfBays > 1
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = "Diminuer",
                                        tint = if (numberOfBays > 1) DeepPurple else Color.Gray
                                    )
                                }

                                // Number display
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = DeepPurple.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = numberOfBays.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepPurple,
                                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
                                    )
                                }

                                // Increase button
                                IconButton(
                                    onClick = { if (numberOfBays < 10) numberOfBays++ },
                                    enabled = numberOfBays < 10
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Augmenter",
                                        tint = if (numberOfBays < 10) DeepPurple else Color.Gray
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "Maximum: 10 créneaux",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Services selection
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = if (selectedServices.isEmpty()) ""
                            else "${selectedServices.size} service(s) sélectionné(s)",
                            onValueChange = {},
                            label = { Text("Services proposés") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            allServiceTypes.filter { type ->
                                selectedServices.none { it.type == type }
                            }.forEach { serviceType ->
                                DropdownMenuItem(
                                    text = { Text(serviceType, fontWeight = FontWeight.Normal) },
                                    onClick = {
                                        selectedServices = selectedServices + GarageServiceForm(type = serviceType)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // Services list
                    selectedServices.forEach { serviceForm ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DeepPurple.copy(alpha = 0.03f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(serviceForm.type, color = DeepPurple, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = {
                                        selectedServices = selectedServices.filter { it.type != serviceForm.type }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer",
                                            tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = serviceForm.coutMoyen,
                                    onValueChange = { value ->
                                        selectedServices = selectedServices.map {
                                            if (it.type == serviceForm.type)
                                                it.copy(coutMoyen = value.filter { c -> c.isDigit() || c == '.' })
                                            else it
                                        }
                                    },
                                    label = { Text("Coût moyen (DH)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = serviceForm.dureeEstimee,
                                    onValueChange = { value ->
                                        selectedServices = selectedServices.map {
                                            if (it.type == serviceForm.type)
                                                it.copy(dureeEstimee = value.filter { c -> c.isDigit() })
                                            else it
                                        }
                                    },
                                    label = { Text("Durée estimée (min)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    // Note utilisateur
                    OutlinedTextField(
                        value = noteUtilisateur,
                        onValueChange = { noteUtilisateur = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Note initiale (0 à 5)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.height(24.dp))

                    // Error message
                    if (createGarageState is Resource.Error) {
                        Text(
                            text = (createGarageState as? Resource.Error)?.message ?: "Erreur",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    // Submit button
                    Button(
                        onClick = {
                            val note = noteUtilisateur.toDoubleOrNull() ?: 0.0
                            garageViewModel.createGarage(
                                nom = nom,
                                adresse = adresse,
                                telephone = telephone,
                                noteUtilisateur = note,
                                heureOuverture = heureOuverture,
                                heureFermeture = heureFermeture,
                                latitude = latitude,
                                longitude = longitude,
                                numberOfBays = numberOfBays // ✅ Passer le paramètre
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPurple),
                        enabled = (createGarageState !is Resource.Loading) && canSubmit
                    ) {
                        if (createGarageState is Resource.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Créer le garage", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Annuler")
                    }
                }
            }
        }
    }

    // Map Dialog
    if (showMapDialog) {
        MapPickerDialog(
            initialLat = latitude ?: 36.79952,
            initialLon = longitude ?: 10.17849,
            onLocationSelected = { lat, lon ->
                latitude = lat
                longitude = lon
                showMapDialog = false

                // Demander le reverse geocoding ; la LaunchedEffect ci‑dessus mettra à jour 'adresse'
                osmViewModel.reverseGeocode(lat, lon)
            },
            onDismiss = { showMapDialog = false }
        )
    }

    // Handle successful garage creation: créer les services ensuite de façon séquentielle
    LaunchedEffect(createGarageState) {
        if (createGarageState is Resource.Success) {
            val garageId = (createGarageState as Resource.Success<GarageResponse>).data?.id

            // ✅ Vérifier si on a déjà traité ce garage pour éviter les doublons
            if (garageId != null && garageId != processedGarageId) {
                processedGarageId = garageId

                if (selectedServices.isNotEmpty()) {
                    android.util.Log.d("AddGarageScreen", "Garage créé avec succès, ID: $garageId. Création de ${selectedServices.size} services...")

                    // Petit délai pour s'assurer que le garage est bien enregistré dans la BD
                    delay(300)

                    // Créer chaque service de façon séquentielle et attendre la réponse
                    var createdCount = 0
                    var failed = false
                    for (serviceForm in selectedServices) {
                        val cout = serviceForm.coutMoyen.toDoubleOrNull()
                        val duree = serviceForm.dureeEstimee.toIntOrNull()
                        if (cout != null && duree != null) {
                            android.util.Log.d("AddGarageScreen", "Création service: ${serviceForm.type}, cout=$cout, duree=$duree, garageId=$garageId")
                            val res = serviceViewModel.createServiceSuspend(
                                type = serviceForm.type,
                                coutMoyen = cout,
                                dureeEstimee = duree,
                                garageId = garageId
                            )
                            when (res) {
                                is Resource.Success -> {
                                    createdCount++
                                    android.util.Log.d("AddGarageScreen", "Service créé avec succès: id=${res.data?.id} type=${serviceForm.type}")
                                }
                                is Resource.Error -> {
                                    failed = true
                                    android.util.Log.e("AddGarageScreen", "Erreur création service (${serviceForm.type}): ${res.message}")
                                    android.widget.Toast.makeText(context, "Erreur service '${serviceForm.type}': ${res.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                                else -> {
                                    failed = true
                                    android.util.Log.e("AddGarageScreen", "Résultat inattendu pour la création du service ${serviceForm.type}")
                                }
                            }
                        } else {
                            android.util.Log.w("AddGarageScreen", "Champs invalides pour le service: ${serviceForm.type}, cout=$cout, duree=$duree")
                        }
                    }

                    // Afficher le résultat final
                    if (createdCount > 0) {
                        android.widget.Toast.makeText(context, "✓ Garage créé avec $createdCount service(s)", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    if (failed) {
                        android.widget.Toast.makeText(context, "⚠ Certains services n'ont pas pu être créés", android.widget.Toast.LENGTH_LONG).show()
                    }
                } else {
                    android.util.Log.w("AddGarageScreen", "Garage créé sans services")
                    android.widget.Toast.makeText(context, "✓ Garage créé (sans services)", android.widget.Toast.LENGTH_SHORT).show()
                }

                // ✅ Naviguer immédiatement vers l'écran précédent
                onGarageAdded()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerDialog(
    initialLat: Double,
    initialLon: Double,
    onLocationSelected: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLat by remember { mutableStateOf(initialLat) }
    var selectedLon by remember { mutableStateOf(initialLon) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Choisir la position du garage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Appuyez sur la carte pour placer le marqueur",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                AndroidView(
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName

                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                            controller.setCenter(GeoPoint(initialLat, initialLon))

                            val newMarker = Marker(this).apply {
                                position = GeoPoint(initialLat, initialLon)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                title = "Position du garage"
                            }
                            overlays.add(newMarker)
                            marker = newMarker

                            overlays.add(object : org.osmdroid.views.overlay.Overlay() {
                                override fun onSingleTapConfirmed(
                                    e: android.view.MotionEvent,
                                    mapView: MapView
                                ): Boolean {
                                    val projection = mapView.projection
                                    val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

                                    selectedLat = geoPoint.latitude
                                    selectedLon = geoPoint.longitude

                                    marker?.position = geoPoint
                                    mapView.invalidate()

                                    return true
                                }
                            })

                            mapView = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "📍 ${String.format(Locale.getDefault(), "%.5f", selectedLat)}, ${String.format(Locale.getDefault(), "%.5f", selectedLon)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeepPurple,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onLocationSelected(selectedLat, selectedLon) },
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
            ) {
                Text("Confirmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDetach()
        }
    }
}