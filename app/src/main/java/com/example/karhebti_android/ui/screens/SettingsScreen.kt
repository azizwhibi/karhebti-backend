package com.example.karhebti_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.ui.theme.*
import com.example.karhebti_android.viewmodel.AuthViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.karhebti_android.data.repository.TranslationManager
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onReclamationsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSOSClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()

    // Translation manager setup
    val db = com.example.karhebti_android.data.database.AppDatabase.getInstance(context.applicationContext)
    val translationRepository = com.example.karhebti_android.data.repository.TranslationRepository(
        apiService = com.example.karhebti_android.data.api.RetrofitClient.apiService,
        translationDao = db.translationDao(),
        languageCacheDao = db.languageCacheDao(),
        languageListCacheDao = db.languageListCacheDao()
    )
    val translationManager = remember { TranslationManager.getInstance(translationRepository, context) }
    val coroutineScope = rememberCoroutineScope()
    val currentLanguage by translationManager.currentLanguage.collectAsState()

    // Translated UI strings
    var settingsTitle by remember { mutableStateOf("Paramètres") }
    var profileSection by remember { mutableStateOf("Profil") }
    var userProfileText by remember { mutableStateOf("Profil utilisateur") }
    var emailText by remember { mutableStateOf("Email") }
    var phoneText by remember { mutableStateOf("Téléphone") }
    var preferencesSection by remember { mutableStateOf("Préférences") }
    var notificationsText by remember { mutableStateOf("Notifications") }
    var languageText by remember { mutableStateOf("Langue") }
    var securitySection by remember { mutableStateOf("Sécurité") }
    var changePasswordText by remember { mutableStateOf("Changer mot de passe") }
    var twoFactorText by remember { mutableStateOf("Authentification 2 facteurs") }
    var supportSection by remember { mutableStateOf("Support") }
    var helpCenterText by remember { mutableStateOf("Centre d'aide") }
    var contactUsText by remember { mutableStateOf("Nous contacter") }
    var logoutText by remember { mutableStateOf("Déconnexion") }
    var activeMemberText by remember { mutableStateOf("Membre actif") }
    var adminText by remember { mutableStateOf("Admin") }
    var userText by remember { mutableStateOf("Utilisateur") }
    var notProvidedText by remember { mutableStateOf("Non renseigné") }
    var backText by remember { mutableStateOf("Retour") }
    var reclamationsText by remember { mutableStateOf("Réclamations") }
    var sosText by remember { mutableStateOf("Déclarer une panne (SOS)") }

    LaunchedEffect(currentLanguage) {
        coroutineScope.launch {
            settingsTitle = translationManager.translate("settings_title", "Paramètres", currentLanguage)
            profileSection = translationManager.translate("profile_section", "Profil", currentLanguage)
            userProfileText = translationManager.translate("user_profile", "Profil utilisateur", currentLanguage)
            emailText = translationManager.translate("email", "Email", currentLanguage)
            phoneText = translationManager.translate("phone", "Téléphone", currentLanguage)
            preferencesSection = translationManager.translate("preferences_section", "Préférences", currentLanguage)
            notificationsText = translationManager.translate("notifications", "Notifications", currentLanguage)
            languageText = translationManager.translate("language", "Langue", currentLanguage)
            securitySection = translationManager.translate("security_section", "Sécurité", currentLanguage)
            changePasswordText = translationManager.translate("change_password", "Changer mot de passe", currentLanguage)
            twoFactorText = translationManager.translate("two_factor_auth", "Authentification 2 facteurs", currentLanguage)
            supportSection = translationManager.translate("support_section", "Support", currentLanguage)
            helpCenterText = translationManager.translate("help_center", "Centre d'aide", currentLanguage)
            contactUsText = translationManager.translate("contact_us", "Nous contacter", currentLanguage)
            logoutText = translationManager.translate("logout", "Déconnexion", currentLanguage)
            activeMemberText = translationManager.translate("active_member", "Membre actif", currentLanguage)
            adminText = translationManager.translate("admin", "Admin", currentLanguage)
            userText = translationManager.translate("user", "Utilisateur", currentLanguage)
            notProvidedText = translationManager.translate("not_provided", "Non renseigné", currentLanguage)
            backText = translationManager.translate("back", "Retour", currentLanguage)
            reclamationsText = translationManager.translate("reclamations", "Réclamations", currentLanguage)
            sosText = translationManager.translate("sos_declare", "Déclarer une panne (SOS)", currentLanguage)
        }
    }

    val prefs = remember { context.getSharedPreferences("login_prefs", android.content.Context.MODE_PRIVATE) }

    var notificationsEnabled by remember { mutableStateOf(true) }
    var twoFactorEnabled by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val currentUser = authViewModel.getCurrentUser()
    val userFullName = if (currentUser != null) {
        "${currentUser.prenom} ${currentUser.nom}"
    } else {
        "Utilisateur"
    }
    val userEmail = currentUser?.email ?: "email@example.com"
    val userPhone = currentUser?.telephone?.takeIf { it.isNotEmpty() } ?: notProvidedText
    val userRole = currentUser?.role ?: "user"

    val languageDisplayName = when (currentLanguage) {
        "fr" -> "Français"
        "en" -> "English"
        "ar" -> "العربية"
        else -> "Français"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, backText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentUser != null) {
                                "${currentUser.prenom.firstOrNull()?.uppercaseChar() ?: ""}${currentUser.nom.firstOrNull()?.uppercaseChar() ?: ""}"
                            } else {
                                "U"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = userFullName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (userRole == "admin") {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AlertRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = adminText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AlertRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AccentYellow.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = userText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentYellow,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = activeMemberText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Profile Section
            Text(
                text = profileSection,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )

            SettingsItem(
                icon = Icons.Default.Person,
                title = userProfileText,
                subtitle = userFullName,
                onClick = { /* Navigate to profile */ }
            )

            SettingsItem(
                icon = Icons.Default.Email,
                title = emailText,
                subtitle = userEmail,
                onClick = { /* Edit email */ }
            )

            SettingsItem(
                icon = Icons.Default.Phone,
                title = phoneText,
                subtitle = userPhone,
                onClick = { /* Edit phone */ }
            )

            // Preferences Section
            Text(
                text = preferencesSection,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )

            SettingsItem(
                icon = Icons.Default.Notifications,
                title = notificationsText,
                subtitle = "Gérer vos notifications",
                onClick = onNotificationsClick,
                iconTint = AccentGreen
            )

            SettingsItem(
                icon = Icons.Default.Language,
                title = languageText,
                subtitle = languageDisplayName,
                onClick = { showLanguageDialog = true },
                iconTint = AccentYellow
            )

            // Security Section
            Text(
                text = securitySection,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )

            SettingsItem(
                icon = Icons.Default.Lock,
                title = changePasswordText,
                onClick = { showChangePasswordDialog = true },
                iconTint = DeepPurple
            )

            SettingsToggleItem(
                icon = Icons.Default.Security,
                title = twoFactorText,
                checked = twoFactorEnabled,
                onCheckedChange = { isChecked: Boolean -> twoFactorEnabled = isChecked },
                iconTint = if (twoFactorEnabled) AccentGreen else TextSecondary
            )

            // Support Section
            Text(
                text = supportSection,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )

            SettingsItem(
                icon = Icons.Default.Feedback,
                title = reclamationsText,
                subtitle = "Signaler un problème",
                onClick = onReclamationsClick,
                iconTint = AccentOrange
            )

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Help,
                title = helpCenterText,
                onClick = { /* Open help */ },
                iconTint = AccentGreen
            )

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.ContactSupport,
                title = contactUsText,
                onClick = { /* Contact support */ },
                iconTint = DeepPurple
            )

            // SOS Section
            Text(
                text = "Assistance & SOS",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )

            SettingsItem(
                icon = Icons.Default.Warning,
                title = sosText,
                subtitle = "Déclarer une panne ou demander de l'aide",
                onClick = onSOSClick,
                iconTint = AlertRed
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "K",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logout Button
            Button(
                onClick = {
                    prefs.edit().clear().apply()
                    authViewModel.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlertRed
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = logoutText,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            authViewModel = authViewModel,
            translationManager = translationManager,
            currentLanguage = currentLanguage,
            onDismiss = { showChangePasswordDialog = false }
        )
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentLanguage = currentLanguage,
            translationManager = translationManager,
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
fun LanguagePickerDialog(
    currentLanguage: String,
    translationManager: TranslationManager,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }

    val supportedLanguages = listOf(
        LanguageOption("fr", "Français", "🇫🇷"),
        LanguageOption("en", "English", "🇬🇧"),
        LanguageOption("ar", "العربية", "🇸🇦")
    )

    var dialogTitle by remember { mutableStateOf("Choisir la langue") }
    var selectText by remember { mutableStateOf("Sélectionner") }
    var cancelText by remember { mutableStateOf("Annuler") }

    LaunchedEffect(currentLanguage) {
        coroutineScope.launch {
            dialogTitle = translationManager.translate("choose_language", "Choisir la langue", currentLanguage)
            selectText = translationManager.translate("select", "Sélectionner", currentLanguage)
            cancelText = translationManager.translate("cancel", "Annuler", currentLanguage)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                dialogTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                supportedLanguages.forEach { language ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLanguage = language.code },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedLanguage == language.code)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (selectedLanguage == language.code) 4.dp else 1.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language.flag,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = language.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedLanguage == language.code)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (selectedLanguage == language.code) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        translationManager.setLanguage(selectedLanguage)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(selectText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText)
            }
        }
    )
}

