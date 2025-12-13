package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.viewmodel.ReclamationViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReclamationScreen(
    reclamationId: String,
    onBackClick: () -> Unit,
    onReclamationUpdated: () -> Unit
) {
    val context = LocalContext.current
    val reclamationViewModel: ReclamationViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    val reclamationDetailState by reclamationViewModel.reclamationDetailState.observeAsState()
    val updateReclamationState by reclamationViewModel.updateReclamationState.observeAsState()

    var titre by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isInitialized by remember { mutableStateOf(false) }

    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(reclamationId) {
        reclamationViewModel.getReclamationById(reclamationId)
    }

    LaunchedEffect(reclamationDetailState) {
        if (reclamationDetailState is Resource.Success && !isInitialized) {
            val reclamation = (reclamationDetailState as Resource.Success).data
            if (reclamation != null) {
                titre = reclamation.titre
                message = reclamation.message
                isInitialized = true
            }
        }
    }

    LaunchedEffect(updateReclamationState) {
        when (updateReclamationState) {
            is Resource.Success -> {
                onReclamationUpdated()
            }
            is Resource.Error -> {
                errorMessage = (updateReclamationState as Resource.Error).message ?: "Erreur inconnue"
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
                title = { Text("Modifier la réclamation") },
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
                text = "Modifier votre réclamation",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

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

            // Bouton Mettre à jour
            Button(
                onClick = {
                    if (titre.isNotBlank() && message.isNotBlank()) {
                        reclamationViewModel.updateReclamation(
                            id = reclamationId,
                            titre = titre,
                            message = message
                        )
                    } else {
                        errorMessage = "Veuillez remplir tous les champs"
                        showErrorDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = updateReclamationState !is Resource.Loading
            ) {
                if (updateReclamationState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Mettre à jour")
            }
        }
    }
}
