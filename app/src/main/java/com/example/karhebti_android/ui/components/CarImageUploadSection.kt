package com.example.karhebti_android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.viewmodel.CarImageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarImageUploadSection(
    carId: String,
    currentImageUrl: String?,
    onImageUpdated: (String) -> Unit,
    viewModel: CarImageViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showProgress by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val uploadState by viewModel.uploadState.collectAsState()

    // Photo Picker launcher for Android 13+ (API 33+)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            viewModel.uploadCarImage(carId, it)
        }
    }

    // Fallback for older Android versions
    val legacyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            viewModel.uploadCarImage(carId, it)
        }
    }

    // Handle upload state
    LaunchedEffect(uploadState) {
        when (uploadState) {
            is Resource.Loading -> {
                showProgress = true
                errorMessage = null
            }
            is Resource.Success -> {
                showProgress = false
                val newImageUrl = (uploadState as Resource.Success).data?.imageUrl
                if (newImageUrl != null) {
                    onImageUpdated(newImageUrl)
                }
                viewModel.resetUploadState()
            }
            is Resource.Error -> {
                showProgress = false
                errorMessage = (uploadState as Resource.Error).message
            }
            else -> {
                showProgress = false
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Photo du véhicule",
                style = MaterialTheme.typography.titleMedium
            )

            // Image display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (currentImageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(currentImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Photo du véhicule",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                }

                if (showProgress) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Error message
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Upload button
            Button(
                onClick = {
                    try {
                        // Try Photo Picker first (Android 13+)
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } catch (e: Exception) {
                        // Fallback to legacy picker
                        legacyPickerLauncher.launch("image/*")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !showProgress
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (currentImageUrl != null) "Changer la photo" else "Ajouter une photo")
            }

            Text(
                text = "Images acceptées: JPEG, PNG, WebP (max 5MB)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

