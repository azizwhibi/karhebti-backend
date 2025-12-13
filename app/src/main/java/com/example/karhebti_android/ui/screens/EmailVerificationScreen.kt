package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.ui.components.OtpCodeInput
import com.example.karhebti_android.data.repository.Resource
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    email: String,
    onBackClick: () -> Unit,
    onVerificationSuccess: () -> Unit,
    // For signup flow: callback to verify signup OTP (POST /auth/signup/verify)
    verifyForSignup: ((String, String) -> Unit)? = null,
    // For signup flow: callback to resend signup OTP (re-initiate signup)
    resendForSignup: ((String) -> Unit)? = null,
    isSignupFlow: Boolean = false,
    // viewModelProvider can be used for testing; type referenced fully-qualified to avoid import issues
    viewModelProvider: (() -> com.example.karhebti_android.viewmodel.EmailVerificationViewModel)? = null
) {
    val vm: com.example.karhebti_android.viewmodel.EmailVerificationViewModel =
        viewModelProvider?.invoke() ?: viewModel()
    var code by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var resendCooldown by remember { mutableStateOf(0) }

    val verificationState = vm.verificationState.collectAsState(initial = null).value
    val resendState = vm.resendState.collectAsState(initial = null).value

    // Auto-send verification code when screen opens for existing-user flows
    LaunchedEffect(email, isSignupFlow) {
        if (isSignupFlow) {
            // Signup flow: OTP should have been sent by signup initiation; do not call sendEmailVerification here.
        } else {
            // Existing-user flow: trigger send
            vm.resendVerificationCode(email)
        }
    }

    // Handle verification state (only for existing-user flow)
    LaunchedEffect(verificationState) {
        if (!isSignupFlow) {
            when (verificationState) {
                is Resource.Success<*> -> {
                    isLoading = false
                    onVerificationSuccess()
                }
                is Resource.Error<*> -> {
                    isLoading = false
                    isError = true
                    errorMessage = (verificationState as Resource.Error<*>).message ?: "Erreur de vérification"
                }
                is Resource.Loading<*> -> {
                    isLoading = true
                    isError = false
                }
                else -> {}
            }
        }
    }

    // Handle resend state for both flows (repository or signup initiation)
    LaunchedEffect(resendState) {
        when (resendState) {
            is Resource.Success<*> -> {
                resendCooldown = 60
            }
            is Resource.Error<*> -> {
                errorMessage = (resendState as Resource.Error<*>).message ?: "Erreur d'envoi"
            }
            else -> {}
        }
    }

    // Cooldown timer
    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            kotlinx.coroutines.delay(1000)
            resendCooldown--
        }
    }

    // Auto-submit when 6 digits entered
    LaunchedEffect(code) {
        if (code.length == 6 && !isLoading) {
            if (isSignupFlow) {
                // Call parent-provided signup verification (will complete signup)
                verifyForSignup?.invoke(email, code)
            } else {
                vm.verifyEmail(email, code)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vérification Email") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
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
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Vérifiez votre email",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Nous avons envoyé un code de vérification à\n$email",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OtpCodeInput(
                length = 6,
                value = code,
                onValueChange = {
                    code = it
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
                    text = "Vous n'avez pas reçu le code? ",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = {
                        if (isSignupFlow) {
                            resendForSignup?.invoke(email)
                        } else {
                            vm.resendVerificationCode(email)
                        }
                    },
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
                onClick = {
                    if (isSignupFlow) {
                        verifyForSignup?.invoke(email, code)
                    } else {
                        vm.verifyEmail(email, code)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = code.length == 6 && !isLoading
            ) {
                Text(if (isSignupFlow) "Vérifier et créer le compte" else "Vérifier")
            }
        }
    }
}
