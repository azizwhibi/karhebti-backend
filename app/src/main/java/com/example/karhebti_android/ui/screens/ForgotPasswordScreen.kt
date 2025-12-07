package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.api.MessageResponse
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.viewmodel.AuthViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit = {},
    onNavigateToSignup: () -> Unit = {},
    onNavigateToOtpVerification: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    val forgotPasswordState by authViewModel.forgotPasswordState.observeAsState()
    var showConfirmation by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(forgotPasswordState) {
        when (val state = forgotPasswordState) {
            is Resource.Success<MessageResponse> -> {
                showConfirmation = true
                delay(1500)
                onNavigateToOtpVerification(email)
            }
            is Resource.Error<MessageResponse> -> {
                isSending = false
                val errorMsg = state.message ?: "Erreur"
                if (errorMsg.contains("Email not found", ignoreCase = true) ||
                    errorMsg.contains("404", ignoreCase = true)) {
                    showErrorDialog = true
                }
            }
            is Resource.Loading<MessageResponse> -> {
                isSending = true
            }
            else -> {}
        }
    }

    fun validateEmail(): Boolean {
        emailError = when {
            email.isBlank() -> "L'email est requis"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email invalide"
            else -> null
        }
        return emailError == null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mot de passe oublié") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Entrez votre email pour réinitialiser votre mot de passe",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it.trim()
                    if (emailError != null) validateEmail()
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = if (emailError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    focusedBorderColor = if (emailError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = if (emailError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedLabelColor = if (emailError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    errorBorderColor = MaterialTheme.colorScheme.error,
                    errorLabelColor = MaterialTheme.colorScheme.error
                ),
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                enabled = !isSending
            )

            Button(
                onClick = {
                    if (validateEmail()) {
                        authViewModel.forgotPassword(email)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = !isSending
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Envoyer instructions",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (showConfirmation) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Un email de réinitialisation a été envoyé",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }

    // Email not found dialog with signup option
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = {
                Text(
                    "Email non trouvé",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Aucun compte n'est associé à cet email.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Veuillez vérifier l'email ou créer un nouveau compte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showErrorDialog = false
                        onNavigateToSignup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Créer un compte")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showErrorDialog = false }
                ) {
                    Text("Essayer un autre email")
                }
            }
        )
    }
}
