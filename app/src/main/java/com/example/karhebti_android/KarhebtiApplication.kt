package com.example.karhebti_android

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.example.karhebti_android.data.api.RetrofitClient
import com.example.karhebti_android.data.preferences.TokenManager

class KarhebtiApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize RetrofitClient with context
        RetrofitClient.initialize(this)

        try {
            // Initialiser Firebase
            FirebaseApp.initializeApp(this)
            Log.d("KarhebtiApplication", "✅ Firebase initialisé avec succès")
        } catch (e: Exception) {
            Log.e("KarhebtiApplication", "❌ Erreur lors de l'initialisation Firebase: ${e.message}", e)
        }

        // Initialize TokenManager on app start
        TokenManager.getInstance(this).initializeToken()
    }
}
