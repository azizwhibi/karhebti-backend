package com.example.karhebti_android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.viewmodel.AuthViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyOtpScreen(
    email: String,
    onBackClick: () -> Unit = {},
    onOtpVerified: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    var otp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    val verifyOtpState by authViewModel.verifyOtpState.collectAsState()
    val forgotPasswordState by authViewModel.forgotPasswordState.observeAsState()
    var isResending by remember { mutableStateOf(false) }

    // Handle OTP verification success
    LaunchedEffect(verifyOtpState) {
        when (verifyOtpState) {
            is Resource.Success -> {
                Toast.makeText(context, "OTP vérifié avec succès!", Toast.LENGTH_SHORT).show()
                delay(500)
                onOtpVerified(email, otp)
            }
            is Resource.Error -> {
                val errorMsg = (verifyOtpState as Resource.Error).message ?: "Erreur"
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    // Handle resend OTP response
    LaunchedEffect(forgotPasswordState) {
        if (isResending) {
            when (forgotPasswordState) {
                is Resource.Success -> {
                    Toast.makeText(context, "Nouveau code OTP envoyé!", Toast.LENGTH_SHORT).show()
                    isResending = false
                }
                is Resource.Error -> {
                    Toast.makeText(
                        context,
                        "Erreur lors du renvoi: ${(forgotPasswordState as Resource.Error).message}",
                        Toast.LENGTH_LONG
                    ).show()
                    isResending = false
                }
                else -> {}
            }
        }
    }

    fun validateOtp(): Boolean {
        otpError = when {
            otp.isBlank() -> "Le code OTP est requis"
            otp.length != 6 -> "Le code OTP doit contenir 6 chiffres"
            !otp.all { it.isDigit() } -> "Le code OTP ne doit contenir que des chiffres"
            else -> null
        }
        return otpError == null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vérification OTP") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Header
            Text(
                text = "Code de vérification",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Un code à 6 chiffres a été envoyé à",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                text = email,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // OTP Input
            OutlinedTextField(
                value = otp,
                onValueChange = { newValue ->
                    if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                        otp = newValue
                        if (otpError != null) validateOtp()
                    }
                },
                label = { Text("Code OTP") },
                placeholder = { Text("000000") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = if (otpError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    focusedBorderColor = if (otpError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = if (otpError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedLabelColor = if (otpError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                ),
                isError = otpError != null,
                supportingText = otpError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = verifyOtpState !is Resource.Loading,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Verify Button
            Button(
                onClick = {
                    if (validateOtp()) {
                        authViewModel.verifyOtp(email, otp)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = verifyOtpState !is Resource.Loading
            ) {
                if (verifyOtpState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Vérifier le code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resend OTP
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vous n'avez pas reçu le code?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = {
                        isResending = true
                        authViewModel.forgotPassword(email)
                    },
                    enabled = forgotPasswordState !is Resource.Loading
                ) {
                    if (isResending && forgotPasswordState is Resource.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Renvoyer",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Info text
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "💡 Vérifiez votre boîte de réception et vos spams",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

