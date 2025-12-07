package com.example.karhebti_android.jitsi

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Lightweight fallback Activity to "join" a Jitsi room by opening the public
 * Jitsi Meet URL (https://meet.jit.si/{room}) in a Custom Tab or browser.
 * This avoids adding the heavy native Jitsi SDK and its transitive dependency
 * issues while keeping the UX for video calls.
 *
 * Start it with an Intent containing extra "ROOM". Example:
 * val intent = Intent(context, JitsiCallActivity::class.java).apply {
 *   putExtra("ROOM", "sos-room-1")
 * }
 * context.startActivity(intent)
 */
class JitsiCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val room = intent?.getStringExtra("ROOM")
        if (room.isNullOrBlank()) {
            Toast.makeText(this, "Aucune room fournie pour l'appel", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Build the URL for the hosted Jitsi Meet instance
        val url = "https://meet.jit.si/${Uri.encode(room)}"

        // Try to open with Custom Tabs for better UX
        val customTabsIntent = CustomTabsIntent.Builder().build()
        try {
            customTabsIntent.launchUrl(this, Uri.parse(url))
        } catch (t: Throwable) {
            // Fallback to generic browser
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            } catch (e: Throwable) {
                Toast.makeText(this, "Impossible d'ouvrir la room: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Close this wrapper activity — the browser/custom tab takes over
        finish()
    }

    companion object {
        fun createIntent(context: Context, room: String): Intent {
            return Intent(context, JitsiCallActivity::class.java).apply {
                putExtra("ROOM", room)
            }
        }
    }
}
