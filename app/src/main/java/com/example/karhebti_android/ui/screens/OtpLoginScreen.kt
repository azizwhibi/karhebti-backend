package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.ui.components.OtpCodeInput
import com.example.karhebti_android.viewmodel.OtpLoginViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpLoginScreen(
    identifier: String,
    onBackClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: OtpLoginViewModel = viewModel()
) {
    var otpCode by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableStateOf(0) }

    val otpVerifyState by viewModel.otpVerifyState.collectAsState()
    val otpSendState by viewModel.otpSendState.collectAsState()

    // Auto-send OTP on first load
    LaunchedEffect(Unit) {
        viewModel.sendOtpCode(identifier)
    }

    // Handle verification state
    LaunchedEffect(otpVerifyState) {
        when (otpVerifyState) {
            is com.example.karhebti_android.data.repository.Resource.Success -> {
                isLoading = false
                onLoginSuccess()
            }
            is com.example.karhebti_android.data.repository.Resource.Error -> {
                isLoading = false
                isError = true
                errorMessage = (otpVerifyState as com.example.karhebti_android.data.repository.Resource.Error).message ?: "Code invalide"
            }
            is com.example.karhebti_android.data.repository.Resource.Loading -> {
                isLoading = true
                isError = false
            }
            else -> {}
        }
    }

    // Handle send state
    LaunchedEffect(otpSendState) {
        when (otpSendState) {
            is com.example.karhebti_android.data.repository.Resource.Success -> {
                resendCooldown = 60
            }
            else -> {}
        }
    }

    // Cooldown timer
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            delay(1000)
            resendCooldown--
        }
    }

    // Auto-submit when 6 digits entered
    LaunchedEffect(otpCode) {
        if (otpCode.length == 6 && !isLoading) {
            viewModel.verifyOtpLogin(identifier, otpCode)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connexion OTP") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Entrez le code OTP",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Code envoyé à\n$identifier",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OtpCodeInput(
                length = 6,
                value = otpCode,
                onValueChange = {
                    otpCode = it
                    if (isError) {
                        isError = false
                        errorMessage = ""
                    }
                },
                isError = isError
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Code non reçu? ",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = { viewModel.sendOtpCode(identifier) },
                    enabled = resendCooldown == 0
                ) {
                    Text(
                        text = if (resendCooldown > 0)
                            "Renvoyer (${resendCooldown}s)"
                        else
                            "Renvoyer"
                    )
                }
            }

            Button(
                onClick = { viewModel.verifyOtpLogin(identifier, otpCode) },
                modifier = Modifier.fillMaxWidth(),
                enabled = otpCode.length == 6 && !isLoading
            ) {
                Text("Se connecter")
            }
        }
    }
}

