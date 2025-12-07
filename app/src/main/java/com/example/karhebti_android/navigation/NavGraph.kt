package com.example.karhebti_android.navigation

import android.app.Application

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.karhebti_android.data.api.SignupData
import com.example.karhebti_android.data.preferences.TokenManager
import com.example.karhebti_android.data.repository.Resource
import com.example.karhebti_android.ui.screens.*
import com.example.karhebti_android.ui.screens.BreakdownSOSScreen
import com.example.karhebti_android.ui.screens.SOSWaitingScreen
import com.example.karhebti_android.ui.screens.GarageTrackingScreen
import com.example.karhebti_android.ui.screens.GarageBreakdownDetailsScreen
import com.example.karhebti_android.ui.screens.SOSRequestsListScreen
import com.example.karhebti_android.ui.screens.ClientTrackingScreen
import com.example.karhebti_android.ui.screens.GarageNavigationScreen
import com.example.karhebti_android.viewmodel.AuthViewModel
import com.example.karhebti_android.viewmodel.GarageViewModel
import com.example.karhebti_android.viewmodel.ViewModelFactory

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ForgotPassword : Screen("forgot_password")
    object VerifyOtp : Screen("verify_otp/{email}") {
        fun createRoute(email: String) = "verify_otp/$email"
    }
    object ResetPassword : Screen("reset_password/{email}/{otp}") {
        fun createRoute(email: String, otp: String) = "reset_password/$email/$otp"
    }
    object EmailVerification : Screen("email_verification/{email}") {
        fun createRoute(email: String) = "email_verification/$email"
    }
    object Home : Screen("home")
    object Vehicles : Screen("vehicles")
    object VehicleDetail : Screen("vehicle_detail/{vehicleId}") {
        fun createRoute(vehicleId: String) = "vehicle_detail/$vehicleId"
    }
    object Entretiens : Screen("entretiens")
    object MaintenanceDetail : Screen("maintenance_detail/{maintenanceId}") {
        fun createRoute(maintenanceId: String) = "maintenance_detail/$maintenanceId"
    }
    object Documents : Screen("documents")
    object DocumentDetail : Screen("document_detail/{documentId}") {
        fun createRoute(documentId: String) = "document_detail/$documentId"
    }
    object AddDocument : Screen("add_document")
    object EditDocument : Screen("edit_document/{documentId}") {
        fun createRoute(documentId: String) = "edit_document/$documentId"
    }
    object Garages : Screen("garages")
    object AddGarage : Screen("add_garage")
    object GarageDetails : Screen("garage_detail/{garageId}") {
        fun createRoute(garageId: String) = "garage_detail/$garageId"
    }
    object EditGarage : Screen("edit_garage/{garageId}") {
        fun createRoute(garageId: String) = "edit_garage/$garageId"
    }
    object Settings : Screen("settings")
    object Notifications : Screen("notifications")
    object Reclamations : Screen("reclamations")
    object AddReclamation : Screen("add_reclamation")
    object ReclamationDetail : Screen("reclamation_detail/{reclamationId}") {
        fun createRoute(reclamationId: String) = "reclamation_detail/$reclamationId"
    }
    object EditReclamation : Screen("edit_reclamation/{reclamationId}") {
        fun createRoute(reclamationId: String) = "edit_reclamation/$reclamationId"
    }
    object AddDocumentChoice : Screen("add_document_choice")
    object OCRDocumentScan : Screen("ocr_document_scan")
    object SOS : Screen("sos")
    object SOSWaiting : Screen("sos_waiting/{breakdownId}") {
        fun createRoute(breakdownId: String) = "sos_waiting/$breakdownId"
    }
    object GarageTracking : Screen("garage_tracking/{breakdownId}") {
        fun createRoute(breakdownId: String) = "garage_tracking/$breakdownId"
    }
    object SOSStatus : Screen("sos_status/{breakdownId}/{type}/{latitude}/{longitude}") {
        fun createRoute(breakdownId: String?, type: String, latitude: Double, longitude: Double) =
            "sos_status/${breakdownId ?: "null"}/$type/$latitude/$longitude"
    }
    object SOSHistory : Screen("sos_history")
    object ClientTracking : Screen("client_tracking/{breakdownId}") {
        fun createRoute(breakdownId: String) = "client_tracking/$breakdownId"
    }

    // Garage SOS screens
    object SOSRequestsList : Screen("sos_requests_list")
    object GarageNavigation : Screen("garage_navigation/{breakdownId}") {
        fun createRoute(breakdownId: String) = "garage_navigation/$breakdownId"
    }
    object GarageBreakdownDetails : Screen("garage_breakdown_details/{breakdownId}") {
        fun createRoute(breakdownId: String) = "garage_breakdown_details/$breakdownId"
    }

    // Reservation screens
    object Reservation : Screen("reservation/{garageId}") {
        fun createRoute(garageId: String) = "reservation/$garageId"
    }
    object ReservationsList : Screen("reservations")
    object GarageReservationsList : Screen("garage_reservations/{garageId}") {
        fun createRoute(garageId: String) = "garage_reservations/$garageId"
    }

    // Service screens
    object AddService : Screen("add_service/{garageId}") {
        fun createRoute(garageId: String) = "add_service/$garageId"
    }

    // Marketplace screens
    object MarketplaceBrowse : Screen("marketplace_browse")
    object MyListings : Screen("my_listings")
    object Conversations : Screen("conversations")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object PendingSwipes : Screen("pending_swipes")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )
    val garageViewModel: GarageViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )

    // CRITICAL: Create a SHARED MarketplaceViewModel that persists across navigation
    val marketplaceViewModel: com.example.karhebti_android.viewmodel.MarketplaceViewModel = viewModel(
        factory = ViewModelFactory(context.applicationContext as Application)
    )

    var pendingSignupPerform by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.observeAsState()

    // Check for existing token and auto-login (optimisé pour performance)
    val tokenManager = remember { TokenManager.getInstance(context) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100) // Petit délai pour laisser l'UI se charger
        val token = tokenManager.getToken()
        android.util.Log.d("NavGraph", "🔍 Checking token on startup: ${if (token != null) "Found (${token.length} chars)" else "Not found"}")

        if (!token.isNullOrEmpty() && navController.currentDestination?.route == Screen.Login.route) {
            android.util.Log.d("NavGraph", "✅ Auto-navigating to Home with existing token")
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        } else if (token.isNullOrEmpty()) {
            android.util.Log.d("NavGraph", "ℹ️ No token found, staying on Login screen")
        }
    }

    // When a signup has been triggered after email verification, navigate to Home on success
    LaunchedEffect(authState, pendingSignupPerform) {
        if (pendingSignupPerform && authState is Resource.Success<*>) {
            // Clear pending signup from previous back stack entry if present
            navController.previousBackStackEntry?.savedStateHandle?.remove<SignupData>("pendingSignup")
            // Navigate to home, clear back stack
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
            }
            pendingSignupPerform = false
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSignUpClick = { navController.navigate(Screen.SignUp.route) },
                onForgotPasswordClick = { navController.navigate(Screen.ForgotPassword.route) }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignupInitiated = { signupData: SignupData ->
                    // Store pending signup data so EmailVerification can access it
                    navController.currentBackStackEntry?.savedStateHandle?.set("pendingSignup", signupData)
                    // Initiate signup (sends OTP) then navigate to verification screen on success
                    authViewModel.signupInitiate(signupData.nom, signupData.prenom, signupData.email, signupData.password, signupData.telephone)
                    // Navigate to EmailVerification immediately; EmailVerificationScreen will show resend/cooldown
                    navController.navigate(Screen.EmailVerification.createRoute(signupData.email))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToSignup = { navController.navigate(Screen.SignUp.route) },
                onNavigateToOtpVerification = { email ->
                    navController.navigate(Screen.VerifyOtp.createRoute(email))
                }
            )
        }

        composable(Screen.VerifyOtp.route) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email")
            requireNotNull(email) { "email parameter wasn't found. Please make sure it's set!" }

            VerifyOtpScreen(
                email = email,
                onBackClick = { navController.popBackStack() },
                onOtpVerified = { verifiedEmail, otp ->
                    navController.navigate(Screen.ResetPassword.createRoute(verifiedEmail, otp))
                }
            )
        }

        composable(Screen.ResetPassword.route) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email")
            val otp = backStackEntry.arguments?.getString("otp")
            requireNotNull(email) { "email parameter wasn't found. Please make sure it's set!" }
            requireNotNull(otp) { "otp parameter wasn't found. Please make sure it's set!" }
            ResetPasswordScreen(
                email = email,
                otp = otp,
                navController = navController,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.EmailVerification.route) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email")
            requireNotNull(email) { "email parameter wasn't found. Please make sure it's set!" }

            // Check if this is a signup flow (has pending signup data)
            val pendingSignup = navController.previousBackStackEntry?.savedStateHandle?.get<SignupData>("pendingSignup")

            EmailVerificationScreen(
                email = email,
                onBackClick = { navController.popBackStack() },
                onVerificationSuccess = {
                    // After successful email verification, check if there is a pending signup to perform.
                    if (pendingSignup != null) {
                        // This is a signup flow - create the account by verifying signup OTP
                        pendingSignupPerform = true
                        // Remove pending signup from saved state
                        navController.previousBackStackEntry?.savedStateHandle?.remove<SignupData>("pendingSignup")
                        // Note: EmailVerificationScreen will supply the OTP code via the verifyForSignup callback
                        // The actual completion (token save & navigation) will happen when AuthViewModel.authState emits success
                    } else {
                        // No pending signup; just navigate to Home
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                // When in signup flow, EmailVerificationScreen will call this to verify signup OTP
                verifyForSignup = { verifiedEmail, code ->
                    // Mark that we are performing a pending signup so NavGraph listens for authState success
                    pendingSignupPerform = true
                    // Trigger the signup verification which will return AuthResponse and set authState
                    authViewModel.verifySignupOtp(verifiedEmail, code)
                },
                // Provide a resend callback for signup flow that re-initiates the signup using saved pendingSignup
                resendForSignup = { _email ->
                    val saved = navController.previousBackStackEntry?.savedStateHandle?.get<SignupData>("pendingSignup")
                    saved?.let { sd ->
                        authViewModel.signupInitiate(sd.nom, sd.prenom, sd.email, sd.password, sd.telephone)
                    }
                },
                isSignupFlow = pendingSignup != null
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onVehiclesClick = { navController.navigate(Screen.Vehicles.route) },
                onEntretiensClick = { navController.navigate(Screen.Entretiens.route) },
                onDocumentsClick = { navController.navigate(Screen.Documents.route) },
                onGaragesClick = { navController.navigate(Screen.Garages.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onMarketplaceClick = { navController.navigate(Screen.MarketplaceBrowse.route) },
                onMyListingsClick = { navController.navigate(Screen.MyListings.route) },
                onConversationsClick = { navController.navigate(Screen.Conversations.route) },
                onPendingSwipesClick = { navController.navigate(Screen.PendingSwipes.route) },
                onSOSGarageClick = {
                    // Naviguer vers la liste des demandes SOS en attente
                    navController.navigate(Screen.SOSRequestsList.route)
                }
            )
        }

        composable(Screen.Vehicles.route) {
            VehiclesScreen(
                onBackClick = { navController.popBackStack() },
                onVehicleClick = { vehicleId ->
                    navController.navigate(Screen.VehicleDetail.createRoute(vehicleId))
                },
                navController = navController
            )
        }

        composable(Screen.VehicleDetail.route) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")
            requireNotNull(vehicleId) { "vehicleId parameter wasn't found. Please make sure it's set!" }
            VehicleDetailScreen(
                vehicleId = vehicleId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Entretiens.route) {
            EntretiensScreen(
                onBackClick = { navController.popBackStack() },
                onMaintenanceClick = { maintenanceId ->
                    navController.navigate(Screen.MaintenanceDetail.createRoute(maintenanceId))
                }
            )
        }

        composable(Screen.MaintenanceDetail.route) { backStackEntry ->
            val maintenanceId = backStackEntry.arguments?.getString("maintenanceId")
            requireNotNull(maintenanceId) { "maintenanceId parameter wasn't found. Please make sure it's set!" }
            MaintenanceDetailsScreen(
                maintenanceId = maintenanceId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Documents.route) {
            DocumentsScreen(
                onBackClick = { navController.popBackStack() },
                onDocumentClick = { documentId ->
                    navController.navigate(Screen.DocumentDetail.createRoute(documentId))
                },
                onAddDocumentClick = { navController.navigate(Screen.AddDocumentChoice.route) },
                navController = navController
            )
        }

        composable(Screen.DocumentDetail.route) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId")
            requireNotNull(documentId) { "documentId parameter wasn't found. Please make sure it's set!" }
            DocumentDetailScreen(
                documentId = documentId,
                onBackClick = { navController.popBackStack() },
                onEditClick = { docId -> navController.navigate(Screen.EditDocument.createRoute(docId)) }
            )
        }

        composable(Screen.AddDocument.route) {
            AddDocumentScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.EditDocument.route) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId")
            requireNotNull(documentId) { "documentId parameter wasn't found. Please make sure it's set!" }
            AddDocumentScreen(
                documentId = documentId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Garages.route) {
            GaragesScreen(
                navController = navController,
                onBackClick = { navController.popBackStack() },
                onAddGarageClick = { navController.navigate(Screen.AddGarage.route) },
                onGarageClick = { garageId ->
                    navController.navigate(Screen.GarageDetails.createRoute(garageId))
                },
                onModifyGarage = { garageId ->
                    navController.navigate(Screen.EditGarage.createRoute(garageId))
                }
            )
        }

        composable(Screen.AddGarage.route) {
            AddGarageScreen(
                onBackClick = { navController.popBackStack() },
                onGarageAdded = { navController.popBackStack() }
            )
        }

        composable(Screen.GarageDetails.route) { backStackEntry ->
            val garageId = backStackEntry.arguments?.getString("garageId") ?: ""
            GarageDetailsScreen(
                garageId = garageId,
                onBackClick = { navController.popBackStack() },
                userRole = TokenManager.getInstance(context).getUser()?.role ?: "",
                navController = navController
            )
        }

        composable(
            route = Screen.EditGarage.route,
            arguments = listOf(navArgument("garageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val garageId = backStackEntry.arguments?.getString("garageId") ?: ""
            UpdateGarageScreen(
                garageId = garageId,
                onBackClick = { navController.popBackStack() },
                onGarageUpdated = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onReclamationsClick = { navController.navigate(Screen.Reclamations.route) },
                onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                onSOSClick = { navController.navigate(Screen.SOS.route) }
            )
        }

        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Reclamations.route) {
            ReclamationsScreen(
                onBackClick = { navController.popBackStack() },
                onAddReclamationClick = { navController.navigate(Screen.AddReclamation.route) },
                onReclamationClick = { reclamationId ->
                    navController.navigate(Screen.ReclamationDetail.createRoute(reclamationId))
                }
            )
        }

        composable(Screen.AddReclamation.route) {
            AddReclamationScreen(
                onBackClick = { navController.popBackStack() },
                onReclamationCreated = { navController.popBackStack() }
            )
        }

        composable(Screen.ReclamationDetail.route) { backStackEntry ->
            val reclamationId = backStackEntry.arguments?.getString("reclamationId")
            requireNotNull(reclamationId) { "reclamationId parameter wasn't found. Please make sure it's set!" }
            ReclamationDetailScreen(
                reclamationId = reclamationId,
                onBackClick = { navController.popBackStack() },
                onEditClick = { id ->
                    navController.navigate(Screen.EditReclamation.createRoute(id))
                }
            )
        }

        composable(Screen.EditReclamation.route) { backStackEntry ->
            val reclamationId = backStackEntry.arguments?.getString("reclamationId")
            requireNotNull(reclamationId) { "reclamationId parameter wasn't found. Please make sure it's set!" }
            EditReclamationScreen(
                reclamationId = reclamationId,
                onBackClick = { navController.popBackStack() },
                onReclamationUpdated = { navController.popBackStack() }
            )
        }

        composable(Screen.AddDocumentChoice.route) {
            AddDocumentChoiceScreen(
                onBackClick = { navController.popBackStack() },
                onOcrClick = { navController.navigate(Screen.OCRDocumentScan.route) },
                onManualEntryClick = { navController.navigate(Screen.AddDocument.route) }
            )
        }

        composable(Screen.OCRDocumentScan.route) {
            OCRDocumentScanScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.SOS.route) {
            BreakdownSOSScreen(
                onBackClick = { navController.popBackStack() },
                onHistoryClick = { navController.navigate(Screen.SOSHistory.route) },
                onSOSSuccess = { breakdownId, type, lat, lon ->
                    // Navigation vers l'écran d'attente
                    if (breakdownId != null) {
                        navController.navigate(Screen.SOSWaiting.createRoute(breakdownId)) {
                            popUpTo(Screen.SOS.route) { inclusive = true }
                        }
                    } else {
                        // Fallback vers l'ancien écran si pas d'ID
                        navController.navigate(Screen.SOSStatus.createRoute(breakdownId, type, lat, lon)) {
                            popUpTo(Screen.SOS.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Écran d'attente après envoi SOS
        composable(
            route = Screen.SOSWaiting.route,
            arguments = listOf(navArgument("breakdownId") { type = NavType.StringType })
        ) { backStackEntry ->
            val breakdownId = backStackEntry.arguments?.getString("breakdownId") ?: ""

            // Setup ViewModel pour polling
            val context = androidx.compose.ui.platform.LocalContext.current
            val retrofitLocal = androidx.compose.runtime.remember {
                val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
                    level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                }
                val client = okhttp3.OkHttpClient.Builder()
                    .addInterceptor(com.example.karhebti_android.data.api.AuthInterceptor(context))
                    .addInterceptor(logging)
                    .build()
                retrofit2.Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:3000/")
                    .client(client)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
            }
            val apiLocal = retrofitLocal.create(com.example.karhebti_android.network.BreakdownsApi::class.java)
            val repoLocal = com.example.karhebti_android.repository.BreakdownsRepository(apiLocal)
            val factoryLocal = com.example.karhebti_android.viewmodel.BreakdownViewModelFactory(repoLocal)
            val viewModel: com.example.karhebti_android.viewmodel.BreakdownViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(factory = factoryLocal)

            SOSWaitingScreen(
                breakdownId = breakdownId,
                onGarageAccepted = { breakdown ->
                    // Navigation vers écran de tracking client
                    navController.navigate(Screen.ClientTracking.createRoute(breakdown.id)) {
                        popUpTo(Screen.SOSWaiting.route) { inclusive = true }
                    }
                },
                onGarageRefused = {
                    // Retour à l'accueil ou SOS
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SOSWaiting.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
                onGetBreakdownStatus = { id ->
                    viewModel.getBreakdownStatus(id)
                }
            )
        }

        // Écran de suivi du dépanneur en temps réel
        composable(
            route = Screen.GarageTracking.route,
            arguments = listOf(navArgument("breakdownId") { type = NavType.StringType })
        ) { backStackEntry ->
            val breakdownId = backStackEntry.arguments?.getString("breakdownId") ?: ""

            // Récupérer les détails de la panne
            val context = androidx.compose.ui.platform.LocalContext.current
            val retrofitLocal = androidx.compose.runtime.remember {
                val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
                    level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                }
                val client = okhttp3.OkHttpClient.Builder()
                    .addInterceptor(com.example.karhebti_android.data.api.AuthInterceptor(context))
                    .addInterceptor(logging)
                    .build()
                retrofit2.Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:3000/")
                    .client(client)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
            }
            val apiLocal = retrofitLocal.create(com.example.karhebti_android.network.BreakdownsApi::class.java)
            val repoLocal = com.example.karhebti_android.repository.BreakdownsRepository(apiLocal)
            val factoryLocal = com.example.karhebti_android.viewmodel.BreakdownViewModelFactory(repoLocal)
            val viewModel: com.example.karhebti_android.viewmodel.BreakdownViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(factory = factoryLocal)

            // État pour charger la panne
            var breakdown by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf<com.example.karhebti_android.data.BreakdownResponse?>(null)
            }

            androidx.compose.runtime.LaunchedEffect(breakdownId) {
                val result = viewModel.getBreakdownStatus(breakdownId)
                result.onSuccess { breakdown = it }
            }

            if (breakdown != null) {
                GarageTrackingScreen(
                    breakdown = breakdown!!,
                    onBackClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.GarageTracking.route) { inclusive = true }
                        }
                    }
                )
            } else {
                // Écran de chargement
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
        }

        // Client Tracking Screen - Shows garage location in real-time
        composable(
            route = Screen.ClientTracking.route,
            arguments = listOf(navArgument("breakdownId") { type = NavType.StringType })
        ) { backStackEntry ->
            val breakdownId = backStackEntry.arguments?.getString("breakdownId") ?: ""

            // Get breakdown details to retrieve positions
            val context = androidx.compose.ui.platform.LocalContext.current
            val retrofitLocal = androidx.compose.runtime.remember {
                val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
                    level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                }
                val client = okhttp3.OkHttpClient.Builder()
                    .addInterceptor(com.example.karhebti_android.data.api.AuthInterceptor(context))
                    .addInterceptor(logging)
                    .build()
                retrofit2.Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:3000/")
                    .client(client)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
            }
            val apiLocal = retrofitLocal.create(com.example.karhebti_android.network.BreakdownsApi::class.java)
            val repoLocal = com.example.karhebti_android.repository.BreakdownsRepository(apiLocal)

            var breakdown by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.karhebti_android.data.BreakdownResponse?>(null) }

            androidx.compose.runtime.LaunchedEffect(breakdownId) {
                repoLocal.getBreakdownString(breakdownId).collect { result ->
                    result.onSuccess { breakdown = it }
                }
            }

            breakdown?.let {
                ClientTrackingScreen(
                    breakdownId = breakdownId,
                    clientLat = it.latitude ?: 36.8065,
                    clientLon = it.longitude ?: 10.1815,
                    garageLat = 36.8165, // TODO: Get from garage position
                    garageLon = 10.1915, // TODO: Get from garage position
                    garageName = "Auto Service Pro",
                    garagePhone = "+216 XX XXX XXX",
                    onBackClick = { navController.popBackStack() }
                )
            } ?: run {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
        }

        // SOS Requests List Screen (for garage owners)
        composable(Screen.SOSRequestsList.route) {
            SOSRequestsListScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSOSClick = { breakdownId ->
                    // Navigate to breakdown details
                    navController.navigate(Screen.GarageBreakdownDetails.createRoute(breakdownId))
                }
            )
        }

        // Garage Breakdown Details Screen (for garage owners)
        composable(
            route = Screen.GarageBreakdownDetails.route,
            arguments = listOf(navArgument("breakdownId") { type = NavType.StringType })
        ) { backStackEntry ->
            val breakdownId = backStackEntry.arguments?.getString("breakdownId") ?: ""

            GarageBreakdownDetailsScreen(
                breakdownId = breakdownId,
                onBackClick = {
                    navController.popBackStack()
                },
                onAcceptSuccess = { breakdown ->
                    // Navigate to navigation screen after accepting
                    navController.navigate(Screen.GarageNavigation.createRoute(breakdown.id)) {
                        popUpTo(Screen.GarageBreakdownDetails.route) { inclusive = true }
                    }
                },
                onRefuseSuccess = {
                    // Navigate back to SOS list
                    navController.navigate(Screen.SOSRequestsList.route) {
                        popUpTo(Screen.GarageBreakdownDetails.route) { inclusive = true }
                    }
                }
            )
        }

        // Garage Navigation Screen - Shows route to client after accepting
        composable(
            route = Screen.GarageNavigation.route,
            arguments = listOf(navArgument("breakdownId") { type = NavType.StringType })
        ) { backStackEntry ->
            val breakdownId = backStackEntry.arguments?.getString("breakdownId") ?: ""

            // Get breakdown details
            val context = androidx.compose.ui.platform.LocalContext.current
            val retrofitLocal = androidx.compose.runtime.remember {
                val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
                    level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
                }
                val client = okhttp3.OkHttpClient.Builder()
                    .addInterceptor(com.example.karhebti_android.data.api.AuthInterceptor(context))
                    .addInterceptor(logging)
                    .build()
                retrofit2.Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:3000/")
                    .client(client)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
            }
            val apiLocal = retrofitLocal.create(com.example.karhebti_android.network.BreakdownsApi::class.java)
            val repoLocal = com.example.karhebti_android.repository.BreakdownsRepository(apiLocal)

            var breakdown by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.karhebti_android.data.BreakdownResponse?>(null) }

            androidx.compose.runtime.LaunchedEffect(breakdownId) {
                repoLocal.getBreakdownString(breakdownId).collect { result ->
                    result.onSuccess { breakdown = it }
                }
            }

            breakdown?.let {
                GarageNavigationScreen(
                    breakdownId = breakdownId,
                    clientLat = it.latitude ?: 36.8065,
                    clientLon = it.longitude ?: 10.1815,
                    clientName = "Client",
                    clientPhone = "+216 XX XXX XXX",
                    breakdownType = it.type ?: "PNEU",
                    onBackClick = { navController.popBackStack() },
                    onArrived = {
                        // TODO: Mark as arrived in backend
                        navController.navigate(Screen.SOSRequestsList.route) {
                            popUpTo(Screen.GarageNavigation.route) { inclusive = true }
                        }
                    }
                )
            } ?: run {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
        }

        composable(Screen.SOSHistory.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val retrofitLocal = androidx.compose.runtime.remember {
                val logging = okhttp3.logging.HttpLoggingInterceptor().apply { level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY }
                val client = okhttp3.OkHttpClient.Builder()
                    .addInterceptor(com.example.karhebti_android.data.api.AuthInterceptor(context))
                    .addInterceptor(logging)
                    .build()
                retrofit2.Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:3000/")
                    .client(client)
                    .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                    .build()
            }
            val apiLocal = retrofitLocal.create(com.example.karhebti_android.network.BreakdownsApi::class.java)
            val repoLocal = com.example.karhebti_android.repository.BreakdownsRepository(apiLocal)
            val factoryLocal = com.example.karhebti_android.viewmodel.BreakdownViewModelFactory(repoLocal)
            val viewModel: com.example.karhebti_android.viewmodel.BreakdownViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factoryLocal)

            val uiState by viewModel.uiState.collectAsState(initial = com.example.karhebti_android.viewmodel.BreakdownUiState.Idle)

            val items: List<HistoryItem> = when (uiState) {
                is com.example.karhebti_android.viewmodel.BreakdownUiState.Success -> {
                    val data = (uiState as com.example.karhebti_android.viewmodel.BreakdownUiState.Success).data
                    if (data is List<*>) {
                        data.filterIsInstance<com.example.karhebti_android.data.BreakdownResponse>().map { b ->
                            HistoryItem(
                                id = b.id,
                                type = b.type,
                                status = b.status,
                                date = b.createdAt ?: "-",
                                latitude = b.latitude,
                                longitude = b.longitude
                            )
                        }
                    } else emptyList()
                }
                else -> emptyList()
            }

            val callContext = context

            BreakdownHistoryScreen(
                items = items,
                isLoading = uiState is com.example.karhebti_android.viewmodel.BreakdownUiState.Loading,
                onRefresh = { viewModel.fetchAllBreakdowns() },
                onBackClick = { navController.popBackStack() },
                onCall = { roomId ->
                    val intent = com.example.karhebti_android.jitsi.JitsiCallActivity.createIntent(callContext, roomId)
                    callContext.startActivity(intent)
                }
            )

            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.fetchAllBreakdowns()
            }
        }

        composable(Screen.SOSStatus.route) { backStackEntry ->
            val breakdownId = backStackEntry.arguments?.getString("breakdownId")?.takeIf { it != "null" }
            val type = backStackEntry.arguments?.getString("type") ?: ""
            val latitude = backStackEntry.arguments?.getString("latitude")?.toDoubleOrNull() ?: 0.0
            val longitude = backStackEntry.arguments?.getString("longitude")?.toDoubleOrNull() ?: 0.0

            SOSStatusScreen(
                breakdownId = breakdownId,
                type = type,
                latitude = latitude,
                longitude = longitude,
                onBackClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Reservation screens
        composable(
            route = Screen.Reservation.route,
            arguments = listOf(navArgument("garageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val garageId = backStackEntry.arguments?.getString("garageId") ?: ""
            ReservationScreen(
                garageId = garageId,
                navController = navController
            )
        }

        composable(Screen.ReservationsList.route) {
            ReservationsListScreen(navController)
        }

        composable(
            route = Screen.GarageReservationsList.route,
            arguments = listOf(navArgument("garageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val garageId = backStackEntry.arguments?.getString("garageId") ?: ""
            GarageReservationsListScreen(
                garageId = garageId,
                navController = navController
            )
        }

        // Service screen
        composable(
            route = Screen.AddService.route,
            arguments = listOf(navArgument("garageId") { type = NavType.StringType })
        ) { backStackEntry ->
            val garageId = backStackEntry.arguments?.getString("garageId") ?: ""
            // AddServiceScreen implementation would go here
            // AddServiceScreen(garageId = garageId, onBackClick = { navController.popBackStack() })
        }

        // Marketplace navigation
        composable(Screen.MarketplaceBrowse.route) {
            MarketplaceBrowseScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToChat = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                navController = navController
            )
        }

        composable(Screen.MyListings.route) {
            MyListingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Conversations.route) {
            ConversationsScreen(
                onBackClick = { navController.popBackStack() },
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                }
            )
        }

        composable(Screen.Chat.route) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId")
            requireNotNull(conversationId) { "conversationId parameter wasn't found. Please make sure it's set!" }
            ChatScreen(
                conversationId = conversationId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.PendingSwipes.route) {
            PendingSwipesScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToChat = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                }
            )
        }
    }
}