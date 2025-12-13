package com.example.karhebti_android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.karhebti_android.data.ocr.ExtractedDocumentData
import com.example.karhebti_android.viewmodel.OCRViewModel
import com.example.karhebti_android.viewmodel.ScanState
import com.example.karhebti_android.viewmodel.ViewModelFactory
import java.io.File
import java.io.FileOutputStream

/**
 * Écran de scan de documents avec OCR
 * Permet de scanner un document, extraire les informations et les éditer avant sauvegarde
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScanScreen(
    onBackClick: () -> Unit,
    onDocumentSaved: (ExtractedDocumentData) -> Unit
) {
    val context = LocalContext.current
    val ocrViewModel: OCRViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    val scanState by ocrViewModel.scanState.collectAsState()
    val extractedData by ocrViewModel.extractedData.collectAsState()
    val progress by ocrViewModel.progress.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Launcher pour sélectionner une image
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Copier l'image dans un fichier temporaire et scanner
            val tempFile = File(context.cacheDir, "temp_scan_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            ocrViewModel.scanDocument(tempFile)
        }
    }

    // Launcher pour prendre une photo
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val tempFile = File(context.cacheDir, "temp_camera_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { output ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, output)
            }
            selectedImageUri = Uri.fromFile(tempFile)
            ocrViewModel.scanDocument(tempFile)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scanner un Document") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barre de progression en haut
            if (progress > 0f) {
                ScanProgressBar(progress = progress)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (scanState) {
                    is ScanState.Idle -> {
                        // Interface de sélection d'image
                        ScanIdleContent(
                            onSelectFromGallery = { imagePickerLauncher.launch("image/*") },
                            onTakePhoto = { cameraLauncher.launch(null) }
                        )
                    }

                    is ScanState.Scanning -> {
                        // Affichage pendant le scan
                        ScanningContent(
                            imageUri = selectedImageUri,
                            progress = progress
                        )
                    }

                    is ScanState.Success -> {
                        // Affichage des résultats
                        extractedData?.let { data ->
                            ScanSuccessContent(
                                imageUri = selectedImageUri,
                                extractedData = data,
                                onEdit = { showEditDialog = true },
                                onSave = {
                                    onDocumentSaved(data)
                                    onBackClick()
                                },
                                onRescan = {
                                    ocrViewModel.resetScan()
                                    selectedImageUri = null
                                }
                            )
                        }
                    }

                    is ScanState.Error -> {
                        // Affichage d'erreur
                        ScanErrorContent(
                            errorMessage = (scanState as ScanState.Error).message,
                            onRetry = {
                                selectedImageUri?.let { uri ->
                                    val tempFile = File(context.cacheDir, "temp_retry_${System.currentTimeMillis()}.jpg")
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        FileOutputStream(tempFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    ocrViewModel.scanDocument(tempFile)
                                }
                            },
                            onCancel = {
                                ocrViewModel.resetScan()
                                selectedImageUri = null
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog d'édition des données extraites
    if (showEditDialog && extractedData != null) {
        EditExtractedDataDialog(
            data = extractedData!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedData ->
                ocrViewModel.updateExtractedData(updatedData)
                showEditDialog = false
            }
        )
    }
}

/**
 * Barre de progression du scan
 */
@Composable
fun ScanProgressBar(progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = when {
                progress < 0.4f -> "Préparation du scan..."
                progress < 0.8f -> "Analyse du document..."
                else -> "Extraction des informations..."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Contenu initial - sélection d'image
 */
@Composable
fun ScanIdleContent(
    onSelectFromGallery: () -> Unit,
    onTakePhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.DocumentScanner,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Scanner un Document",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Scannez une carte grise, permis de conduire, assurance ou contrôle technique pour extraire automatiquement les informations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bouton Galerie
        Button(
            onClick = onSelectFromGallery,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Choisir depuis la galerie")
        }

        // Bouton Caméra
        OutlinedButton(
            onClick = onTakePhoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Prendre une photo")
        }
    }
}

/**
 * Contenu pendant le scan
 */
@Composable
fun ScanningContent(
    imageUri: Uri?,
    progress: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Image scannée
        imageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Document scanné",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Analyse en cours...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Contenu après scan réussi
 */
@Composable
fun ScanSuccessContent(
    imageUri: Uri?,
    extractedData: ExtractedDocumentData,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onRescan: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Image
        imageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Document scanné",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Scan réussi!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Informations extraites
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Informations extraites",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                ExtractedInfoRow("Type", extractedData.documentType)
                ExtractedInfoRow("Numéro", extractedData.documentNumber)
                ExtractedInfoRow("Immatriculation", extractedData.immatriculation)
                ExtractedInfoRow("Date émission", extractedData.issuedDate)
                ExtractedInfoRow("Date expiration", extractedData.expiryDate)
                ExtractedInfoRow("Titulaire", extractedData.holderName)
            }
        }

        // Boutons d'action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onRescan,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Rescanner")
            }

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Modifier")
            }
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enregistrer le document")
        }
    }
}

@Composable
fun ExtractedInfoRow(label: String, value: String) {
    if (value.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Contenu en cas d'erreur
 */
@Composable
fun ScanErrorContent(
    errorMessage: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = "Erreur de scan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Réessayer")
        }

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Annuler")
        }
    }
}

/**
 * Dialog pour éditer les données extraites
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExtractedDataDialog(
    data: ExtractedDocumentData,
    onDismiss: () -> Unit,
    onSave: (ExtractedDocumentData) -> Unit
) {
    var documentType by remember { mutableStateOf(data.documentType) }
    var documentNumber by remember { mutableStateOf(data.documentNumber) }
    var immatriculation by remember { mutableStateOf(data.immatriculation) }
    var issuedDate by remember { mutableStateOf(data.issuedDate) }
    var expiryDate by remember { mutableStateOf(data.expiryDate) }
    var holderName by remember { mutableStateOf(data.holderName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier les informations") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = documentType,
                    onValueChange = { documentType = it },
                    label = { Text("Type de document") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = documentNumber,
                    onValueChange = { documentNumber = it },
                    label = { Text("Numéro") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = immatriculation,
                    onValueChange = { immatriculation = it },
                    label = { Text("Immatriculation") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = issuedDate,
                    onValueChange = { issuedDate = it },
                    label = { Text("Date d'émission") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Date d'expiration") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = holderName,
                    onValueChange = { holderName = it },
                    label = { Text("Titulaire") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    ExtractedDocumentData(
                        documentType = documentType,
                        documentNumber = documentNumber,
                        immatriculation = immatriculation,
                        issuedDate = issuedDate,
                        expiryDate = expiryDate,
                        holderName = holderName,
                        rawText = data.rawText
                    )
                )
            }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

