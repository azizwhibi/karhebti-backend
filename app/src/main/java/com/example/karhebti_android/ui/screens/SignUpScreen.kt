package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.api.SignupData
import com.example.karhebti_android.viewmodel.AuthUiState
import com.example.karhebti_android.viewmodel.AuthViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignupInitiated: (SignupData) -> Unit = {},
    onSignUpSuccess: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as android.app.Application)
    )

    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var nomError by remember { mutableStateOf<String?>(null) }
    var prenomError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var telephoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    val authState by authViewModel.authState.observeAsState(AuthUiState.Idle)

    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Success -> onSignUpSuccess()
            else -> {}
        }
    }

    // When signup is initiated we simply emit the pending signup data to the caller
    // The caller will start the email verification flow before creating the account.
    fun validateNom(): Boolean {
        nomError = if (nom.isBlank()) "Le nom est requis" else null
        return nomError == null
    }

    fun validatePrenom(): Boolean {
        prenomError = if (prenom.isBlank()) "Le prénom est requis" else null
        return prenomError == null
    }

    fun validateEmail(): Boolean {
        emailError = when {
            email.isBlank() -> "L'email est requis"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email invalide"
            else -> null
        }
        return emailError == null
    }

    fun validateTelephone(): Boolean {
        telephoneError = when {
            telephone.isBlank() -> "Le téléphone est requis"
            telephone.length < 8 -> "Numéro invalide"
            else -> null
        }
        return telephoneError == null
    }

    fun validatePassword(): Boolean {
        passwordError = when {
            password.isBlank() -> "Le mot de passe est requis"
            password.length < 6 -> "Au moins 6 caractères"
            else -> null
        }
        return passwordError == null
    }

    fun validateConfirmPassword(): Boolean {
        confirmPasswordError = when {
            confirmPassword.isBlank() -> "Confirmez le mot de passe"
            confirmPassword != password -> "Les mots de passe ne correspondent pas"
            else -> null
        }
        return confirmPasswordError == null
    }

    fun validateAll(): Boolean {
        val validations = listOf(
            validateNom(),
            validatePrenom(),
            validateEmail(),
            validateTelephone(),
            validatePassword(),
            validateConfirmPassword()
        )
        return validations.all { it }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(authState) {
        if (authState is AuthUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (authState as AuthUiState.Error).message ?: "Erreur d'inscription",
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Inscription") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Créer un compte",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = nom,
                    onValueChange = {
                        nom = it
                        if (nomError != null) validateNom()
                    },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = if (nomError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        focusedBorderColor = if (nomError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = if (nomError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = if (nomError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    isError = nomError != null,
                    supportingText = nomError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    enabled = authState !is AuthUiState.Loading
                )

                OutlinedTextField(
                    value = prenom,
                    onValueChange = {
                        prenom = it
                        if (prenomError != null) validatePrenom()
                    },
                    label = { Text("Prénom") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = if (prenomError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        focusedBorderColor = if (prenomError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = if (prenomError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = if (prenomError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    isError = prenomError != null,
                    supportingText = prenomError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    enabled = authState !is AuthUiState.Loading
                )

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
                        focusedLabelColor = if (emailError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = authState !is AuthUiState.Loading
                )

                OutlinedTextField(
                    value = telephone,
                    onValueChange = {
                        telephone = it.filter { c -> c.isDigit() }
                        if (telephoneError != null) validateTelephone()
                    },
                    label = { Text("Téléphone") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = if (telephoneError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        focusedBorderColor = if (telephoneError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = if (telephoneError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = if (telephoneError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    isError = telephoneError != null,
                    supportingText = telephoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    enabled = authState !is AuthUiState.Loading
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (passwordError != null) validatePassword()
                    },
                    label = { Text("Mot de passe") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = if (passwordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        focusedBorderColor = if (passwordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = if (passwordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = if (passwordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Cacher" else "Afficher",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    enabled = authState !is AuthUiState.Loading
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        if (confirmPasswordError != null) validateConfirmPassword()
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
                        unfocusedLabelColor = if (confirmPasswordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedLabelColor = if (confirmPasswordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    isError = confirmPasswordError != null,
                    supportingText = confirmPasswordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Cacher" else "Afficher",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    enabled = authState !is AuthUiState.Loading
                )

                Button(
                    onClick = {
                        if (validateAll()) {
                            onSignupInitiated(SignupData(nom = nom, prenom = prenom, email = email, telephone = telephone, password = password))
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
                    enabled = authState !is AuthUiState.Loading
                ) {
                    if (authState is AuthUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "S'inscrire",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "Déjà membre ? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Connexion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onLoginClick() }
                    )
                }
            }
        }
    }
}
