package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.viewmodel.GarageViewModel
import com.example.karhebti_android.viewmodel.ReclamationViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReclamationScreen(
    onBackClick: () -> Unit,
    onReclamationCreated: () -> Unit
) {
    val context = LocalContext.current
    val reclamationViewModel: ReclamationViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val garageViewModel: GarageViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    val createReclamationState by reclamationViewModel.createReclamationState.observeAsState()
    val garagesState by garageViewModel.garagesState.observeAsState()

    var type by remember { mutableStateOf("garage") }
    var titre by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedGarageId by remember { mutableStateOf<String?>(null) }
    var expandedGarageDropdown by remember { mutableStateOf(false) }

    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        android.util.Log.d("AddReclamation", "Loading garages...")
        garageViewModel.getGarages()
    }

    LaunchedEffect(garagesState) {
        when (val state = garagesState) {
            is Resource.Success -> {
                android.util.Log.d("AddReclamation", "Garages loaded: ${state.data?.size} garages")
                state.data?.forEach { garage ->
                    android.util.Log.d("AddReclamation", "Garage: ${garage.id} - ${garage.nom}")
                }
            }
            is Resource.Error -> android.util.Log.e("AddReclamation", "Error loading garages: ${state.message}")
            is Resource.Loading -> android.util.Log.d("AddReclamation", "Loading garages...")
            null -> android.util.Log.d("AddReclamation", "Garages state is null")
        }
    }

    LaunchedEffect(createReclamationState) {
        when (createReclamationState) {
            is Resource.Success -> {
                onReclamationCreated()
            }
            is Resource.Error -> {
                errorMessage = (createReclamationState as Resource.Error).message ?: "Erreur inconnue"
                showErrorDialog = true
            }
            else -> {}
        }
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Erreur") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle Réclamation") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Créer une réclamation",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Type de réclamation
            Text(
                text = "Type de réclamation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = type == "garage",
                    onClick = { type = "garage" },
                    label = { Text("Garage") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = type == "service",
                    onClick = { type = "service" },
                    label = { Text("Service") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Sélection du garage (si type = garage)
            if (type == "garage") {
                Text(
                    text = "Sélectionner un garage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                ExposedDropdownMenuBox(
                    expanded = expandedGarageDropdown,
                    onExpandedChange = { expandedGarageDropdown = !expandedGarageDropdown }
                ) {
                    val selectedGarageName = garagesState?.let { state ->
                        (state as? Resource.Success)?.data?.find { it.id == selectedGarageId }?.nom
                    } ?: ""

                    OutlinedTextField(
                        value = selectedGarageName,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Sélectionner un garage") },
                        label = { Text("Garage") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGarageDropdown) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        isError = selectedGarageId == null
                    )

                    ExposedDropdownMenu(
                        expanded = expandedGarageDropdown,
                        onDismissRequest = { expandedGarageDropdown = false }
                    ) {
                        when (val resource = garagesState) {
                            is Resource.Success -> {
                                if (resource.data.isNullOrEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Aucun garage disponible") },
                                        onClick = {},
                                        enabled = false
                                    )
                                } else {
                                    resource.data.forEach { garage ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = garage.nom,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = garage.adresse,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedGarageId = garage.id
                                                expandedGarageDropdown = false
                                                android.util.Log.d("AddReclamation", "Selected garage: ${garage.id} - ${garage.nom}")
                                            }
                                        )
                                    }
                                }
                            }
                            is Resource.Loading -> {
                                DropdownMenuItem(
                                    text = { Text("Chargement...") },
                                    onClick = {},
                                    enabled = false
                                )
                            }
                            is Resource.Error -> {
                                DropdownMenuItem(
                                    text = { Text("Erreur: ${resource.message}") },
                                    onClick = {},
                                    enabled = false
                                )
                            }
                            else -> {
                                DropdownMenuItem(
                                    text = { Text("Chargement...") },
                                    onClick = {},
                                    enabled = false
                                )
                            }
                        }
                    }
                }
            }

            // Titre
            OutlinedTextField(
                value = titre,
                onValueChange = { titre = it },
                label = { Text("Titre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors()
            )

            // Message
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                minLines = 6,
                maxLines = 10,
                colors = OutlinedTextFieldDefaults.colors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bouton Soumettre
            Button(
                onClick = {
                    when {
                        titre.isBlank() -> {
                            errorMessage = "Veuillez entrer un titre"
                            showErrorDialog = true
                        }
                        message.isBlank() -> {
                            errorMessage = "Veuillez entrer un message"
                            showErrorDialog = true
                        }
                        type == "garage" && selectedGarageId == null -> {
                            errorMessage = "Veuillez sélectionner un garage"
                            showErrorDialog = true
                        }
                        else -> {
                            android.util.Log.d("AddReclamation", "Creating reclamation: type=$type, titre=$titre, garageId=$selectedGarageId")
                            reclamationViewModel.createReclamation(
                                type = type,
                                titre = titre,
                                message = message,
                                garageId = if (type == "garage") selectedGarageId else null,
                                serviceId = null // TODO: Add service selection if needed
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = createReclamationState !is Resource.Loading
            ) {
                if (createReclamationState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Soumettre la réclamation")
            }
        }
    }
}
