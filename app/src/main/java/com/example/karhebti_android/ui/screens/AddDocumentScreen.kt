package com.example.karhebti_android.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.karhebti_android.data.api.CreateDocumentRequest
import com.example.karhebti_android.data.api.UpdateDocumentRequest
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.viewmodel.CarViewModel
import com.example.karhebti_android.viewmodel.DocumentViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentScreen(
    onBackClick: () -> Unit,
    documentId: String? = null
) {
    val context = LocalContext.current
    val documentViewModel: DocumentViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val carViewModel: CarViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    val isEditMode = documentId != null

    // Document fields state
    var selectedType by remember { mutableStateOf("") }
    var dateEmission by remember { mutableStateOf<Calendar?>(null) }
    var dateExpiration by remember { mutableStateOf<Calendar?>(null) }
    var selectedCarId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Dropdown states
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var carMenuExpanded by remember { mutableStateOf(false) }

    // File/image state
    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Permission helpers
    fun readPermissionForApi(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                selectedFilePath = copyUriToFile(context, it)
            }
        }
    )

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            bitmap?.let {
                val file = File(context.cacheDir, "doc_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                selectedFilePath = file.absolutePath
                selectedImageUri = Uri.fromFile(file)
            }
        }
    )

    // Read permission launcher
    val readPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                galleryLauncher.launch("image/*")
            } else {
                Toast.makeText(context, "Permission lecture média refusée", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                cameraLauncher.launch(null)
            } else {
                Toast.makeText(context, "Permission caméra refusée", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Date pickers
    val dateEmissionPicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dateEmission = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        },
        dateEmission?.get(Calendar.YEAR) ?: Calendar.getInstance().get(Calendar.YEAR),
        dateEmission?.get(Calendar.MONTH) ?: Calendar.getInstance().get(Calendar.MONTH),
        dateEmission?.get(Calendar.DAY_OF_MONTH) ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    val dateExpirationPicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dateExpiration = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        },
        dateExpiration?.get(Calendar.YEAR) ?: Calendar.getInstance().get(Calendar.YEAR),
        dateExpiration?.get(Calendar.MONTH) ?: Calendar.getInstance().get(Calendar.MONTH),
        dateExpiration?.get(Calendar.DAY_OF_MONTH) ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    // Load data on start
    LaunchedEffect(Unit) {
        carViewModel.getMyCars()
        documentId?.let { documentViewModel.getDocumentById(it) }
    }

    // Pre-fill form in edit mode
    val documentDetailState by documentViewModel.documentDetailState.observeAsState()
    LaunchedEffect(documentDetailState) {
        if (isEditMode) {
            (documentDetailState as? Resource.Success)?.data?.let { doc ->
                selectedType = doc.type.replace("_", " ").replaceFirstChar { it.titlecase(Locale.getDefault()) }

                dateEmission = Calendar.getInstance().apply {
                    time = doc.dateEmission
                }
                dateExpiration = Calendar.getInstance().apply {
                    time = doc.dateExpiration
                }

                selectedCarId = doc.voiture
            }
        }
    }

    // Observe creation/update state
    val createDocumentState by documentViewModel.createDocumentState.observeAsState()
    val updateDocumentState by documentViewModel.updateDocumentState.observeAsState()

    LaunchedEffect(createDocumentState, updateDocumentState) {
        val state = if (isEditMode) updateDocumentState else createDocumentState
        when (state) {
            is Resource.Success -> {
                isLoading = false
                val message = if (isEditMode) "Document modifié avec succès" else "Document ajouté avec succès"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                onBackClick()
            }
            is Resource.Error -> {
                isLoading = false
                Toast.makeText(context, "Erreur: ${state.message}", Toast.LENGTH_LONG).show()
            }
            is Resource.Loading -> {
                isLoading = true
            }
            null -> {}
        }
    }

    val carsState by carViewModel.carsState.observeAsState()
    val documentTypes = listOf("Assurance", "Carte Grise", "Contrôle Technique", "Autre")

    // Format dates - ISO 8601 avec timezone
    val sdfDisplay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val sdfIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Modifier le Document" else "Ajouter un Document") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type dropdown
            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { if (!isLoading) typeMenuExpanded = !typeMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type de document") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = !isLoading
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    documentTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedType = type
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Car dropdown
            ExposedDropdownMenuBox(
                expanded = carMenuExpanded,
                onExpandedChange = { if (!isLoading && !isEditMode) carMenuExpanded = !carMenuExpanded }
            ) {
                val selectedCarText = when (val state = carsState) {
                    is Resource.Success -> state.data?.find { it.id == selectedCarId }?.let { "${it.marque} ${it.modele}" } ?: ""
                    else -> "Chargement..."
                }
                OutlinedTextField(
                    value = selectedCarText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Véhicule") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = carMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = !isLoading && !isEditMode
                )
                if (!isEditMode) {
                    ExposedDropdownMenu(
                        expanded = carMenuExpanded,
                        onDismissRequest = { carMenuExpanded = false }
                    ) {
                        (carsState as? Resource.Success)?.data?.forEach { car ->
                            DropdownMenuItem(
                                text = { Text("${car.marque} ${car.modele}") },
                                onClick = {
                                    selectedCarId = car.id
                                    carMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Date d'émission picker
            OutlinedTextField(
                value = dateEmission?.let { sdfDisplay.format(it.time) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Date d\'émission") },
                trailingIcon = {
                    IconButton(onClick = { if (!isLoading) dateEmissionPicker.show() }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Sélectionner la date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) { dateEmissionPicker.show() }
            )

            // Date d'expiration picker
            OutlinedTextField(
                value = dateExpiration?.let { sdfDisplay.format(it.time) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Date d\'expiration") },
                trailingIcon = {
                    IconButton(onClick = { if (!isLoading) dateExpirationPicker.show() }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Sélectionner la date")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) { dateExpirationPicker.show() }
            )

            // Image selection UI
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    readPermissionLauncher.launch(readPermissionForApi())
                }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Galerie")
                }
                Button(onClick = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Caméra")
                }
            }

            selectedImageUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Button(
                onClick = {
                    if (isEditMode) {
                        val dateEmissionStr = dateEmission?.let { sdfIso.format(it.time) }
                        val dateExpirationStr = dateExpiration?.let { sdfIso.format(it.time) }
                        // Format du type : minuscules AVEC ESPACES (ex: "carte grise")
                        val typeStr = selectedType.lowercase()

                        android.util.Log.d("AddDocumentScreen", "UPDATE - Type: $typeStr")
                        android.util.Log.d("AddDocumentScreen", "UPDATE - DateEmission: $dateEmissionStr")
                        android.util.Log.d("AddDocumentScreen", "UPDATE - DateExpiration: $dateExpirationStr")

                        val request = UpdateDocumentRequest(
                            type = typeStr,
                            dateEmission = dateEmissionStr,
                            dateExpiration = dateExpirationStr
                        )
                        documentId?.let { id ->
                            documentViewModel.updateDocument(id, request, selectedFilePath)
                        }
                    } else {
                        val dateEmissionStr = sdfIso.format(dateEmission!!.time)
                        val dateExpirationStr = sdfIso.format(dateExpiration!!.time)
                        // Format du type : minuscules AVEC ESPACES (ex: "carte grise")
                        val typeStr = selectedType.lowercase()

                        android.util.Log.d("AddDocumentScreen", "CREATE - Type: $typeStr")
                        android.util.Log.d("AddDocumentScreen", "CREATE - DateEmission: $dateEmissionStr")
                        android.util.Log.d("AddDocumentScreen", "CREATE - DateExpiration: $dateExpirationStr")
                        android.util.Log.d("AddDocumentScreen", "CREATE - Voiture: $selectedCarId")
                        android.util.Log.d("AddDocumentScreen", "CREATE - Fichier: ${selectedFilePath ?: "none"}")

                        val request = CreateDocumentRequest(
                            type = typeStr,
                            dateEmission = dateEmissionStr,
                            dateExpiration = dateExpirationStr,
                            fichier = selectedFilePath ?: "",
                            voiture = selectedCarId ?: ""
                        )
                        documentViewModel.createDocument(request, selectedFilePath)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedType.isNotBlank() && dateEmission != null && dateExpiration != null && (isEditMode || selectedCarId != null) && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (isEditMode) "Enregistrer les modifications" else "Enregistrer")
                }
            }
        }
    }
}

private fun copyUriToFile(context: Context, uri: Uri): String? {
    try {
        val resolver: ContentResolver = context.contentResolver
        val inputStream: InputStream? = resolver.openInputStream(uri)

        val documentsDir = File(context.filesDir, "documents")
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }

        val fileName = "doc_${System.currentTimeMillis()}.${getExtension(resolver, uri) ?: "jpg"}"
        val file = File(documentsDir, fileName)

        FileOutputStream(file).use { output ->
            inputStream?.copyTo(output)
        }

        android.util.Log.d("AddDocumentScreen", "File copied to: ${file.absolutePath}")
        return file.absolutePath
    } catch (e: Exception) {
        android.util.Log.e("AddDocumentScreen", "Error copying file", e)
        e.printStackTrace()
        return null
    }
}

private fun getExtension(resolver: ContentResolver, uri: Uri): String? {
    val type = resolver.getType(uri)
    return when (type) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/gif" -> "gif"
        "application/pdf" -> "pdf"
        else -> null
    }
}
