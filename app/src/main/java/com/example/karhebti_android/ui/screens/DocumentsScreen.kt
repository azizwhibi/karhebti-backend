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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.api.DocumentResponse
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.viewmodel.DocumentViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import com.example.karhebti_android.data.repository.TranslationManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import java.text.SimpleDateFormat
import java.util.*
import androidx.navigation.NavController
import com.example.karhebti_android.ui.components.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onBackClick: () -> Unit,
    onAddDocumentClick: () -> Unit,
    onDocumentClick: (String) -> Unit,
    navController: NavController? = null
) {
    val context = LocalContext.current
    val documentViewModel: DocumentViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
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
    var documentsTitle by remember { mutableStateOf("Documents") }
    var backText by remember { mutableStateOf("Retour") }
    var refreshText by remember { mutableStateOf("Actualiser") }
    var addDocumentText by remember { mutableStateOf("Ajouter document") }
    var allText by remember { mutableStateOf("Tous") }
    var registrationText by remember { mutableStateOf("Carte grise") }
    var insuranceText by remember { mutableStateOf("Assurance") }
    var inspectionText by remember { mutableStateOf("Contrôle technique") }
    var otherText by remember { mutableStateOf("Autre") }
    var loadingText by remember { mutableStateOf("Chargement des documents...") }
    var noDocumentsText by remember { mutableStateOf("Aucun document") }
    var searchText by remember { mutableStateOf("Rechercher un document...") }
    var clearText by remember { mutableStateOf("Effacer") }
    var deleteText by remember { mutableStateOf("Supprimer") }
    var cancelText by remember { mutableStateOf("Annuler") }

    // Update translations when language changes
    LaunchedEffect(currentLanguage) {
        coroutineScope.launch {
            documentsTitle = translationManager.translate("documents_title", "Documents", currentLanguage)
            backText = translationManager.translate("back", "Retour", currentLanguage)
            refreshText = translationManager.translate("refresh", "Actualiser", currentLanguage)
            addDocumentText = translationManager.translate("add_document", "Ajouter document", currentLanguage)
            allText = translationManager.translate("all", "Tous", currentLanguage)
            registrationText = translationManager.translate("registration", "Carte grise", currentLanguage)
            insuranceText = translationManager.translate("insurance", "Assurance", currentLanguage)
            inspectionText = translationManager.translate("inspection", "Contrôle technique", currentLanguage)
            otherText = translationManager.translate("other", "Autre", currentLanguage)
            loadingText = translationManager.translate("documents_loading", "Chargement des documents...", currentLanguage)
            noDocumentsText = translationManager.translate("no_documents", "Aucun document", currentLanguage)
            searchText = translationManager.translate("search_document", "Rechercher un document...", currentLanguage)
            clearText = translationManager.translate("clear", "Effacer", currentLanguage)
            deleteText = translationManager.translate("delete", "Supprimer", currentLanguage)
            cancelText = translationManager.translate("cancel", "Annuler", currentLanguage)
        }
    }

    val documentsState by documentViewModel.documentsState.observeAsState()
    var selectedFilter by remember { mutableStateOf(allText) }
    var showDeleteDialog by remember { mutableStateOf<DocumentResponse?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        documentViewModel.getDocuments()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(documentsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backText,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { documentViewModel.getDocuments() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = refreshText,
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
        bottomBar = {
            if (navController != null) {
                BottomNavigationBar(navController = navController)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddDocumentClick() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = addDocumentText)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text(searchText) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = searchText,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = clearText,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // Filter chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf(allText, registrationText, insuranceText, inspectionText, otherText)) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // Content
            item {
                when (val state = documentsState) {
                    is Resource.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        val allDocs = state.data ?: emptyList()

                        // Filtrage par type
                        val typeFilteredDocs = if (selectedFilter == allText) allDocs
                        else allDocs.filter { it.type.equals(selectedFilter, ignoreCase = true) }

                        // Filtrage par recherche
                        val filteredDocs = if (searchQuery.isEmpty()) {
                            typeFilteredDocs
                        } else {
                            typeFilteredDocs.filter { doc ->
                                doc.type.contains(searchQuery, ignoreCase = true) ||
                                doc.description?.contains(searchQuery, ignoreCase = true) == true ||
                                doc.etat?.contains(searchQuery, ignoreCase = true) == true
                            }
                        }

                        if (filteredDocs.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(
                                        if (searchQuery.isNotEmpty())
                                            Icons.Default.SearchOff
                                        else
                                            Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        if (searchQuery.isNotEmpty())
                                            "Aucun résultat"
                                        else if (selectedFilter != allText)
                                            "Aucun document de ce type"
                                        else
                                            noDocumentsText,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        if (searchQuery.isNotEmpty())
                                            "Essayez avec d'autres mots-clés"
                                        else
                                            "Ajoutez vos documents importants",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            filteredDocs.forEach { document ->
                                DocumentCard(
                                    document = document,
                                    onClick = { onDocumentClick(document.id) },
                                    onDelete = { showDeleteDialog = document }
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
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
                                    "Erreur",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    state.message ?: "Une erreur est survenue",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { documentViewModel.getDocuments() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
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

    // Delete dialog
    showDeleteDialog?.let { document ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Supprimer le document ?") },
            text = { Text("Voulez-vous vraiment supprimer ce document ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        documentViewModel.deleteDocument(document.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(deleteText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(cancelText)
                }
            }
        )
    }
}

@Composable
fun DocumentCard(
    document: DocumentResponse,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    val now = Date()
    val daysUntilExpiry = ((document.dateExpiration.time - now.time) / (1000 * 60 * 60 * 24)).toInt()

    val (statusLabel, statusColor) = when {
        daysUntilExpiry < 0 -> "Expiré" to MaterialTheme.colorScheme.error
        daysUntilExpiry <= 30 -> "Expire bientôt" to MaterialTheme.colorScheme.tertiary
        else -> "Valide" to MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = document.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Expire le ${dateFormat.format(document.dateExpiration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(statusLabel, style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusColor.copy(alpha = 0.2f),
                        labelColor = statusColor
                    )
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Supprimer",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