data class LanguageOption(
    val code: String,
    val name: String,
    val flag: String
)

@Composable
fun ChangePasswordDialog(
    authViewModel: AuthViewModel,
    translationManager: TranslationManager,
    currentLanguage: String,
    onDismiss: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val changePasswordState by authViewModel.changePasswordState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var dialogTitle by remember { mutableStateOf("Changer le mot de passe") }
    var currentPasswordLabel by remember { mutableStateOf("Mot de passe actuel") }
    var newPasswordLabel by remember { mutableStateOf("Nouveau mot de passe") }
    var confirmPasswordLabel by remember { mutableStateOf("Confirmer le mot de passe") }
    var changeText by remember { mutableStateOf("Changer") }
    var cancelText by remember { mutableStateOf("Annuler") }
    var showText by remember { mutableStateOf("Afficher") }
    var hideText by remember { mutableStateOf("Masquer") }

    LaunchedEffect(currentLanguage) {
        coroutineScope.launch {
            dialogTitle = translationManager.translate("change_password_title", "Changer le mot de passe", currentLanguage)
            currentPasswordLabel = translationManager.translate("current_password", "Mot de passe actuel", currentLanguage)
            newPasswordLabel = translationManager.translate("new_password", "Nouveau mot de passe", currentLanguage)
            confirmPasswordLabel = translationManager.translate("confirm_password", "Confirmer le mot de passe", currentLanguage)
            changeText = translationManager.translate("change", "Changer", currentLanguage)
            cancelText = translationManager.translate("cancel", "Annuler", currentLanguage)
            showText = translationManager.translate("show", "Afficher", currentLanguage)
            hideText = translationManager.translate("hide", "Masquer", currentLanguage)
        }
    }

    LaunchedEffect(changePasswordState) {
        if (changePasswordState is Resource.Success) {
            authViewModel.resetChangePasswordState()
            onDismiss()
        } else if (changePasswordState is Resource.Error) {
            errorMessage = (changePasswordState as Resource.Error).message
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                dialogTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        errorMessage = null
                    },
                    label = { Text(currentPasswordLabel) },
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                imageVector = if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (currentPasswordVisible) hideText else showText
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorMessage = null
                    },
                    label = { Text(newPasswordLabel) },
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (newPasswordVisible) hideText else showText
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text(confirmPasswordLabel) },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) hideText else showText
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = AlertRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                            errorMessage = "Tous les champs sont requis"
                        }
                        newPassword != confirmPassword -> {
                            errorMessage = "Les mots de passe ne correspondent pas"
                        }
                        newPassword.length < 6 -> {
                            errorMessage = "Le mot de passe doit contenir au moins 6 caractères"
                        }
                        else -> {
                            authViewModel.changePassword(currentPassword, newPassword)
                        }
                    }
                },
                enabled = changePasswordState !is Resource.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (changePasswordState is Resource.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White
                    )
                } else {
                    Text(changeText)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    authViewModel.resetChangePasswordState()
                    onDismiss()
                },
                enabled = changePasswordState !is Resource.Loading
            ) {
                Text(cancelText)
            }
        }
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    iconTint: Color = DeepPurple
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: Color = DeepPurple
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AccentGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = LightGrey
                )
            )
        }
    }
}

