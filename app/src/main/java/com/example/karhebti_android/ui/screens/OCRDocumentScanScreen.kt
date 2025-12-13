package com.example.karhebti_android.ui.screens

import android.Manifest
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.karhebti_android.data.api.CreateDocumentRequest
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.ui.components.StepProgressBar
import com.example.karhebti_android.viewmodel.CarViewModel
import com.example.karhebti_android.viewmodel.DocumentViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OCRDocumentScanScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val documentViewModel: DocumentViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    val carViewModel: CarViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )
    // Utiliser le ViewModel OCR qui utilise le service local OCRApiService
    val ocrViewModel: com.example.karhebti_android.viewmodel.OCRViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    var currentStep by remember { mutableStateOf(0) }
    val steps = listOf("Scan", "Extraction", "Vérification", "Sauvegarde", "Terminé")

    // Support multiple images
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedFilePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    
    // Observer l'état du scan OCR
    val scanState by ocrViewModel.scanState.collectAsState()
    val extractedData by ocrViewModel.extractedData.collectAsState()
    val scanProgress by ocrViewModel.progress.collectAsState()

    // Extracted data - TOUS LES CHAMPS
    var extractedType by remember { mutableStateOf("") }
    var extractedDateEmission by remember { mutableStateOf<Calendar?>(null) }
    var extractedDateExpiration by remember { mutableStateOf<Calendar?>(null) }
    var extractedDocumentNumber by remember { mutableStateOf("") }
    var extractedHolderName by remember { mutableStateOf("") }
    var extractedImmatriculation by remember { mutableStateOf("") }
    var extractedRawText by remember { mutableStateOf("") }
    var selectedCarId by remember { mutableStateOf<String?>(null) }

    // Mettre à jour TOUS les champs locaux quand les données extraites changent
    LaunchedEffect(extractedData) {
        extractedData?.let { data ->
            extractedType = data.documentType
            extractedDocumentNumber = data.documentNumber
            extractedHolderName = data.holderName
            extractedImmatriculation = data.immatriculation
            extractedRawText = data.rawText

            // Parser les dates (format attendu: JJ/MM/AAAA ou JJ-MM-AAAA)
            extractedDateEmission = parseLocalDate(data.issuedDate)
            extractedDateExpiration = parseLocalDate(data.expiryDate)
            
            android.util.Log.d("OCRExtraction", "✅ Données extraites: Type=$extractedType, Numéro=$extractedDocumentNumber, Titulaire=$extractedHolderName, Immat=$extractedImmatriculation")

            currentStep = 2
        }
    }

    // Permission helpers
    fun readPermissionForApi(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Gallery launcher (multiple)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris: List<Uri> ->
            if (uris.isNotEmpty()) {
                selectedImageUris = selectedImageUris + uris
                val filePaths = uris.mapNotNull { copyUriToFile(context, it) }
                selectedFilePaths = selectedFilePaths + filePaths
                if (filePaths.isNotEmpty()) {
                    currentStep = 1
                    // Lancer le scan sur la première image
                    ocrViewModel.scanDocument(File(filePaths.first()))
                }
            }
        }
    )

    // Camera launcher (ajoute une image à la liste)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            bitmap?.let {
                val file = File(context.cacheDir, "doc_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                val filePath = file.absolutePath
                selectedFilePaths = selectedFilePaths + filePath
                val uri = Uri.fromFile(file)
                selectedImageUris = selectedImageUris + uri
                currentStep = 1
                // Lancer le scan
                ocrViewModel.scanDocument(file)
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

    // Load cars
    LaunchedEffect(Unit) {
        carViewModel.getMyCars()
    }

    val carsState by carViewModel.carsState.observeAsState()
    val createDocumentState by documentViewModel.createDocumentState.observeAsState()

    LaunchedEffect(createDocumentState) {
        when (createDocumentState) {
            is Resource.Success -> {
                currentStep = 4
                isProcessing = false
                Toast.makeText(context, "Document ajouté avec succès", Toast.LENGTH_SHORT).show()
                kotlinx.coroutines.delay(1000)
                onBackClick()
            }
            is Resource.Error -> {
                isProcessing = false
                Toast.makeText(context, "Erreur: ${(createDocumentState as Resource.Error).message}", Toast.LENGTH_LONG).show()
            }
            is Resource.Loading -> {
                isProcessing = true
                currentStep = 3
            }
            null -> {}
        }
    }

    val sdfIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanner un Document (OCR)") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress Bar
            StepProgressBar(steps = steps, currentStep = currentStep)

            Spacer(modifier = Modifier.height(16.dp))

            when (currentStep) {
                0 -> {
                    // Étape 1: Sélection de l'image
                    Text("Choisissez une image du document", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { readPermissionLauncher.launch(readPermissionForApi()) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Galerie")
                        }
                        Button(
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Caméra")
                        }
                    }
                }
                1 -> {
                    // Étape 2: Extraction OCR en cours
                    selectedImageUris.forEach { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (scanState is com.example.karhebti_android.viewmodel.ScanState.Scanning) {
                        CircularProgressIndicator(progress = scanProgress)
                        Text("Extraction des données en cours... ${(scanProgress * 100).toInt()}%")
                    } else if (scanState is com.example.karhebti_android.viewmodel.ScanState.Error) {
                         Text(
                            text = "Erreur: ${(scanState as com.example.karhebti_android.viewmodel.ScanState.Error).message}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { currentStep = 0 }) {
                            Text("Réessayer")
                        }
                    }
                }
                2 -> {
                    // Étape 3: Vérification et correction des données extraites
                    selectedImageUris.forEach { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Vérifiez et corrigez les données :", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Type de document (modifiable)
                    val documentTypes = listOf("Assurance", "Carte Grise", "Permis de Conduire", "Contrôle Technique", "Vignette")
                    var typeMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = typeMenuExpanded,
                        onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = extractedType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type de document") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false }
                        ) {
                            documentTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        extractedType = type
                                        typeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Numéro de document (modifiable)
                    OutlinedTextField(
                        value = extractedDocumentNumber,
                        onValueChange = { extractedDocumentNumber = it },
                        label = { Text("Numéro de document") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ex: 123456789") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Nom du titulaire (modifiable)
                    OutlinedTextField(
                        value = extractedHolderName,
                        onValueChange = { extractedHolderName = it },
                        label = { Text("Nom du titulaire") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ex: MOSBEH Eya") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Immatriculation (modifiable)
                    OutlinedTextField(
                        value = extractedImmatriculation,
                        onValueChange = { extractedImmatriculation = it },
                        label = { Text("Immatriculation") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ex: 123 TU 4567") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date d'émission (modifiable avec DatePicker)
                    val dateEmissionPicker = remember {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                extractedDateEmission = Calendar.getInstance().apply {
                                    set(year, month, dayOfMonth, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                            },
                            extractedDateEmission?.get(Calendar.YEAR) ?: Calendar.getInstance().get(Calendar.YEAR),
                            extractedDateEmission?.get(Calendar.MONTH) ?: Calendar.getInstance().get(Calendar.MONTH),
                            extractedDateEmission?.get(Calendar.DAY_OF_MONTH) ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                        )
                    }

                    OutlinedTextField(
                        value = extractedDateEmission?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.time) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date d'émission") },
                        trailingIcon = {
                            IconButton(onClick = { dateEmissionPicker.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Sélectionner la date")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dateEmissionPicker.show() },
                        placeholder = { Text("Cliquez pour sélectionner") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date d'expiration (VISIBLE SEULEMENT SI CE N'EST PAS UNE CARTE GRISE)
                    if (extractedType != "Carte Grise") {
                        val dateExpirationPicker = remember {
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    extractedDateExpiration = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                },
                                extractedDateExpiration?.get(Calendar.YEAR) ?: Calendar.getInstance().get(Calendar.YEAR),
                                extractedDateExpiration?.get(Calendar.MONTH) ?: Calendar.getInstance().get(Calendar.MONTH),
                                extractedDateExpiration?.get(Calendar.DAY_OF_MONTH) ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                            )
                        }

                        OutlinedTextField(
                            value = extractedDateExpiration?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.time) } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date d'expiration") },
                            trailingIcon = {
                                IconButton(onClick = { dateExpirationPicker.show() }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Sélectionner la date")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dateExpirationPicker.show() },
                            placeholder = { Text("Cliquez pour sélectionner") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sélection de la voiture
                    var carMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = carMenuExpanded,
                        onExpandedChange = { carMenuExpanded = !carMenuExpanded }
                    ) {
                        val selectedCarText = when (val state = carsState) {
                            is Resource.Success -> state.data?.find { it.id == selectedCarId }?.let { "${it.marque} ${it.modele}" } ?: "Sélectionner une voiture"
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
                                .menuAnchor()
                        )
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            when {
                                selectedCarId == null -> {
                                    Toast.makeText(context, "Veuillez sélectionner une voiture", Toast.LENGTH_SHORT).show()
                                }
                                extractedType.isBlank() -> {
                                    Toast.makeText(context, "Veuillez sélectionner un type de document", Toast.LENGTH_SHORT).show()
                                }
                                extractedDateEmission == null -> {
                                    Toast.makeText(context, "Veuillez sélectionner la date d'émission", Toast.LENGTH_SHORT).show()
                                }
                                // Validation date expiration seulement si ce n'est pas une carte grise
                                extractedType != "Carte Grise" && extractedDateExpiration == null -> {
                                    Toast.makeText(context, "Veuillez sélectionner la date d'expiration", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    // Si c'est une carte grise, on met une date d'expiration lointaine ou vide selon le backend
                                    // Ici on met null ou une date par défaut si le backend l'exige
                                    val expirationDateStr = if (extractedType == "Carte Grise") {
                                        // 50 ans plus tard par défaut ou vide ? 
                                        // Le backend attend probablement une date valide si le champ est requis.
                                        // Mettons une date lointaine pour éviter les erreurs 400
                                        val cal = Calendar.getInstance()
                                        cal.add(Calendar.YEAR, 50)
                                        sdfIso.format(cal.time)
                                    } else {
                                        sdfIso.format(extractedDateExpiration!!.time)
                                    }

                                    val request = CreateDocumentRequest(
                                        type = extractedType.lowercase(), // Format: "carte grise" (with spaces, not underscores)
                                        dateEmission = sdfIso.format(extractedDateEmission!!.time),
                                        dateExpiration = expirationDateStr,
                                        fichier = selectedFilePaths.joinToString(","),
                                        voiture = selectedCarId!!
                                    )
                                    documentViewModel.createDocument(request, selectedFilePaths.first())
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        Text("Confirmer et Enregistrer")
                    }
                }
                3 -> {
                    // Étape 4: Sauvegarde en cours
                    CircularProgressIndicator()
                    Text("Sauvegarde en cours...")
                }
                4 -> {
                    // Étape 5: Terminé
                    Text("✓ Document ajouté avec succès!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Afficher toutes les images sélectionnées
            if (selectedImageUris.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text("Images sélectionnées :", style = MaterialTheme.typography.titleMedium)
                    LazyRow {
                        items(selectedImageUris) { uri ->
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp).padding(4.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
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

        return file.absolutePath
    } catch (e: Exception) {
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

// Fonction utilitaire pour parser les dates locales (JJ/MM/AAAA ou JJ-MM-AAAA)
private fun parseLocalDate(dateString: String): Calendar? {
    if (dateString.isBlank()) return null
    return try {
        // Essayer plusieurs formats
        val formats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy", "yyyy-MM-dd")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                val date = sdf.parse(dateString)
                if (date != null) {
                    return Calendar.getInstance().apply { time = date }
                }
            } catch (e: Exception) {
                // Continuer
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}
