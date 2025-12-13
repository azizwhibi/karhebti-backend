package com.example.karhebti_android.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.example.karhebti_android.ui.screens.BreakdownHistoryScreen
import com.example.karhebti_android.ui.screens.HistoryItem

/**
 * Simple Fragment that hosts the Compose BreakdownHistoryScreen and
 * launches the JitsiCallActivity when the user taps "Appeler".
 * Replace `JitsiCallActivity::class.java` with your actual activity class.
 */
class HistoryFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val composeView = ComposeView(requireContext()).apply {
            setContent {
                // Sample data - replace with your real ViewModel data
                val sample = listOf(
                    HistoryItem(id = "1", type = "PNEU", status = "RESOLVED", date = "2025-11-27"),
                    HistoryItem(id = "2", type = "BATTERIE", status = "OPEN", date = "2025-11-28")
                )

                BreakdownHistoryScreen(items = sample, onCall = { roomId ->
                    // Start Jitsi activity and pass the room id
                    val intent = Intent(requireContext(), Class.forName("com.example.karhebti_android.jitsi.JitsiCallActivity"))
                    intent.putExtra("ROOM_ID", roomId)
                    startActivity(intent)
                })
            }
        }
        return composeView
    }
}
