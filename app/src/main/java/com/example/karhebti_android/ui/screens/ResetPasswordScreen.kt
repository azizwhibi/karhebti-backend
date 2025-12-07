package com.example.karhebti_android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.navigation.Screen
import com.example.karhebti_android.viewmodel.AuthViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory
import kotlinx.coroutines.delay
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    email: String,
    otp: String,
    navController: NavController,
    onBackClick: () -> Unit = {},
    onPasswordResetSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var navigationInProgress by remember { mutableStateOf(false) }

    val resetPasswordState by authViewModel.resetPasswordState.collectAsState()

    // Handle password reset success
    LaunchedEffect(resetPasswordState) {
        when (val state = resetPasswordState) {
            is Resource.Success -> {
                if (!navigationInProgress) {
                    navigationInProgress = true
                    Toast.makeText(
                        context,
                        "Mot de passe réinitialisé avec succès!",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Clear old token and user data before redirecting to login
                    authViewModel.logout()
                    // Reset the state to prevent re-triggering
                    authViewModel.resetResetPasswordState()
                    // Small delay to ensure toast is visible
                    delay(1000)
                    // Use NavController to navigate to login and clear stack
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }



                }
            }
            is Resource.Error -> {
                Toast.makeText(
                    context,
                    state.message ?: "Erreur lors de la réinitialisation",
                    Toast.LENGTH_LONG
                ).show()
                authViewModel.resetResetPasswordState()
            }
            else -> {}
        }
    }

    fun validatePasswords(): Boolean {
        var isValid = true

        // Validate new password
        newPasswordError = when {
            newPassword.isBlank() -> {
                isValid = false
                "Le mot de passe est requis"
            }
            newPassword.length < 8 -> {
                isValid = false
                "Le mot de passe doit contenir au moins 8 caractères"
            }
            !newPassword.any { it.isUpperCase() } -> {
                isValid = false
                "Le mot de passe doit contenir au moins une majuscule"
            }
            !newPassword.any { it.isLowerCase() } -> {
                isValid = false
                "Le mot de passe doit contenir au moins une minuscule"
            }
            !newPassword.any { it.isDigit() } -> {
                isValid = false
                "Le mot de passe doit contenir au moins un chiffre"
            }
            else -> null
        }

        // Validate confirm password
        confirmPasswordError = when {
            confirmPassword.isBlank() -> {
                isValid = false
                "Veuillez confirmer le mot de passe"
            }
            confirmPassword != newPassword -> {
                isValid = false
                "Les mots de passe ne correspondent pas"
            }
            else -> null
        }

        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouveau mot de passe") },
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
                text = "Réinitialisez votre mot de passe",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Entrez votre nouveau mot de passe sécurisé",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // New Password Field
            OutlinedTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    if (newPasswordError != null) validatePasswords()
                },
                label = { Text("Nouveau mot de passe") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = if (newPasswordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    focusedBorderColor = if (newPasswordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                isError = newPasswordError != null,
                supportingText = newPasswordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Masquer le mot de passe" else "Afficher le mot de passe",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                singleLine = true,
                enabled = resetPasswordState !is Resource.Loading
            )

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (confirmPasswordError != null) validatePasswords()
                },
                label = { Text("Confirmer le mot de passe") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = if (confirmPasswordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    focusedBorderColor = if (confirmPasswordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                isError = confirmPasswordError != null,
                supportingText = confirmPasswordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Masquer le mot de passe" else "Afficher le mot de passe",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                singleLine = true,
                enabled = resetPasswordState !is Resource.Loading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Password Requirements
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Le mot de passe doit contenir:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PasswordRequirement("Au moins 8 caractères", newPassword.length >= 8)
                    PasswordRequirement("Une majuscule", newPassword.any { it.isUpperCase() })
                    PasswordRequirement("Une minuscule", newPassword.any { it.isLowerCase() })
                    PasswordRequirement("Un chiffre", newPassword.any { it.isDigit() })
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reset Button
            Button(
                onClick = {
                    if (validatePasswords()) {
                        authViewModel.resetPassword(email, otp, newPassword)
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
                enabled = resetPasswordState !is Resource.Loading
            ) {
                if (resetPasswordState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Réinitialiser le mot de passe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PasswordRequirement(text: String, met: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (met) "✓" else "○",
            color = if (met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
